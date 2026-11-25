package auth

import (
	"errors"

	"github.com/gofiber/fiber/v3"
	"github.com/jackc/pgx/v5"
)

// Handler holds the auth service.
type Handler struct {
	svc *Service
}

// NewHandler creates a new auth handler.
func NewHandler(svc *Service) *Handler {
	return &Handler{svc: svc}
}

// Register handles POST /api/auth/register.
func (h *Handler) Register(c fiber.Ctx) error {
	var req RegisterRequest
	if err := c.Bind().JSON(&req); err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid request body")
	}
	if req.Email == "" || req.Password == "" {
		return fiber.NewError(fiber.StatusBadRequest, "email and password are required")
	}
	if len(req.Password) < 8 {
		return fiber.NewError(fiber.StatusBadRequest, "password must be at least 8 characters")
	}

	resp, err := h.svc.Register(c.Context(), &req)
	if err != nil {
		if isPgUniqueViolation(err) {
			return fiber.NewError(fiber.StatusConflict, "email already registered")
		}
		return fiber.NewError(fiber.StatusInternalServerError, "registration failed")
	}

	return c.Status(fiber.StatusCreated).JSON(resp)
}

// Login handles POST /api/auth/login.
func (h *Handler) Login(c fiber.Ctx) error {
	var req LoginRequest
	if err := c.Bind().JSON(&req); err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid request body")
	}
	if req.Email == "" || req.Password == "" {
		return fiber.NewError(fiber.StatusBadRequest, "email and password are required")
	}

	resp, err := h.svc.Login(c.Context(), &req)
	if err != nil {
		if errors.Is(err, ErrInvalidCredentials) {
			return fiber.NewError(fiber.StatusUnauthorized, "invalid credentials")
		}
		return fiber.NewError(fiber.StatusInternalServerError, "login failed")
	}

	return c.JSON(resp)
}

// Refresh handles POST /api/auth/refresh.
func (h *Handler) Refresh(c fiber.Ctx) error {
	var req RefreshRequest
	if err := c.Bind().JSON(&req); err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid request body")
	}
	if req.RefreshToken == "" {
		return fiber.NewError(fiber.StatusBadRequest, "refresh_token is required")
	}

	resp, err := h.svc.RefreshToken(c.Context(), &req)
	if err != nil {
		if errors.Is(err, ErrTokenExpired) {
			return fiber.NewError(fiber.StatusUnauthorized, "refresh token expired")
		}
		if errors.Is(err, pgx.ErrNoRows) {
			return fiber.NewError(fiber.StatusUnauthorized, "invalid refresh token")
		}
		return fiber.NewError(fiber.StatusInternalServerError, "token refresh failed")
	}

	return c.JSON(resp)
}

// Logout handles POST /api/auth/logout.
func (h *Handler) Logout(c fiber.Ctx) error {
	var req LogoutRequest
	if err := c.Bind().JSON(&req); err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid request body")
	}
	if req.RefreshToken == "" {
		return fiber.NewError(fiber.StatusBadRequest, "refresh_token is required")
	}

	_ = h.svc.Logout(c.Context(), &req)
	return c.Status(fiber.StatusNoContent).Send(nil)
}

// isPgUniqueViolation checks for PostgreSQL unique constraint violations.
func isPgUniqueViolation(err error) bool {
	if err == nil {
		return false
	}
	return containsCode(err, "23505")
}

func containsCode(err error, code string) bool {
	type pgErr interface {
		SQLState() string
	}
	var pe pgErr
	if errors.As(err, &pe) {
		return pe.SQLState() == code
	}
	return false
}
