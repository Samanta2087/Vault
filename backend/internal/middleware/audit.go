package middleware

import (
	"context"
	"encoding/json"
	"log"

	"github.com/gofiber/fiber/v3"
	"github.com/jackc/pgx/v5/pgxpool"
)

// AuditLogger returns a middleware that logs requests to the audit_logs table.
func AuditLogger(pool *pgxpool.Pool) fiber.Handler {
	return func(c fiber.Ctx) error {
		// Execute the handler first
		err := c.Next()

		// Build audit details
		details := map[string]interface{}{
			"method": c.Method(),
			"path":   c.Path(),
			"status": c.Response().StatusCode(),
		}
		detailsJSON, _ := json.Marshal(details)

		// Get user ID if available
		var userID interface{}
		if uid, ok := c.Locals("userID").(string); ok && uid != "" {
			userID = uid
		}

		eventType := c.Method() + " " + c.Path()
		ip := c.IP()

		go func() {
			_, dbErr := pool.Exec(context.Background(),
				`INSERT INTO audit_logs (event_type, user_id, ip_address, details) VALUES ($1, $2, $3, $4)`,
				eventType, userID, ip, string(detailsJSON),
			)
			if dbErr != nil {
				log.Printf("audit log error: %v", dbErr)
			}
		}()

		return err
	}
}
