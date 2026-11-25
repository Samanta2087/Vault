package auth

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"errors"
	"fmt"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
	"pixelvault/backend/internal/config"
)

// ErrInvalidCredentials is returned when login fails.
var ErrInvalidCredentials = errors.New("invalid credentials")

// ErrTokenExpired is returned when a refresh token is expired.
var ErrTokenExpired = errors.New("token expired")

// Service provides authentication business logic.
type Service struct {
	repo *Repository
	cfg  *config.Config
}

// NewService creates a new auth service.
func NewService(repo *Repository, cfg *config.Config) *Service {
	return &Service{repo: repo, cfg: cfg}
}

// Claims represents JWT claims.
type Claims struct {
	UserID string `json:"user_id"`
	Role   string `json:"role"`
	jwt.RegisteredClaims
}

// Register creates a new user account.
func (s *Service) Register(ctx context.Context, req *RegisterRequest) (*AuthResponse, error) {
	hash, err := bcrypt.GenerateFromPassword([]byte(req.Password), 12)
	if err != nil {
		return nil, fmt.Errorf("Register: bcrypt: %w", err)
	}

	user, err := s.repo.CreateUser(ctx, req.Email, string(hash))
	if err != nil {
		return nil, fmt.Errorf("Register: create user: %w", err)
	}

	deviceName := req.DeviceName
	if deviceName == "" {
		deviceName = "default"
	}
	deviceType := req.DeviceType
	if deviceType == "" {
		deviceType = "web"
	}

	device, err := s.repo.CreateDevice(ctx, user.ID, deviceName, deviceType)
	if err != nil {
		return nil, fmt.Errorf("Register: create device: %w", err)
	}

	return s.generateAuthResponse(ctx, user, device.ID)
}

// Login authenticates a user and returns tokens.
func (s *Service) Login(ctx context.Context, req *LoginRequest) (*AuthResponse, error) {
	user, err := s.repo.GetUserByEmail(ctx, req.Email)
	if err != nil {
		return nil, ErrInvalidCredentials
	}

	if err := bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(req.Password)); err != nil {
		return nil, ErrInvalidCredentials
	}

	deviceName := req.DeviceName
	if deviceName == "" {
		deviceName = "default"
	}
	deviceType := req.DeviceType
	if deviceType == "" {
		deviceType = "web"
	}

	// Upsert device
	device, err := s.repo.GetDevice(ctx, user.ID, deviceName, deviceType)
	if err != nil {
		device, err = s.repo.CreateDevice(ctx, user.ID, deviceName, deviceType)
		if err != nil {
			return nil, fmt.Errorf("Login: create device: %w", err)
		}
	} else {
		_ = s.repo.UpdateDeviceLastSeen(ctx, device.ID)
	}

	return s.generateAuthResponse(ctx, user, device.ID)
}

// RefreshToken rotates the refresh token.
func (s *Service) RefreshToken(ctx context.Context, req *RefreshRequest) (*AuthResponse, error) {
	tokenHash := hashToken(req.RefreshToken)

	rt, err := s.repo.GetRefreshToken(ctx, tokenHash)
	if err != nil {
		return nil, fmt.Errorf("RefreshToken: get token: %w", err)
	}

	if time.Now().After(rt.ExpiresAt) {
		_ = s.repo.DeleteRefreshToken(ctx, tokenHash)
		return nil, ErrTokenExpired
	}

	user, err := s.repo.GetUserByID(ctx, rt.UserID)
	if err != nil {
		return nil, fmt.Errorf("RefreshToken: get user: %w", err)
	}

	// Rotate: delete old, create new
	if err := s.repo.DeleteRefreshToken(ctx, tokenHash); err != nil {
		return nil, fmt.Errorf("RefreshToken: delete old token: %w", err)
	}

	return s.generateAuthResponse(ctx, user, rt.DeviceID)
}

// Logout invalidates a refresh token.
func (s *Service) Logout(ctx context.Context, req *LogoutRequest) error {
	tokenHash := hashToken(req.RefreshToken)
	return s.repo.DeleteRefreshToken(ctx, tokenHash)
}

// generateAuthResponse creates JWT access + refresh tokens.
func (s *Service) generateAuthResponse(ctx context.Context, user *User, deviceID uuid.UUID) (*AuthResponse, error) {
	accessToken, err := s.generateAccessToken(user)
	if err != nil {
		return nil, err
	}

	rawRefresh, err := generateRandomToken()
	if err != nil {
		return nil, err
	}

	refreshHash := hashToken(rawRefresh)
	expiresAt := time.Now().Add(time.Duration(s.cfg.RefreshExpiryDays) * 24 * time.Hour)

	_, err = s.repo.CreateRefreshToken(ctx, user.ID, refreshHash, deviceID, expiresAt)
	if err != nil {
		return nil, fmt.Errorf("generateAuthResponse: store refresh token: %w", err)
	}

	return &AuthResponse{
		AccessToken:  accessToken,
		RefreshToken: rawRefresh,
		User: UserResponse{
			ID:        user.ID.String(),
			Email:     user.Email,
			Role:      user.Role,
			CreatedAt: user.CreatedAt,
		},
	}, nil
}

// generateAccessToken creates a signed JWT for the user.
func (s *Service) generateAccessToken(user *User) (string, error) {
	claims := Claims{
		UserID: user.ID.String(),
		Role:   user.Role,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(time.Duration(s.cfg.JWTExpiryMin) * time.Minute)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
			Issuer:    "pixelvault",
		},
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	signed, err := token.SignedString([]byte(s.cfg.JWTSecret))
	if err != nil {
		return "", fmt.Errorf("generateAccessToken: sign: %w", err)
	}
	return signed, nil
}

// hashToken returns SHA-256 hex of the raw token.
func hashToken(raw string) string {
	h := sha256.Sum256([]byte(raw))
	return hex.EncodeToString(h[:])
}

// generateRandomToken returns a 32-byte base64url-encoded random string.
func generateRandomToken() (string, error) {
	b := make([]byte, 32)
	if _, err := rand.Read(b); err != nil {
		return "", fmt.Errorf("generateRandomToken: %w", err)
	}
	return base64.URLEncoding.EncodeToString(b), nil
}
