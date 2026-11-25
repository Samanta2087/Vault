package admin

import (
	"context"
	"fmt"
	"strconv"

	"github.com/gofiber/fiber/v3"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

// Handler holds admin dependencies.
type Handler struct {
	pool *pgxpool.Pool
}

// NewHandler creates a new admin handler.
func NewHandler(pool *pgxpool.Pool) *Handler {
	return &Handler{pool: pool}
}

// Stats holds server statistics.
type Stats struct {
	UserCount  int64 `json:"user_count"`
	MediaCount int64 `json:"media_count"`
	TotalBytes int64 `json:"total_bytes"`
}

// UserRecord represents a user in the admin list.
type UserRecord struct {
	ID         string `json:"id"`
	Email      string `json:"email"`
	Role       string `json:"role"`
	CreatedAt  string `json:"created_at"`
	MediaCount int64  `json:"media_count"`
}

// GetStats handles GET /api/admin/stats.
func (h *Handler) GetStats(c fiber.Ctx) error {
	var stats Stats
	err := h.pool.QueryRow(context.Background(),
		`SELECT
		(SELECT COUNT(*) FROM users) AS user_count,
		(SELECT COUNT(*) FROM media WHERE is_deleted = false) AS media_count,
		(SELECT COALESCE(SUM(size), 0) FROM media WHERE is_deleted = false) AS total_bytes`,
	).Scan(&stats.UserCount, &stats.MediaCount, &stats.TotalBytes)
	if err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to get stats")
	}
	return c.JSON(stats)
}

// ListUsers handles GET /api/admin/users.
func (h *Handler) ListUsers(c fiber.Ctx) error {
	page := 1
	if p, err := strconv.Atoi(c.Query("page", "1")); err == nil && p > 0 {
		page = p
	}
	perPage := 50
	offset := (page - 1) * perPage

	rows, err := h.pool.Query(context.Background(),
		`SELECT u.id, u.email, u.role, u.created_at::text,
		COUNT(m.id) as media_count
		FROM users u
		LEFT JOIN media m ON u.id = m.user_id AND m.is_deleted = false
		GROUP BY u.id
		ORDER BY u.created_at DESC
		LIMIT $1 OFFSET $2`,
		perPage, offset,
	)
	if err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to list users")
	}
	defer rows.Close()

	var users []UserRecord
	for rows.Next() {
		var u UserRecord
		if err := rows.Scan(&u.ID, &u.Email, &u.Role, &u.CreatedAt, &u.MediaCount); err != nil {
			return fiber.NewError(fiber.StatusInternalServerError, "failed to scan user")
		}
		users = append(users, u)
	}
	if err := rows.Err(); err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, err.Error())
	}

	return c.JSON(fiber.Map{"users": users, "page": page})
}

// DeleteUser handles DELETE /api/admin/users/:id.
func (h *Handler) DeleteUser(c fiber.Ctx) error {
	userID, err := uuid.Parse(c.Params("id"))
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid user ID")
	}

	_, err = h.pool.Exec(context.Background(), `DELETE FROM users WHERE id=$1`, userID)
	if err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, fmt.Sprintf("failed to delete user: %v", err))
	}

	return c.Status(fiber.StatusNoContent).Send(nil)
}

// UpdateUserRoleRequest is the payload for role updates.
type UpdateUserRoleRequest struct {
	Role string `json:"role"`
}

// UpdateUserRole handles PUT /api/admin/users/:id/role.
func (h *Handler) UpdateUserRole(c fiber.Ctx) error {
	userID, err := uuid.Parse(c.Params("id"))
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid user ID")
	}

	var req UpdateUserRoleRequest
	if err := c.Bind().JSON(&req); err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid request body")
	}

	if req.Role != "user" && req.Role != "admin" {
		return fiber.NewError(fiber.StatusBadRequest, "role must be 'user' or 'admin'")
	}

	_, err = h.pool.Exec(context.Background(),
		`UPDATE users SET role=$1, updated_at=NOW() WHERE id=$2`,
		req.Role, userID,
	)
	if err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to update role")
	}

	return c.JSON(fiber.Map{"message": "role updated"})
}
