package middleware

import (
	"strings"

	"github.com/gofiber/fiber/v3"
	"github.com/gofiber/fiber/v3/middleware/cors"
	"pixelvault/backend/internal/config"
)

// CORS returns a configured CORS middleware.
func CORS(cfg *config.Config) fiber.Handler {
	origins := []string{"*"}
	if cfg.AllowedOrigins != "" {
		origins = strings.Split(cfg.AllowedOrigins, ",")
	}
	return cors.New(cors.Config{
		AllowOrigins:     origins,
		AllowMethods:     []string{"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"},
		AllowHeaders:     []string{"Origin", "Content-Type", "Accept", "Authorization", "X-Chunk-Index"},
		AllowCredentials: false,
		MaxAge:           86400,
	})
}
