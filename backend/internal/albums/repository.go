package albums

import (
	"context"
	"fmt"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"pixelvault/backend/internal/media"
)

// Repository handles album database operations.
type Repository struct {
	pool *pgxpool.Pool
}

// NewRepository creates a new albums repository.
func NewRepository(pool *pgxpool.Pool) *Repository {
	return &Repository{pool: pool}
}

// CreateAlbum inserts a new album record.
func (r *Repository) CreateAlbum(ctx context.Context, album *Album) (*Album, error) {
	err := r.pool.QueryRow(ctx,
		`INSERT INTO albums (user_id, name, description, cover_id) VALUES ($1, $2, $3, $4)
		RETURNING id, user_id, name, description, cover_id, created_at, updated_at`,
		album.UserID, album.Name, album.Description, album.CoverID,
	).Scan(&album.ID, &album.UserID, &album.Name, &album.Description, &album.CoverID, &album.CreatedAt, &album.UpdatedAt)
	if err != nil {
		return nil, fmt.Errorf("CreateAlbum: %w", err)
	}
	return album, nil
}

// GetAlbum retrieves a single album by ID.
func (r *Repository) GetAlbum(ctx context.Context, id, userID uuid.UUID) (*Album, error) {
	a := &Album{}
	err := r.pool.QueryRow(ctx,
		`SELECT a.id, a.user_id, a.name, a.description, a.cover_id, a.created_at, a.updated_at,
		COUNT(am.media_id) as media_count
		FROM albums a LEFT JOIN album_media am ON a.id = am.album_id
		WHERE a.id=$1 AND a.user_id=$2
		GROUP BY a.id`,
		id, userID,
	).Scan(&a.ID, &a.UserID, &a.Name, &a.Description, &a.CoverID, &a.CreatedAt, &a.UpdatedAt, &a.MediaCount)
	if err != nil {
		return nil, fmt.Errorf("GetAlbum: %w", err)
	}
	return a, nil
}

// ListAlbums returns all albums for a user.
func (r *Repository) ListAlbums(ctx context.Context, userID uuid.UUID) ([]Album, error) {
	rows, err := r.pool.Query(ctx,
		`SELECT a.id, a.user_id, a.name, a.description, a.cover_id, a.created_at, a.updated_at,
		COUNT(am.media_id) as media_count
		FROM albums a LEFT JOIN album_media am ON a.id = am.album_id
		WHERE a.user_id=$1
		GROUP BY a.id ORDER BY a.created_at DESC`,
		userID,
	)
	if err != nil {
		return nil, fmt.Errorf("ListAlbums: %w", err)
	}
	defer rows.Close()

	var albums []Album
	for rows.Next() {
		var a Album
		if err := rows.Scan(&a.ID, &a.UserID, &a.Name, &a.Description, &a.CoverID, &a.CreatedAt, &a.UpdatedAt, &a.MediaCount); err != nil {
			return nil, fmt.Errorf("ListAlbums scan: %w", err)
		}
		albums = append(albums, a)
	}
	return albums, rows.Err()
}

// UpdateAlbum saves changes to an album.
func (r *Repository) UpdateAlbum(ctx context.Context, album *Album) (*Album, error) {
	err := r.pool.QueryRow(ctx,
		`UPDATE albums SET name=$1, description=$2, cover_id=$3, updated_at=NOW()
		WHERE id=$4 AND user_id=$5
		RETURNING id, user_id, name, description, cover_id, created_at, updated_at`,
		album.Name, album.Description, album.CoverID, album.ID, album.UserID,
	).Scan(&album.ID, &album.UserID, &album.Name, &album.Description, &album.CoverID, &album.CreatedAt, &album.UpdatedAt)
	if err != nil {
		return nil, fmt.Errorf("UpdateAlbum: %w", err)
	}
	return album, nil
}

// DeleteAlbum removes an album.
func (r *Repository) DeleteAlbum(ctx context.Context, id, userID uuid.UUID) error {
	_, err := r.pool.Exec(ctx, `DELETE FROM albums WHERE id=$1 AND user_id=$2`, id, userID)
	if err != nil {
		return fmt.Errorf("DeleteAlbum: %w", err)
	}
	return nil
}

// AddMedia adds media items to an album.
func (r *Repository) AddMedia(ctx context.Context, albumID uuid.UUID, mediaIDs []uuid.UUID) error {
	for _, mID := range mediaIDs {
		_, err := r.pool.Exec(ctx,
			`INSERT INTO album_media (album_id, media_id) VALUES ($1, $2) ON CONFLICT DO NOTHING`,
			albumID, mID,
		)
		if err != nil {
			return fmt.Errorf("AddMedia: %w", err)
		}
	}
	return nil
}

// RemoveMedia removes a media item from an album.
func (r *Repository) RemoveMedia(ctx context.Context, albumID, mediaID uuid.UUID) error {
	_, err := r.pool.Exec(ctx, `DELETE FROM album_media WHERE album_id=$1 AND media_id=$2`, albumID, mediaID)
	if err != nil {
		return fmt.Errorf("RemoveMedia: %w", err)
	}
	return nil
}

// GetAlbumMedia returns a paginated list of media in an album.
func (r *Repository) GetAlbumMedia(ctx context.Context, albumID uuid.UUID, cursor string, limit int) ([]media.Media, string, error) {
	if limit <= 0 || limit > 200 {
		limit = 50
	}

	args := []interface{}{albumID, limit + 1}
	query := `SELECT m.id, m.user_id, m.filename, m.original_name, m.mime_type, m.size, m.width, m.height,
		m.duration, m.storage_path, m.thumb_path, m.taken_at, m.lat, m.lng, m.is_favorite,
		m.is_archived, m.is_deleted, m.deleted_at, m.media_type, m.created_at, m.updated_at
		FROM media m
		INNER JOIN album_media am ON m.id = am.media_id
		WHERE am.album_id = $1 AND m.is_deleted = false`

	if cursor != "" {
		args = append(args, cursor)
		query += fmt.Sprintf(" AND am.added_at < (SELECT added_at FROM album_media WHERE album_id=$1 AND media_id=$%d)", len(args))
	}
	query += " ORDER BY am.added_at DESC LIMIT $2"

	rows, err := r.pool.Query(ctx, query, args...)
	if err != nil {
		return nil, "", fmt.Errorf("GetAlbumMedia: %w", err)
	}
	defer rows.Close()

	var items []media.Media
	for rows.Next() {
		var m media.Media
		if err := rows.Scan(
			&m.ID, &m.UserID, &m.Filename, &m.OriginalName, &m.MimeType, &m.Size,
			&m.Width, &m.Height, &m.Duration, &m.StoragePath, &m.ThumbPath,
			&m.TakenAt, &m.Lat, &m.Lng, &m.IsFavorite, &m.IsArchived, &m.IsDeleted,
			&m.DeletedAt, &m.MediaType, &m.CreatedAt, &m.UpdatedAt,
		); err != nil {
			return nil, "", fmt.Errorf("GetAlbumMedia scan: %w", err)
		}
		items = append(items, m)
	}
	if err := rows.Err(); err != nil {
		return nil, "", err
	}

	var nextCursor string
	if len(items) > limit {
		nextCursor = items[limit-1].ID.String()
		items = items[:limit]
	}

	return items, nextCursor, nil
}
