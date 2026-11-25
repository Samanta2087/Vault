package auth

import (
	"context"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

// Repository handles auth-related database operations.
type Repository struct {
	pool *pgxpool.Pool
}

// NewRepository creates a new auth repository.
func NewRepository(pool *pgxpool.Pool) *Repository {
	return &Repository{pool: pool}
}

// CreateUser inserts a new user record.
func (r *Repository) CreateUser(ctx context.Context, email, passwordHash string) (*User, error) {
	u := &User{}
	err := r.pool.QueryRow(ctx,
		`INSERT INTO users (email, password_hash, role) VALUES ($1, $2, 'user')
		RETURNING id, email, password_hash, role, created_at, updated_at`,
		email, passwordHash,
	).Scan(&u.ID, &u.Email, &u.PasswordHash, &u.Role, &u.CreatedAt, &u.UpdatedAt)
	if err != nil {
		return nil, fmt.Errorf("CreateUser: %w", err)
	}
	return u, nil
}

// GetUserByEmail retrieves a user by email address.
func (r *Repository) GetUserByEmail(ctx context.Context, email string) (*User, error) {
	u := &User{}
	err := r.pool.QueryRow(ctx,
		`SELECT id, email, password_hash, role, created_at, updated_at FROM users WHERE email = $1`,
		email,
	).Scan(&u.ID, &u.Email, &u.PasswordHash, &u.Role, &u.CreatedAt, &u.UpdatedAt)
	if err != nil {
		return nil, fmt.Errorf("GetUserByEmail: %w", err)
	}
	return u, nil
}

// GetUserByID retrieves a user by their UUID.
func (r *Repository) GetUserByID(ctx context.Context, id uuid.UUID) (*User, error) {
	u := &User{}
	err := r.pool.QueryRow(ctx,
		`SELECT id, email, password_hash, role, created_at, updated_at FROM users WHERE id = $1`,
		id,
	).Scan(&u.ID, &u.Email, &u.PasswordHash, &u.Role, &u.CreatedAt, &u.UpdatedAt)
	if err != nil {
		return nil, fmt.Errorf("GetUserByID: %w", err)
	}
	return u, nil
}

// CreateRefreshToken stores a new refresh token record.
func (r *Repository) CreateRefreshToken(ctx context.Context, userID uuid.UUID, tokenHash string, deviceID uuid.UUID, expiresAt time.Time) (*RefreshToken, error) {
	rt := &RefreshToken{}
	err := r.pool.QueryRow(ctx,
		`INSERT INTO refresh_tokens (user_id, token_hash, device_id, expires_at)
		VALUES ($1, $2, $3, $4)
		RETURNING id, user_id, token_hash, device_id, expires_at, created_at`,
		userID, tokenHash, deviceID, expiresAt,
	).Scan(&rt.ID, &rt.UserID, &rt.TokenHash, &rt.DeviceID, &rt.ExpiresAt, &rt.CreatedAt)
	if err != nil {
		return nil, fmt.Errorf("CreateRefreshToken: %w", err)
	}
	return rt, nil
}

// GetRefreshToken retrieves a refresh token by its hash.
func (r *Repository) GetRefreshToken(ctx context.Context, tokenHash string) (*RefreshToken, error) {
	rt := &RefreshToken{}
	err := r.pool.QueryRow(ctx,
		`SELECT id, user_id, token_hash, device_id, expires_at, created_at FROM refresh_tokens WHERE token_hash = $1`,
		tokenHash,
	).Scan(&rt.ID, &rt.UserID, &rt.TokenHash, &rt.DeviceID, &rt.ExpiresAt, &rt.CreatedAt)
	if err != nil {
		return nil, fmt.Errorf("GetRefreshToken: %w", err)
	}
	return rt, nil
}

// DeleteRefreshToken removes a refresh token by hash.
func (r *Repository) DeleteRefreshToken(ctx context.Context, tokenHash string) error {
	_, err := r.pool.Exec(ctx, `DELETE FROM refresh_tokens WHERE token_hash = $1`, tokenHash)
	if err != nil {
		return fmt.Errorf("DeleteRefreshToken: %w", err)
	}
	return nil
}

// CreateDevice inserts a new device record for the user.
func (r *Repository) CreateDevice(ctx context.Context, userID uuid.UUID, name, deviceType string) (*Device, error) {
	d := &Device{}
	err := r.pool.QueryRow(ctx,
		`INSERT INTO devices (user_id, device_name, device_type) VALUES ($1, $2, $3)
		RETURNING id, user_id, device_name, device_type, last_seen_at, created_at`,
		userID, name, deviceType,
	).Scan(&d.ID, &d.UserID, &d.DeviceName, &d.DeviceType, &d.LastSeenAt, &d.CreatedAt)
	if err != nil {
		return nil, fmt.Errorf("CreateDevice: %w", err)
	}
	return d, nil
}

// GetDevice retrieves a device by user, name and type.
func (r *Repository) GetDevice(ctx context.Context, userID uuid.UUID, name, deviceType string) (*Device, error) {
	d := &Device{}
	err := r.pool.QueryRow(ctx,
		`SELECT id, user_id, device_name, device_type, last_seen_at, created_at FROM devices WHERE user_id = $1 AND device_name = $2 AND device_type = $3`,
		userID, name, deviceType,
	).Scan(&d.ID, &d.UserID, &d.DeviceName, &d.DeviceType, &d.LastSeenAt, &d.CreatedAt)
	if err != nil {
		return nil, fmt.Errorf("GetDevice: %w", err)
	}
	return d, nil
}

// UpdateDeviceLastSeen updates the last_seen_at timestamp of a device.
func (r *Repository) UpdateDeviceLastSeen(ctx context.Context, deviceID uuid.UUID) error {
	_, err := r.pool.Exec(ctx, `UPDATE devices SET last_seen_at = NOW() WHERE id = $1`, deviceID)
	if err != nil {
		return fmt.Errorf("UpdateDeviceLastSeen: %w", err)
	}
	return nil
}
