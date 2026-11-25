package search

import (
	"context"
	"fmt"
	"strings"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"pixelvault/backend/internal/media"
)

// SearchParams holds all search filter options.
type SearchParams struct {
	Q        string
	DateFrom *time.Time
	DateTo   *time.Time
	AlbumID  *uuid.UUID
	Lat      *float64
	Lng      *float64
	RadiusKm *float64
	Favorite bool
	Cursor   string
	Limit    int
	UserID   uuid.UUID
}

// Service provides search functionality.
type Service struct {
	pool *pgxpool.Pool
}

// NewService creates a new search service.
func NewService(pool *pgxpool.Pool) *Service {
	return &Service{pool: pool}
}

// Search executes a dynamic search query.
func (s *Service) Search(ctx context.Context, params SearchParams) ([]media.Media, string, error) {
	if params.Limit <= 0 || params.Limit > 200 {
		params.Limit = 50
	}

	var conditions []string
	var args []interface{}
	argIdx := 1

	addArg := func(val interface{}) int {
		args = append(args, val)
		i := argIdx
		argIdx++
		return i
	}

	// Always filter by user and not deleted
	conditions = append(conditions, fmt.Sprintf("m.user_id = $%d", addArg(params.UserID)))
	conditions = append(conditions, "m.is_deleted = false")

	// Full-text search
	if params.Q != "" {
		i := addArg(params.Q)
		conditions = append(conditions,
			fmt.Sprintf(`(m.search_vector @@ plainto_tsquery('english', $%d) OR m.original_name ILIKE $%d)`, i, addArg("%"+params.Q+"%")),
		)
	}

	// Date range
	if params.DateFrom != nil {
		conditions = append(conditions, fmt.Sprintf("m.taken_at >= $%d", addArg(*params.DateFrom)))
	}
	if params.DateTo != nil {
		conditions = append(conditions, fmt.Sprintf("m.taken_at <= $%d", addArg(*params.DateTo)))
	}

	// Album filter
	if params.AlbumID != nil {
		conditions = append(conditions,
			fmt.Sprintf("EXISTS (SELECT 1 FROM album_media am WHERE am.media_id = m.id AND am.album_id = $%d)", addArg(*params.AlbumID)),
		)
	}

	// Geo radius filter (using Haversine approximation)
	if params.Lat != nil && params.Lng != nil && params.RadiusKm != nil {
		latI := addArg(*params.Lat)
		lngI := addArg(*params.Lng)
		radI := addArg(*params.RadiusKm)
		conditions = append(conditions, fmt.Sprintf(
			`(m.lat IS NOT NULL AND m.lng IS NOT NULL AND
			(6371 * acos(cos(radians($%d)) * cos(radians(m.lat)) * cos(radians(m.lng) - radians($%d)) + sin(radians($%d)) * sin(radians(m.lat)))) <= $%d)`,
			latI, lngI, latI, radI,
		))
	}

	// Favorite filter
	if params.Favorite {
		conditions = append(conditions, "m.is_favorite = true")
	}

	// Cursor
	if params.Cursor != "" {
		i := addArg(params.Cursor)
		conditions = append(conditions, fmt.Sprintf(
			"m.created_at < (SELECT created_at FROM media WHERE id = $%d)", i,
		))
	}

	limitI := addArg(params.Limit + 1)

	query := fmt.Sprintf(`
		SELECT m.id, m.user_id, m.filename, m.original_name, m.mime_type, m.size, m.width, m.height,
		m.duration, m.storage_path, m.thumb_path, m.taken_at, m.lat, m.lng, m.is_favorite,
		m.is_archived, m.is_deleted, m.deleted_at, m.media_type, m.created_at, m.updated_at
		FROM media m
		WHERE %s
		ORDER BY m.created_at DESC
		LIMIT $%d
	`, strings.Join(conditions, " AND "), limitI)

	rows, err := s.pool.Query(ctx, query, args...)
	if err != nil {
		return nil, "", fmt.Errorf("Search: %w", err)
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
			return nil, "", fmt.Errorf("Search scan: %w", err)
		}
		items = append(items, m)
	}
	if err := rows.Err(); err != nil {
		return nil, "", err
	}

	var nextCursor string
	if len(items) > params.Limit {
		nextCursor = items[params.Limit-1].ID.String()
		items = items[:params.Limit]
	}

	return items, nextCursor, nil
}
