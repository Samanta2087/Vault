package albums

import (
	"time"

	"github.com/google/uuid"
)

// Album represents a user-created photo album.
type Album struct {
	ID          uuid.UUID  `json:"id"`
	UserID      uuid.UUID  `json:"user_id"`
	Name        string     `json:"name"`
	Description string     `json:"description"`
	CoverID     *uuid.UUID `json:"cover_id,omitempty"`
	MediaCount  int        `json:"media_count"`
	CreatedAt   time.Time  `json:"created_at"`
	UpdatedAt   time.Time  `json:"updated_at"`
}

// CreateAlbumRequest is the payload for creating an album.
type CreateAlbumRequest struct {
	Name        string     `json:"name"`
	Description string     `json:"description"`
	CoverID     *uuid.UUID `json:"cover_id,omitempty"`
}

// UpdateAlbumRequest is the payload for updating an album.
type UpdateAlbumRequest struct {
	Name        string     `json:"name"`
	Description string     `json:"description"`
	CoverID     *uuid.UUID `json:"cover_id,omitempty"`
}

// AddMediaRequest is the payload for adding media to an album.
type AddMediaRequest struct {
	MediaIDs []uuid.UUID `json:"media_ids"`
}
