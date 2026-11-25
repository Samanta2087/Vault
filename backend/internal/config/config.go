package config

import (
	"bufio"
	"errors"
	"os"
	"strconv"
	"strings"
)

// Config holds all application configuration.
type Config struct {
	DBUrl             string
	JWTSecret         string
	JWTExpiryMin      int
	RefreshExpiryDays int
	MinIOEndpoint     string
	MinIOAccessKey    string
	MinIOSecretKey    string
	MinIOBucket       string
	MinIOUseSSL       bool
	StorageType       string
	LocalStoragePath  string
	ServerPort        int
	SignedURLSecret   string
	UploadChunkSizeMB int
	ThumbnailSize     int
	AllowedOrigins    string
}

func loadDotEnv(filepath string) {
	file, err := os.Open(filepath)
	if err != nil {
		return
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		parts := strings.SplitN(line, "=", 2)
		if len(parts) == 2 {
			k := strings.TrimSpace(parts[0])
			v := strings.TrimSpace(parts[1])
			if os.Getenv(k) == "" {
				os.Setenv(k, v)
			}
		}
	}
}

// Load reads configuration from environment variables.
func Load() (*Config, error) {
	loadDotEnv(".env")

	cfg := &Config{
		JWTExpiryMin:      getEnvInt("JWT_EXPIRY_MIN", 15),
		RefreshExpiryDays: getEnvInt("REFRESH_EXPIRY_DAYS", 30),
		MinIOUseSSL:       getEnvBool("MINIO_USE_SSL", false),
		StorageType:       getEnvStr("STORAGE_TYPE", "local"),
		LocalStoragePath:  getEnvStr("LOCAL_STORAGE_PATH", "./storage"),
		ServerPort:        getEnvInt("SERVER_PORT", 8080),
		UploadChunkSizeMB: getEnvInt("UPLOAD_CHUNK_SIZE_MB", 5),
		ThumbnailSize:     getEnvInt("THUMBNAIL_SIZE", 256),
		AllowedOrigins:    getEnvStr("ALLOWED_ORIGINS", "*"),
		DBUrl:             os.Getenv("DATABASE_URL"),
		JWTSecret:         os.Getenv("JWT_SECRET"),
		SignedURLSecret:   os.Getenv("SIGNED_URL_SECRET"),
		MinIOEndpoint:     os.Getenv("MINIO_ENDPOINT"),
		MinIOAccessKey:    os.Getenv("MINIO_ACCESS_KEY"),
		MinIOSecretKey:    os.Getenv("MINIO_SECRET_KEY"),
		MinIOBucket:       os.Getenv("MINIO_BUCKET"),
	}

	if cfg.DBUrl == "" {
		return nil, errors.New("DATABASE_URL is required")
	}
	if cfg.JWTSecret == "" {
		return nil, errors.New("JWT_SECRET is required")
	}
	if cfg.SignedURLSecret == "" {
		return nil, errors.New("SIGNED_URL_SECRET is required")
	}
	if cfg.StorageType == "minio" {
		if cfg.MinIOEndpoint == "" || cfg.MinIOAccessKey == "" || cfg.MinIOSecretKey == "" || cfg.MinIOBucket == "" {
			return nil, errors.New("MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, and MINIO_BUCKET are required when STORAGE_TYPE=minio")
		}
	}

	return cfg, nil
}

func getEnvStr(key, defaultVal string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return defaultVal
}

func getEnvInt(key string, defaultVal int) int {
	if v := os.Getenv(key); v != "" {
		if i, err := strconv.Atoi(v); err == nil {
			return i
		}
	}
	return defaultVal
}

func getEnvBool(key string, defaultVal bool) bool {
	if v := os.Getenv(key); v != "" {
		b, err := strconv.ParseBool(v)
		if err == nil {
			return b
		}
	}
	return defaultVal
}
