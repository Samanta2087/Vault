package albums

import (
	"context"
	"fmt"

	"github.com/google/uuid"
	"pixelvault/backend/internal/media"
)

// Service provides album business logic.
type Service struct {
	repo *Repository
}

// NewService creates a new albums service.
func NewService(repo *Repository) *Service {
	return &Service{repo: repo}
}

// CreateAlbum creates a new album for a user.
func (s *Service) CreateAlbum(ctx context.Context, userID uuid.UUID, req *CreateAlbumRequest) (*Album, error) {
	if req.Name == "" {
		return nil, fmt.Errorf("album name is required")
	}
	album := &Album{
		UserID:      userID,
		Name:        req.Name,
		Description: req.Description,
		CoverID:     req.CoverID,
	}
	return s.repo.CreateAlbum(ctx, album)
}

// GetAlbum retrieves an album by ID.
func (s *Service) GetAlbum(ctx context.Context, id, userID uuid.UUID) (*Album, error) {
	return s.repo.GetAlbum(ctx, id, userID)
}

// ListAlbums returns all albums for a user.
func (s *Service) ListAlbums(ctx context.Context, userID uuid.UUID) ([]Album, error) {
	return s.repo.ListAlbums(ctx, userID)
}

// UpdateAlbum updates an existing album.
func (s *Service) UpdateAlbum(ctx context.Context, id, userID uuid.UUID, req *UpdateAlbumRequest) (*Album, error) {
	album := &Album{
		ID:          id,
		UserID:      userID,
		Name:        req.Name,
		Description: req.Description,
		CoverID:     req.CoverID,
	}
	return s.repo.UpdateAlbum(ctx, album)
}

// DeleteAlbum removes an album.
func (s *Service) DeleteAlbum(ctx context.Context, id, userID uuid.UUID) error {
	return s.repo.DeleteAlbum(ctx, id, userID)
}

// AddMedia adds media items to an album.
func (s *Service) AddMedia(ctx context.Context, albumID uuid.UUID, mediaIDs []uuid.UUID) error {
	if len(mediaIDs) == 0 {
		return fmt.Errorf("no media IDs provided")
	}
	return s.repo.AddMedia(ctx, albumID, mediaIDs)
}

// RemoveMedia removes a media item from an album.
func (s *Service) RemoveMedia(ctx context.Context, albumID, mediaID uuid.UUID) error {
	return s.repo.RemoveMedia(ctx, albumID, mediaID)
}

// GetAlbumMedia returns paginated media in an album.
func (s *Service) GetAlbumMedia(ctx context.Context, albumID uuid.UUID, cursor string, limit int) ([]media.Media, string, error) {
	return s.repo.GetAlbumMedia(ctx, albumID, cursor, limit)
}
