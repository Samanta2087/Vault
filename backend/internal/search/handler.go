package search

import (
	"strconv"
	"time"

	"github.com/gofiber/fiber/v3"
	"github.com/google/uuid"
)

// Handler holds the search service.
type Handler struct {
	svc *Service
}

// NewHandler creates a new search handler.
func NewHandler(svc *Service) *Handler {
	return &Handler{svc: svc}
}

// Search handles GET /api/search.
func (h *Handler) Search(c fiber.Ctx) error {
	userIDStr, ok := c.Locals("userID").(string)
	if !ok || userIDStr == "" {
		return fiber.NewError(fiber.StatusUnauthorized, "unauthenticated")
	}
	userID, err := uuid.Parse(userIDStr)
	if err != nil {
		return fiber.NewError(fiber.StatusUnauthorized, "invalid user ID")
	}

	params := SearchParams{
		Q:      c.Query("q"),
		Cursor: c.Query("cursor"),
		UserID: userID,
	}

	if limitStr := c.Query("limit"); limitStr != "" {
		if l, err := strconv.Atoi(limitStr); err == nil {
			params.Limit = l
		}
	}

	if df := c.Query("date_from"); df != "" {
		if t, err := time.Parse(time.RFC3339, df); err == nil {
			params.DateFrom = &t
		}
	}

	if dt := c.Query("date_to"); dt != "" {
		if t, err := time.Parse(time.RFC3339, dt); err == nil {
			params.DateTo = &t
		}
	}

	if albumIDStr := c.Query("album_id"); albumIDStr != "" {
		if id, err := uuid.Parse(albumIDStr); err == nil {
			params.AlbumID = &id
		}
	}

	if latStr := c.Query("lat"); latStr != "" {
		if la, err := strconv.ParseFloat(latStr, 64); err == nil {
			params.Lat = &la
		}
	}

	if lngStr := c.Query("lng"); lngStr != "" {
		if lo, err := strconv.ParseFloat(lngStr, 64); err == nil {
			params.Lng = &lo
		}
	}

	if radiusStr := c.Query("radius_km"); radiusStr != "" {
		if r, err := strconv.ParseFloat(radiusStr, 64); err == nil {
			params.RadiusKm = &r
		}
	}

	params.Favorite = c.Query("favorite") == "true"

	items, nextCursor, err := h.svc.Search(c.Context(), params)
	if err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "search failed")
	}

	return c.JSON(fiber.Map{
		"items":       items,
		"next_cursor": nextCursor,
	})
}
