package albums

import (
	"strconv"

	"github.com/gofiber/fiber/v3"
	"github.com/google/uuid"
)

// Handler holds the albums service.
type Handler struct {
	svc *Service
}

// NewHandler creates a new albums handler.
func NewHandler(svc *Service) *Handler {
	return &Handler{svc: svc}
}

// CreateAlbum handles POST /api/albums.
func (h *Handler) CreateAlbum(c fiber.Ctx) error {
	userID, err := getUserID(c)
	if err != nil {
		return err
	}

	var req CreateAlbumRequest
	if err := c.Bind().JSON(&req); err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid request body")
	}

	album, err := h.svc.CreateAlbum(c.Context(), userID, &req)
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, err.Error())
	}

	return c.Status(fiber.StatusCreated).JSON(album)
}

// ListAlbums handles GET /api/albums.
func (h *Handler) ListAlbums(c fiber.Ctx) error {
	userID, err := getUserID(c)
	if err != nil {
		return err
	}

	albums, err := h.svc.ListAlbums(c.Context(), userID)
	if err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to list albums")
	}

	return c.JSON(fiber.Map{"items": albums})
}

// GetAlbum handles GET /api/albums/:id.
func (h *Handler) GetAlbum(c fiber.Ctx) error {
	userID, err := getUserID(c)
	if err != nil {
		return err
	}

	id, err := uuid.Parse(c.Params("id"))
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid album ID")
	}

	album, err := h.svc.GetAlbum(c.Context(), id, userID)
	if err != nil {
		return fiber.NewError(fiber.StatusNotFound, "album not found")
	}

	return c.JSON(album)
}

// UpdateAlbum handles PUT /api/albums/:id.
func (h *Handler) UpdateAlbum(c fiber.Ctx) error {
	userID, err := getUserID(c)
	if err != nil {
		return err
	}

	id, err := uuid.Parse(c.Params("id"))
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid album ID")
	}

	var req UpdateAlbumRequest
	if err := c.Bind().JSON(&req); err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid request body")
	}

	album, err := h.svc.UpdateAlbum(c.Context(), id, userID, &req)
	if err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to update album")
	}

	return c.JSON(album)
}

// DeleteAlbum handles DELETE /api/albums/:id.
func (h *Handler) DeleteAlbum(c fiber.Ctx) error {
	userID, err := getUserID(c)
	if err != nil {
		return err
	}

	id, err := uuid.Parse(c.Params("id"))
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid album ID")
	}

	if err := h.svc.DeleteAlbum(c.Context(), id, userID); err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to delete album")
	}

	return c.Status(fiber.StatusNoContent).Send(nil)
}

// AddMedia handles POST /api/albums/:id/media.
func (h *Handler) AddMedia(c fiber.Ctx) error {
	_, err := getUserID(c)
	if err != nil {
		return err
	}

	albumID, err := uuid.Parse(c.Params("id"))
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid album ID")
	}

	var req AddMediaRequest
	if err := c.Bind().JSON(&req); err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid request body")
	}

	if err := h.svc.AddMedia(c.Context(), albumID, req.MediaIDs); err != nil {
		return fiber.NewError(fiber.StatusBadRequest, err.Error())
	}

	return c.Status(fiber.StatusNoContent).Send(nil)
}

// RemoveMedia handles DELETE /api/albums/:id/media/:mediaID.
func (h *Handler) RemoveMedia(c fiber.Ctx) error {
	_, err := getUserID(c)
	if err != nil {
		return err
	}

	albumID, err := uuid.Parse(c.Params("id"))
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid album ID")
	}

	mediaID, err := uuid.Parse(c.Params("mediaID"))
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid media ID")
	}

	if err := h.svc.RemoveMedia(c.Context(), albumID, mediaID); err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to remove media from album")
	}

	return c.Status(fiber.StatusNoContent).Send(nil)
}

// GetAlbumMedia handles GET /api/albums/:id/media.
func (h *Handler) GetAlbumMedia(c fiber.Ctx) error {
	_, err := getUserID(c)
	if err != nil {
		return err
	}

	albumID, err := uuid.Parse(c.Params("id"))
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid album ID")
	}

	cursor := c.Query("cursor")
	limit, _ := strconv.Atoi(c.Query("limit", "50"))

	items, nextCursor, err := h.svc.GetAlbumMedia(c.Context(), albumID, cursor, limit)
	if err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to get album media")
	}

	return c.JSON(fiber.Map{
		"items":       items,
		"next_cursor": nextCursor,
	})
}

func getUserID(c fiber.Ctx) (uuid.UUID, error) {
	userIDStr, ok := c.Locals("userID").(string)
	if !ok || userIDStr == "" {
		return uuid.UUID{}, fiber.NewError(fiber.StatusUnauthorized, "unauthenticated")
	}
	id, err := uuid.Parse(userIDStr)
	if err != nil {
		return uuid.UUID{}, fiber.NewError(fiber.StatusUnauthorized, "invalid user ID")
	}
	return id, nil
}
