package auth

import (
	"time"

	"github.com/google/uuid"
)

// User represents a registered user.
type User struct {
	ID           uuid.UUID
	Email        string
	PasswordHash string
	Role         string
	CreatedAt    time.Time
	UpdatedAt    time.Time
}

// RefreshToken represents a stored refresh token record.
type RefreshToken struct {
	ID        uuid.UUID
	UserID    uuid.UUID
	TokenHash string
	DeviceID  uuid.UUID
	ExpiresAt time.Time
	CreatedAt time.Time
}

// Device represents a user's registered device.
type Device struct {
	ID         uuid.UUID
	UserID     uuid.UUID
	DeviceName string
	DeviceType string
	LastSeenAt time.Time
	CreatedAt  time.Time
}

// RegisterRequest is the payload for user registration.
type RegisterRequest struct {
	Email      string `json:"email"`
	Password   string `json:"password"`
	DeviceName string `json:"device_name"`
	DeviceType string `json:"device_type"`
}

// LoginRequest is the payload for user login.
type LoginRequest struct {
	Email      string `json:"email"`
	Password   string `json:"password"`
	DeviceName string `json:"device_name"`
	DeviceType string `json:"device_type"`
}

// AuthResponse is the response after successful auth.
type AuthResponse struct {
	AccessToken  string       `json:"access_token"`
	RefreshToken string       `json:"refresh_token"`
	User         UserResponse `json:"user"`
}

// UserResponse is the public-facing user representation.
type UserResponse struct {
	ID        string    `json:"id"`
	Email     string    `json:"email"`
	Role      string    `json:"role"`
	CreatedAt time.Time `json:"created_at"`
}

// RefreshRequest is the payload for token refresh.
type RefreshRequest struct {
	RefreshToken string `json:"refresh_token"`
}

// LogoutRequest is the payload for logout.
type LogoutRequest struct {
	RefreshToken string `json:"refresh_token"`
}
