package media

import (
	"context"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

// Repository handles media database operations.
type Repository struct {
	pool *pgxpool.Pool
}

// NewRepository creates a new media repository.
func NewRepository(pool *pgxpool.Pool) *Repository {
	return &Repository{pool: pool}
}

// Create inserts a new media record.
func (r *Repository) Create(ctx context.Context, m *Media) (*Media, error) {
	err := r.pool.QueryRow(ctx,
		`INSERT INTO media (user_id, filename, original_name, mime_type, size, width, height, duration,
		storage_path, thumb_path, taken_at, lat, lng, is_favorite, is_archived, is_deleted, media_type)
		VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17)
		RETURNING id, user_id, filename, original_name, mime_type, size, width, height, duration,
		storage_path, thumb_path, taken_at, lat, lng, is_favorite, is_archived, is_deleted, deleted_at, media_type, created_at, updated_at`,
		m.UserID, m.Filename, m.OriginalName, m.MimeType, m.Size, m.Width, m.Height, m.Duration,
		m.StoragePath, m.ThumbPath, m.TakenAt, m.Lat, m.Lng, m.IsFavorite, m.IsArchived, m.IsDeleted, m.MediaType,
	).Scan(
		&m.ID, &m.UserID, &m.Filename, &m.OriginalName, &m.MimeType, &m.Size, &m.Width, &m.Height, &m.Duration,
		&m.StoragePath, &m.ThumbPath, &m.TakenAt, &m.Lat, &m.Lng, &m.IsFavorite, &m.IsArchived, &m.IsDeleted,
		&m.DeletedAt, &m.MediaType, &m.CreatedAt, &m.UpdatedAt,
	)
	if err != nil {
		return nil, fmt.Errorf("media.Create: %w", err)
	}
	return m, nil
}

// ListByUserCursor returns a page of media for a user using cursor-based pagination.
func (r *Repository) ListByUserCursor(ctx context.Context, userID uuid.UUID, cursor string, limit int, archived, favorite bool) ([]Media, string, error) {
	if limit <= 0 || limit > 200 {
		limit = 50
	}

	args := []interface{}{userID, archived, favorite, limit + 1}
	query := `SELECT id, user_id, filename, original_name, mime_type, size, width, height, duration,
		storage_path, thumb_path, taken_at, lat, lng, is_favorite, is_archived, is_deleted, deleted_at, media_type, created_at, updated_at
		FROM media
		WHERE user_id = $1 AND is_deleted = false AND is_archived = $2 AND ($3 = false OR is_favorite = true)`

	if cursor != "" {
		args = append(args, cursor)
		query += fmt.Sprintf(" AND created_at < (SELECT created_at FROM media WHERE id = $%d)", len(args))
	}
	query += " ORDER BY created_at DESC LIMIT $4"

	rows, err := r.pool.Query(ctx, query, args...)
	if err != nil {
		return nil, "", fmt.Errorf("ListByUserCursor: %w", err)
	}
	defer rows.Close()

	var items []Media
	for rows.Next() {
		var m Media
		if err := rows.Scan(
			&m.ID, &m.UserID, &m.Filename, &m.OriginalName, &m.MimeType, &m.Size, &m.Width, &m.Height, &m.Duration,
			&m.StoragePath, &m.ThumbPath, &m.TakenAt, &m.Lat, &m.Lng, &m.IsFavorite, &m.IsArchived, &m.IsDeleted,
			&m.DeletedAt, &m.MediaType, &m.CreatedAt, &m.UpdatedAt,
		); err != nil {
			return nil, "", fmt.Errorf("ListByUserCursor scan: %w", err)
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

// GetByID fetches a single media item by ID.
func (r *Repository) GetByID(ctx context.Context, id uuid.UUID) (*Media, error) {
	m := &Media{}
	err := r.pool.QueryRow(ctx,
		`SELECT id, user_id, filename, original_name, mime_type, size, width, height, duration,
		storage_path, thumb_path, taken_at, lat, lng, is_favorite, is_archived, is_deleted, deleted_at, media_type, created_at, updated_at
		FROM media WHERE id = $1`,
		id,
	).Scan(
		&m.ID, &m.UserID, &m.Filename, &m.OriginalName, &m.MimeType, &m.Size, &m.Width, &m.Height, &m.Duration,
		&m.StoragePath, &m.ThumbPath, &m.TakenAt, &m.Lat, &m.Lng, &m.IsFavorite, &m.IsArchived, &m.IsDeleted,
		&m.DeletedAt, &m.MediaType, &m.CreatedAt, &m.UpdatedAt,
	)
	if err != nil {
		return nil, fmt.Errorf("GetByID: %w", err)
	}
	return m, nil
}

// Update saves changes to a media record.
func (r *Repository) Update(ctx context.Context, m *Media) (*Media, error) {
	err := r.pool.QueryRow(ctx,
		`UPDATE media SET filename=$1, original_name=$2, mime_type=$3, size=$4, width=$5, height=$6,
		duration=$7, storage_path=$8, thumb_path=$9, taken_at=$10, lat=$11, lng=$12,
		is_favorite=$13, is_archived=$14, is_deleted=$15, deleted_at=$16, media_type=$17, updated_at=NOW()
		WHERE id=$18
		RETURNING id, user_id, filename, original_name, mime_type, size, width, height, duration,
		storage_path, thumb_path, taken_at, lat, lng, is_favorite, is_archived, is_deleted, deleted_at, media_type, created_at, updated_at`,
		m.Filename, m.OriginalName, m.MimeType, m.Size, m.Width, m.Height, m.Duration,
		m.StoragePath, m.ThumbPath, m.TakenAt, m.Lat, m.Lng, m.IsFavorite, m.IsArchived,
		m.IsDeleted, m.DeletedAt, m.MediaType, m.ID,
	).Scan(
		&m.ID, &m.UserID, &m.Filename, &m.OriginalName, &m.MimeType, &m.Size, &m.Width, &m.Height, &m.Duration,
		&m.StoragePath, &m.ThumbPath, &m.TakenAt, &m.Lat, &m.Lng, &m.IsFavorite, &m.IsArchived, &m.IsDeleted,
		&m.DeletedAt, &m.MediaType, &m.CreatedAt, &m.UpdatedAt,
	)
	if err != nil {
		return nil, fmt.Errorf("Update: %w", err)
	}
	return m, nil
}

// SoftDelete marks a media item as deleted.
func (r *Repository) SoftDelete(ctx context.Context, id, userID uuid.UUID) error {
	_, err := r.pool.Exec(ctx,
		`UPDATE media SET is_deleted=true, deleted_at=NOW(), updated_at=NOW() WHERE id=$1 AND user_id=$2`,
		id, userID,
	)
	return err
}

// HardDelete permanently removes a media record.
func (r *Repository) HardDelete(ctx context.Context, id, userID uuid.UUID) error {
	_, err := r.pool.Exec(ctx, `DELETE FROM media WHERE id=$1 AND user_id=$2`, id, userID)
	return err
}

// SetFavorite sets the is_favorite flag on a media item.
func (r *Repository) SetFavorite(ctx context.Context, id, userID uuid.UUID, value bool) error {
	_, err := r.pool.Exec(ctx,
		`UPDATE media SET is_favorite=$1, updated_at=NOW() WHERE id=$2 AND user_id=$3`,
		value, id, userID,
	)
	return err
}

// SetArchived sets the is_archived flag on a media item.
func (r *Repository) SetArchived(ctx context.Context, id, userID uuid.UUID, value bool) error {
	_, err := r.pool.Exec(ctx,
		`UPDATE media SET is_archived=$1, updated_at=NOW() WHERE id=$2 AND user_id=$3`,
		value, id, userID,
	)
	return err
}

// PurgeExpiredTrash hard-deletes media soft-deleted longer ago than olderThan.
func (r *Repository) PurgeExpiredTrash(ctx context.Context, olderThan time.Duration) error {
	_, err := r.pool.Exec(ctx,
		`DELETE FROM media WHERE is_deleted=true AND deleted_at < $1`,
		time.Now().Add(-olderThan),
	)
	return err
}

// CreateChunkUpload inserts a new chunk upload record.
func (r *Repository) CreateChunkUpload(ctx context.Context, cu *ChunkUpload) (*ChunkUpload, error) {
	err := r.pool.QueryRow(ctx,
		`INSERT INTO chunk_uploads (user_id, original_name, mime_type, total_size, chunk_size, total_chunks, uploaded_chunks, temp_path)
		VALUES ($1,$2,$3,$4,$5,$6,$7,$8)
		RETURNING id, user_id, original_name, mime_type, total_size, chunk_size, total_chunks, uploaded_chunks, temp_path, created_at`,
		cu.UserID, cu.OriginalName, cu.MimeType, cu.TotalSize, cu.ChunkSize, cu.TotalChunks, 0, cu.TempPath,
	).Scan(&cu.ID, &cu.UserID, &cu.OriginalName, &cu.MimeType, &cu.TotalSize, &cu.ChunkSize, &cu.TotalChunks, &cu.UploadedChunks, &cu.TempPath, &cu.CreatedAt)
	if err != nil {
		return nil, fmt.Errorf("CreateChunkUpload: %w", err)
	}
	return cu, nil
}

// GetChunkUpload retrieves a chunk upload by ID.
func (r *Repository) GetChunkUpload(ctx context.Context, id uuid.UUID) (*ChunkUpload, error) {
	cu := &ChunkUpload{}
	err := r.pool.QueryRow(ctx,
		`SELECT id, user_id, original_name, mime_type, total_size, chunk_size, total_chunks, uploaded_chunks, temp_path, created_at
		FROM chunk_uploads WHERE id=$1`,
		id,
	).Scan(&cu.ID, &cu.UserID, &cu.OriginalName, &cu.MimeType, &cu.TotalSize, &cu.ChunkSize, &cu.TotalChunks, &cu.UploadedChunks, &cu.TempPath, &cu.CreatedAt)
	if err != nil {
		return nil, fmt.Errorf("GetChunkUpload: %w", err)
	}
	return cu, nil
}

// UpdateChunkUpload increments the uploaded_chunks counter.
func (r *Repository) UpdateChunkUpload(ctx context.Context, id uuid.UUID, uploadedChunks int) error {
	_, err := r.pool.Exec(ctx,
		`UPDATE chunk_uploads SET uploaded_chunks=$1 WHERE id=$2`,
		uploadedChunks, id,
	)
	return err
}

// DeleteChunkUpload removes a chunk upload record.
func (r *Repository) DeleteChunkUpload(ctx context.Context, id uuid.UUID) error {
	_, err := r.pool.Exec(ctx, `DELETE FROM chunk_uploads WHERE id=$1`, id)
	return err
}
