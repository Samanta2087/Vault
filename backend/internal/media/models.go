package media

import (
	"time"

	"github.com/google/uuid"
)

// MediaType indicates whether the media is a photo or video.
type MediaType string

const (
	MediaTypePhoto MediaType = "photo"
	MediaTypeVideo MediaType = "video"
)

// Media represents a photo or video asset.
type Media struct {
	ID           uuid.UUID
	UserID       uuid.UUID
	Filename     string
	OriginalName string
	MimeType     string
	Size         int64
	Width        int
	Height       int
	Duration     *float64
	StoragePath  string
	ThumbPath    string
	TakenAt      *time.Time
	Lat          *float64
	Lng          *float64
	IsFavorite   bool
	IsArchived   bool
	IsDeleted    bool
	DeletedAt    *time.Time
	MediaType    MediaType
	CreatedAt    time.Time
	UpdatedAt    time.Time
}

// ChunkUpload tracks a chunked upload in progress.
type ChunkUpload struct {
	ID             uuid.UUID
	UserID         uuid.UUID
	OriginalName   string
	MimeType       string
	TotalSize      int64
	ChunkSize      int64
	TotalChunks    int
	UploadedChunks int
	TempPath       string
	CreatedAt      time.Time
}

// SignedURLRequest is the request body for a signed URL.
type SignedURLRequest struct {
	MediaID string `json:"media_id"`
}

// SignedURLResponse contains the signed URL and expiry.
type SignedURLResponse struct {
	URL       string    `json:"url"`
	ExpiresAt time.Time `json:"expires_at"`
}

// InitUploadRequest starts a chunked upload session.
type InitUploadRequest struct {
	Filename  string `json:"filename"`
	MimeType  string `json:"mime_type"`
	TotalSize int64  `json:"total_size"`
}

// InitUploadResponse contains the upload session ID and chunk size.
type InitUploadResponse struct {
	UploadID  string `json:"upload_id"`
	ChunkSize int64  `json:"chunk_size"`
}
