package auth

import (
	"strings"

	"github.com/golang-jwt/jwt/v5"
	"github.com/gofiber/fiber/v3"
	"pixelvault/backend/internal/config"
)

// JWTMiddleware validates Authorization: Bearer tokens.
func JWTMiddleware(cfg *config.Config) fiber.Handler {
	return func(c fiber.Ctx) error {
		authorization := c.Get("Authorization")
		if authorization == "" {
			return fiber.NewError(fiber.StatusUnauthorized, "missing authorization header")
		}

		parts := strings.SplitN(authorization, " ", 2)
		if len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") {
			return fiber.NewError(fiber.StatusUnauthorized, "invalid authorization header format")
		}

		tokenStr := parts[1]
		claims := &Claims{}

		token, err := jwt.ParseWithClaims(tokenStr, claims, func(token *jwt.Token) (interface{}, error) {
			if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
				return nil, fiber.NewError(fiber.StatusUnauthorized, "unexpected signing method")
			}
			return []byte(cfg.JWTSecret), nil
		})

		if err != nil || !token.Valid {
			return fiber.NewError(fiber.StatusUnauthorized, "invalid or expired token")
		}

		c.Locals("userID", claims.UserID)
		c.Locals("role", claims.Role)
		return c.Next()
	}
}

// AdminMiddleware checks that the authenticated user has the "admin" role.
func AdminMiddleware() fiber.Handler {
	return func(c fiber.Ctx) error {
		role, ok := c.Locals("role").(string)
		if !ok || role != "admin" {
			return fiber.NewError(fiber.StatusForbidden, "admin access required")
		}
		return c.Next()
	}
}
