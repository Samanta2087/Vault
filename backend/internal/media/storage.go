package media

import (
	"context"
	"errors"
	"fmt"
	"io"
	"net/url"
	"os"
	"path/filepath"
	"time"

	"github.com/minio/minio-go/v7"
	"github.com/minio/minio-go/v7/pkg/credentials"
)

// Storage defines the interface for file storage backends.
type Storage interface {
	Put(ctx context.Context, path string, r io.Reader, size int64, contentType string) error
	Get(ctx context.Context, path string) (io.ReadCloser, error)
	Delete(ctx context.Context, path string) error
	GetURL(ctx context.Context, path string, expiry time.Duration) (string, error)
	Exists(ctx context.Context, path string) (bool, error)
}

// ─────────────────────────── MinIO ───────────────────────────

// MinIOStorage implements Storage using MinIO.
type MinIOStorage struct {
	client *minio.Client
	bucket string
}

// NewMinIOStorage creates a new MinIOStorage.
func NewMinIOStorage(endpoint, accessKey, secretKey, bucket string, useSSL bool) (*MinIOStorage, error) {
	client, err := minio.New(endpoint, &minio.Options{
		Creds:  credentials.NewStaticV4(accessKey, secretKey, ""),
		Secure: useSSL,
	})
	if err != nil {
		return nil, fmt.Errorf("NewMinIOStorage: %w", err)
	}

	// Ensure bucket exists
	ctx := context.Background()
	exists, err := client.BucketExists(ctx, bucket)
	if err != nil {
		return nil, fmt.Errorf("NewMinIOStorage: bucket check: %w", err)
	}
	if !exists {
		if err := client.MakeBucket(ctx, bucket, minio.MakeBucketOptions{}); err != nil {
			return nil, fmt.Errorf("NewMinIOStorage: make bucket: %w", err)
		}
	}

	return &MinIOStorage{client: client, bucket: bucket}, nil
}

func (s *MinIOStorage) Put(ctx context.Context, path string, r io.Reader, size int64, contentType string) error {
	_, err := s.client.PutObject(ctx, s.bucket, path, r, size, minio.PutObjectOptions{ContentType: contentType})
	if err != nil {
		return fmt.Errorf("MinIOStorage.Put: %w", err)
	}
	return nil
}

func (s *MinIOStorage) Get(ctx context.Context, path string) (io.ReadCloser, error) {
	obj, err := s.client.GetObject(ctx, s.bucket, path, minio.GetObjectOptions{})
	if err != nil {
		return nil, fmt.Errorf("MinIOStorage.Get: %w", err)
	}
	return obj, nil
}

func (s *MinIOStorage) Delete(ctx context.Context, path string) error {
	err := s.client.RemoveObject(ctx, s.bucket, path, minio.RemoveObjectOptions{})
	if err != nil {
		return fmt.Errorf("MinIOStorage.Delete: %w", err)
	}
	return nil
}

func (s *MinIOStorage) GetURL(ctx context.Context, path string, expiry time.Duration) (string, error) {
	presigned, err := s.client.PresignedGetObject(ctx, s.bucket, path, expiry, url.Values{})
	if err != nil {
		return "", fmt.Errorf("MinIOStorage.GetURL: %w", err)
	}
	return presigned.String(), nil
}

func (s *MinIOStorage) Exists(ctx context.Context, path string) (bool, error) {
	_, err := s.client.StatObject(ctx, s.bucket, path, minio.StatObjectOptions{})
	if err != nil {
		var merr minio.ErrorResponse
		if errors.As(err, &merr) && merr.Code == "NoSuchKey" {
			return false, nil
		}
		return false, fmt.Errorf("MinIOStorage.Exists: %w", err)
	}
	return true, nil
}

// ─────────────────────────── Local ───────────────────────────

// LocalStorage implements Storage using the local filesystem.
type LocalStorage struct {
	basePath string
}

// NewLocalStorage creates a new LocalStorage.
func NewLocalStorage(basePath string) *LocalStorage {
	_ = os.MkdirAll(basePath, 0755)
	return &LocalStorage{basePath: basePath}
}

func (s *LocalStorage) fullPath(path string) string {
	return filepath.Join(s.basePath, filepath.FromSlash(path))
}

func (s *LocalStorage) Put(ctx context.Context, path string, r io.Reader, size int64, contentType string) error {
	full := s.fullPath(path)
	if err := os.MkdirAll(filepath.Dir(full), 0755); err != nil {
		return fmt.Errorf("LocalStorage.Put: mkdir: %w", err)
	}
	f, err := os.Create(full)
	if err != nil {
		return fmt.Errorf("LocalStorage.Put: create: %w", err)
	}
	defer f.Close()
	if _, err := io.Copy(f, r); err != nil {
		return fmt.Errorf("LocalStorage.Put: copy: %w", err)
	}
	return nil
}

func (s *LocalStorage) Get(ctx context.Context, path string) (io.ReadCloser, error) {
	f, err := os.Open(s.fullPath(path))
	if err != nil {
		return nil, fmt.Errorf("LocalStorage.Get: %w", err)
	}
	return f, nil
}

func (s *LocalStorage) Delete(ctx context.Context, path string) error {
	err := os.Remove(s.fullPath(path))
	if err != nil && !errors.Is(err, os.ErrNotExist) {
		return fmt.Errorf("LocalStorage.Delete: %w", err)
	}
	return nil
}

func (s *LocalStorage) GetURL(ctx context.Context, path string, expiry time.Duration) (string, error) {
	// Local storage serves files via the API; return relative path.
	return "/api/media/local/" + path, nil
}

func (s *LocalStorage) Exists(ctx context.Context, path string) (bool, error) {
	_, err := os.Stat(s.fullPath(path))
	if err != nil {
		if errors.Is(err, os.ErrNotExist) {
			return false, nil
		}
		return false, fmt.Errorf("LocalStorage.Exists: %w", err)
	}
	return true, nil
}
