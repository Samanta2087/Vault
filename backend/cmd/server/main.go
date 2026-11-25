package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/gofiber/fiber/v3"
	"pixelvault/backend/internal/admin"
	"pixelvault/backend/internal/albums"
	"pixelvault/backend/internal/auth"
	"pixelvault/backend/internal/config"
	"pixelvault/backend/internal/database"
	"pixelvault/backend/internal/media"
	"pixelvault/backend/internal/middleware"
	"pixelvault/backend/internal/search"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("failed to load config: %v", err)
	}

	ctx := context.Background()
	if err := database.Connect(ctx, cfg.DBUrl); err != nil {
		log.Fatalf("failed to connect to database: %v", err)
	}
	defer database.Pool.Close()

	if err := database.RunMigrations(cfg.DBUrl, "file://migrations"); err != nil {
		log.Fatalf("failed to run migrations: %v", err)
	}

	// Initialize storage
	var storage media.Storage
	if cfg.StorageType == "minio" {
		storage, err = media.NewMinIOStorage(cfg.MinIOEndpoint, cfg.MinIOAccessKey, cfg.MinIOSecretKey, cfg.MinIOBucket, cfg.MinIOUseSSL)
		if err != nil {
			log.Fatalf("failed to init MinIO storage: %v", err)
		}
	} else {
		storage = media.NewLocalStorage(cfg.LocalStoragePath)
	}

	app := fiber.New(fiber.Config{
		ErrorHandler: func(c fiber.Ctx, err error) error {
			code := fiber.StatusInternalServerError
			var e *fiber.Error
			if ok := false; ok {
				_ = e
			}
			if fe, ok := err.(*fiber.Error); ok {
				code = fe.Code
			}
			return c.Status(code).JSON(fiber.Map{"error": err.Error()})
		},
	})

	// Global middleware
	app.Use(middleware.Recover())
	app.Use(middleware.CORS(cfg))
	app.Use(middleware.GlobalRateLimit())

	// Repositories and services
	authorRepo := auth.NewRepository(database.Pool)
	authSvc := auth.NewService(authorRepo, cfg)
	authHandler := auth.NewHandler(authSvc)

	mediaRepo := media.NewRepository(database.Pool)
	mediaHandler := media.NewHandler(mediaRepo, storage, cfg)

	albumsRepo := albums.NewRepository(database.Pool)
	albumsSvc := albums.NewService(albumsRepo)
	albumsHandler := albums.NewHandler(albumsSvc)

	searchSvc := search.NewService(database.Pool)
	searchHandler := search.NewHandler(searchSvc)

	adminHandler := admin.NewHandler(database.Pool)

	authMiddleware := auth.JWTMiddleware(cfg)
	adminMiddleware := auth.AdminMiddleware()

	// Routes
	api := app.Group("/api")

	// Auth routes
	authRoutes := api.Group("/auth")
	authRoutes.Use(middleware.AuthRateLimit())
	authRoutes.Post("/register", authHandler.Register)
	authRoutes.Post("/login", authHandler.Login)
	authRoutes.Post("/refresh", authHandler.Refresh)
	authRoutes.Post("/logout", authHandler.Logout)

	// Media routes (JWT protected)
	mediaRoutes := api.Group("/media", authMiddleware)
	mediaRoutes.Post("/upload/init", mediaHandler.InitUpload)
	mediaRoutes.Patch("/upload/:id", mediaHandler.UploadChunk)
	mediaRoutes.Post("/upload/:id/complete", mediaHandler.CompleteUpload)
	mediaRoutes.Get("/", mediaHandler.ListMedia)
	mediaRoutes.Get("/signed-url/:id", mediaHandler.GetSignedURL)
	mediaRoutes.Get("/file/:token", mediaHandler.ServeFile)
	mediaRoutes.Get("/:id", mediaHandler.GetMedia)
	mediaRoutes.Delete("/:id/permanent", mediaHandler.HardDeleteMedia)
	mediaRoutes.Delete("/:id", mediaHandler.SoftDeleteMedia)
	mediaRoutes.Put("/:id/favorite", mediaHandler.ToggleFavorite)
	mediaRoutes.Put("/:id/archive", mediaHandler.ToggleArchive)
	mediaRoutes.Get("/:id/thumbnail", mediaHandler.ServeThumbnail)

	// Albums routes (JWT protected)
	albumsRoutes := api.Group("/albums", authMiddleware)
	albumsRoutes.Post("/", albumsHandler.CreateAlbum)
	albumsRoutes.Get("/", albumsHandler.ListAlbums)
	albumsRoutes.Get("/:id", albumsHandler.GetAlbum)
	albumsRoutes.Put("/:id", albumsHandler.UpdateAlbum)
	albumsRoutes.Delete("/:id", albumsHandler.DeleteAlbum)
	albumsRoutes.Post("/:id/media", albumsHandler.AddMedia)
	albumsRoutes.Delete("/:id/media/:mediaID", albumsHandler.RemoveMedia)
	albumsRoutes.Get("/:id/media", albumsHandler.GetAlbumMedia)

	// Search routes (JWT protected)
	searchRoutes := api.Group("/search", authMiddleware)
	searchRoutes.Get("/", searchHandler.Search)

	// Admin routes (JWT + admin middleware)
	adminRoutes := api.Group("/admin", authMiddleware, adminMiddleware)
	adminRoutes.Get("/stats", adminHandler.GetStats)
	adminRoutes.Get("/users", adminHandler.ListUsers)
	adminRoutes.Delete("/users/:id", adminHandler.DeleteUser)
	adminRoutes.Put("/users/:id/role", adminHandler.UpdateUserRole)

	// Graceful shutdown
	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt, syscall.SIGTERM)
	go func() {
		<-c
		log.Println("Shutting down server...")
		_ = app.Shutdown()
	}()

	addr := fmt.Sprintf(":%d", cfg.ServerPort)
	log.Printf("PixelVault server starting on %s", addr)
	if err := app.Listen(addr); err != nil {
		log.Fatalf("server error: %v", err)
	}
}
