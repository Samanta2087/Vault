package media

import (
	"bytes"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"fmt"
	"io"
	"math"
	"mime"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/gofiber/fiber/v3"
	"github.com/google/uuid"
	"github.com/rwcarlsen/goexif/exif"
	"pixelvault/backend/internal/config"
)

// Handler holds dependencies for media endpoints.
type Handler struct {
	repo    *Repository
	storage Storage
	cfg     *config.Config
}

// NewHandler creates a new media handler.
func NewHandler(repo *Repository, storage Storage, cfg *config.Config) *Handler {
	return &Handler{repo: repo, storage: storage, cfg: cfg}
}

// InitUpload handles POST /api/media/upload/init.
func (h *Handler) InitUpload(c fiber.Ctx) error {
	userID, err := getUserID(c)
	if err != nil {
		return err
	}

	var req InitUploadRequest
	if err := c.Bind().JSON(&req); err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid request body")
	}
	if req.Filename == "" || req.MimeType == "" || req.TotalSize <= 0 {
		return fiber.NewError(fiber.StatusBadRequest, "filename, mime_type and total_size are required")
	}

	chunkSize := int64(h.cfg.UploadChunkSizeMB) * 1024 * 1024
	totalChunks := int(math.Ceil(float64(req.TotalSize) / float64(chunkSize)))
	tempDir := filepath.Join(os.TempDir(), "pixelvault_chunks")
	_ = os.MkdirAll(tempDir, 0755)
	tempPath := filepath.Join(tempDir, uuid.New().String())

	cu := &ChunkUpload{
		UserID:       userID,
		OriginalName: req.Filename,
		MimeType:     req.MimeType,
		TotalSize:    req.TotalSize,
		ChunkSize:    chunkSize,
		TotalChunks:  totalChunks,
		TempPath:     tempPath,
	}

	cu, err = h.repo.CreateChunkUpload(c.Context(), cu)
	if err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to create upload session")
	}

	return c.Status(fiber.StatusCreated).JSON(InitUploadResponse{
		UploadID:  cu.ID.String(),
		ChunkSize: chunkSize,
	})
}

// UploadChunk handles PATCH /api/media/upload/:id.
func (h *Handler) UploadChunk(c fiber.Ctx) error {
	userID, err := getUserID(c)
	if err != nil {
		return err
	}

	uploadID, err := uuid.Parse(c.Params("id"))
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid upload ID")
	}

	cu, err := h.repo.GetChunkUpload(c.Context(), uploadID)
	if err != nil {
		return fiber.NewError(fiber.StatusNotFound, "upload session not found")
	}
	if cu.UserID != userID {
		return fiber.NewError(fiber.StatusForbidden, "access denied")
	}

	chunkIndexStr := c.Get("X-Chunk-Index")
	chunkIndex, err := strconv.Atoi(chunkIndexStr)
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "X-Chunk-Index header required")
	}

	// Open temp file for writing (append at chunk offset)
	f, err := os.OpenFile(cu.TempPath, os.O_CREATE|os.O_WRONLY, 0644)
	if err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to open temp file")
	}
	defer f.Close()

	offset := int64(chunkIndex) * cu.ChunkSize
	if _, err := f.Seek(offset, io.SeekStart); err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to seek temp file")
	}

	body := c.Body()
	if _, err := f.Write(body); err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to write chunk")
	}

	newCount := cu.UploadedChunks + 1
	if err := h.repo.UpdateChunkUpload(c.Context(), uploadID, newCount); err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to update upload progress")
	}

	return c.JSON(fiber.Map{
		"uploaded_chunks": newCount,
		"total_chunks":    cu.TotalChunks,
	})
}

// CompleteUpload handles POST /api/media/upload/:id/complete.
func (h *Handler) CompleteUpload(c fiber.Ctx) error {
	userID, err := getUserID(c)
	if err != nil {
		return err
	}

	uploadID, err := uuid.Parse(c.Params("id"))
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid upload ID")
	}

	cu, err := h.repo.GetChunkUpload(c.Context(), uploadID)
	if err != nil {
		return fiber.NewError(fiber.StatusNotFound, "upload session not found")
	}
	if cu.UserID != userID {
		return fiber.NewError(fiber.StatusForbidden, "access denied")
	}

	// Open assembled temp file
	f, err := os.Open(cu.TempPath)
	if err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to open assembled file")
	}
	defer f.Close()
	defer os.Remove(cu.TempPath)

	fi, err := f.Stat()
	if err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to stat assembled file")
	}

	// Extract EXIF metadata
	var takenAt *time.Time
	var lat, lng *float64
	var width, height int

	if isImageMime(cu.MimeType) {
		// Try EXIF extraction on a copy (reader consumed once)
		f.Seek(0, io.SeekStart)
		if x, err := exif.Decode(f); err == nil {
			if t, err := x.DateTime(); err == nil {
				takenAt = &t
			}
			if la, lo, err := x.LatLong(); err == nil {
				lat = &la
				lng = &lo
			}
			if tag, err := x.Get(exif.PixelXDimension); err == nil {
				if v, err := tag.Int(0); err == nil {
					width = v
				}
			}
			if tag, err := x.Get(exif.PixelYDimension); err == nil {
				if v, err := tag.Int(0); err == nil {
					height = v
				}
			}
		}
	}

	// Determine media type
	mType := MediaTypePhoto
	if isVideoMime(cu.MimeType) {
		mType = MediaTypeVideo
	}

	// Generate unique filename
	ext := filepath.Ext(cu.OriginalName)
	newFilename := uuid.New().String() + ext
	storagePath := fmt.Sprintf("media/%s/%s", userID.String(), newFilename)

	// Upload to storage
	f.Seek(0, io.SeekStart)
	if err := h.storage.Put(c.Context(), storagePath, f, fi.Size(), cu.MimeType); err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to store file")
	}

	// Generate thumbnail for images
	thumbPath := ""
	if isImageMime(cu.MimeType) {
		f.Seek(0, io.SeekStart)
		var thumbBuf bytes.Buffer
		if err := GenerateThumbnail(f, &thumbBuf, h.cfg.ThumbnailSize); err == nil {
			thumbPath = fmt.Sprintf("thumbs/%s/%s.jpg", userID.String(), uuid.New().String())
			_ = h.storage.Put(c.Context(), thumbPath, &thumbBuf, int64(thumbBuf.Len()), "image/jpeg")
		}
	}

	m := &Media{
		UserID:       userID,
		Filename:     newFilename,
		OriginalName: cu.OriginalName,
		MimeType:     cu.MimeType,
		Size:         fi.Size(),
		Width:        width,
		Height:       height,
		StoragePath:  storagePath,
		ThumbPath:    thumbPath,
		TakenAt:      takenAt,
		Lat:          lat,
		Lng:          lng,
		MediaType:    mType,
	}

	created, err := h.repo.Create(c.Context(), m)
	if err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to create media record")
	}

	_ = h.repo.DeleteChunkUpload(c.Context(), uploadID)

	return c.Status(fiber.StatusCreated).JSON(created)
}

// ListMedia handles GET /api/media.
func (h *Handler) ListMedia(c fiber.Ctx) error {
	userID, err := getUserID(c)
	if err != nil {
		return err
	}

	cursor := c.Query("cursor")
	limit, _ := strconv.Atoi(c.Query("limit", "50"))
	archived := c.Query("archived") == "true"
	favorite := c.Query("favorite") == "true"

	items, nextCursor, err := h.repo.ListByUserCursor(c.Context(), userID, cursor, limit, archived, favorite)
	if err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to list media")
	}

	return c.JSON(fiber.Map{
		"items":       items,
		"next_cursor": nextCursor,
	})
}

// GetMedia handles GET /api/media/:id.
func (h *Handler) GetMedia(c fiber.Ctx) error {
	userID, err := getUserID(c)
	if err != nil {
		return err
	}

	id, err := uuid.Parse(c.Params("id"))
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid media ID")
	}

	m, err := h.repo.GetByID(c.Context(), id)
	if err != nil {
		return fiber.NewError(fiber.StatusNotFound, "media not found")
	}
	if m.UserID != userID {
		return fiber.NewError(fiber.StatusForbidden, "access denied")
	}

	return c.JSON(m)
}

// SoftDeleteMedia handles DELETE /api/media/:id.
func (h *Handler) SoftDeleteMedia(c fiber.Ctx) error {
	userID, err := getUserID(c)
	if err != nil {
		return err
	}

	id, err := uuid.Parse(c.Params("id"))
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid media ID")
	}

	if err := h.repo.SoftDelete(c.Context(), id, userID); err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to delete media")
	}

	return c.Status(fiber.StatusNoContent).Send(nil)
}

// HardDeleteMedia handles DELETE /api/media/:id/permanent.
func (h *Handler) HardDeleteMedia(c fiber.Ctx) error {
	userID, err := getUserID(c)
	if err != nil {
		return err
	}

	id, err := uuid.Parse(c.Params("id"))
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid media ID")
	}

	// Get the media to delete storage files
	m, err := h.repo.GetByID(c.Context(), id)
	if err != nil {
		return fiber.NewError(fiber.StatusNotFound, "media not found")
	}
	if m.UserID != userID {
		return fiber.NewError(fiber.StatusForbidden, "access denied")
	}

	_ = h.storage.Delete(c.Context(), m.StoragePath)
	if m.ThumbPath != "" {
		_ = h.storage.Delete(c.Context(), m.ThumbPath)
	}

	if err := h.repo.HardDelete(c.Context(), id, userID); err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to permanently delete media")
	}

	return c.Status(fiber.StatusNoContent).Send(nil)
}

// ToggleFavorite handles PUT /api/media/:id/favorite.
func (h *Handler) ToggleFavorite(c fiber.Ctx) error {
	userID, err := getUserID(c)
	if err != nil {
		return err
	}

	id, err := uuid.Parse(c.Params("id"))
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid media ID")
	}

	m, err := h.repo.GetByID(c.Context(), id)
	if err != nil {
		return fiber.NewError(fiber.StatusNotFound, "media not found")
	}
	if m.UserID != userID {
		return fiber.NewError(fiber.StatusForbidden, "access denied")
	}

	if err := h.repo.SetFavorite(c.Context(), id, userID, !m.IsFavorite); err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to toggle favorite")
	}

	m.IsFavorite = !m.IsFavorite
	return c.JSON(m)
}

// ToggleArchive handles PUT /api/media/:id/archive.
func (h *Handler) ToggleArchive(c fiber.Ctx) error {
	userID, err := getUserID(c)
	if err != nil {
		return err
	}

	id, err := uuid.Parse(c.Params("id"))
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid media ID")
	}

	m, err := h.repo.GetByID(c.Context(), id)
	if err != nil {
		return fiber.NewError(fiber.StatusNotFound, "media not found")
	}
	if m.UserID != userID {
		return fiber.NewError(fiber.StatusForbidden, "access denied")
	}

	if err := h.repo.SetArchived(c.Context(), id, userID, !m.IsArchived); err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to toggle archive")
	}

	m.IsArchived = !m.IsArchived
	return c.JSON(m)
}

// ServeThumbnail handles GET /api/media/:id/thumbnail.
func (h *Handler) ServeThumbnail(c fiber.Ctx) error {
	userID, err := getUserID(c)
	if err != nil {
		return err
	}

	id, err := uuid.Parse(c.Params("id"))
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid media ID")
	}

	m, err := h.repo.GetByID(c.Context(), id)
	if err != nil {
		return fiber.NewError(fiber.StatusNotFound, "media not found")
	}
	if m.UserID != userID {
		return fiber.NewError(fiber.StatusForbidden, "access denied")
	}

	if m.ThumbPath == "" {
		return fiber.NewError(fiber.StatusNotFound, "thumbnail not available")
	}

	reader, err := h.storage.Get(c.Context(), m.ThumbPath)
	if err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to get thumbnail")
	}
	defer reader.Close()

	c.Set("Content-Type", "image/jpeg")
	c.Set("Cache-Control", "public, max-age=86400")
	_, err = io.Copy(c.Response().BodyWriter(), reader)
	return err
}

// GetSignedURL handles GET /api/media/signed-url/:id.
func (h *Handler) GetSignedURL(c fiber.Ctx) error {
	userID, err := getUserID(c)
	if err != nil {
		return err
	}

	id, err := uuid.Parse(c.Params("id"))
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid media ID")
	}

	m, err := h.repo.GetByID(c.Context(), id)
	if err != nil {
		return fiber.NewError(fiber.StatusNotFound, "media not found")
	}
	if m.UserID != userID {
		return fiber.NewError(fiber.StatusForbidden, "access denied")
	}

	expiresAt := time.Now().Add(time.Hour)
	exp := strconv.FormatInt(expiresAt.Unix(), 10)

	mac := hmac.New(sha256.New, []byte(h.cfg.SignedURLSecret))
	mac.Write([]byte(id.String() + ":" + exp))
	sig := base64.URLEncoding.EncodeToString(mac.Sum(nil))

	token := base64.URLEncoding.EncodeToString([]byte(sig))
	url := fmt.Sprintf("/api/media/file/%s?id=%s&exp=%s", token, id.String(), exp)

	return c.JSON(SignedURLResponse{
		URL:       url,
		ExpiresAt: expiresAt,
	})
}

// ServeFile handles GET /api/media/file/:token.
func (h *Handler) ServeFile(c fiber.Ctx) error {
	tokenB64 := c.Params("token")
	mediaIDStr := c.Query("id")
	exp := c.Query("exp")

	if tokenB64 == "" || mediaIDStr == "" || exp == "" {
		return fiber.NewError(fiber.StatusBadRequest, "missing token parameters")
	}

	// Validate expiry
	expUnix, err := strconv.ParseInt(exp, 10, 64)
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid expiry")
	}
	if time.Now().Unix() > expUnix {
		return fiber.NewError(fiber.StatusGone, "signed URL has expired")
	}

	// Decode and verify HMAC
	sigBytes, err := base64.URLEncoding.DecodeString(tokenB64)
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid token encoding")
	}

	mac := hmac.New(sha256.New, []byte(h.cfg.SignedURLSecret))
	mac.Write([]byte(mediaIDStr + ":" + exp))
	expectedSig := base64.URLEncoding.EncodeToString(mac.Sum(nil))

	if !hmac.Equal(sigBytes, []byte(expectedSig)) {
		return fiber.NewError(fiber.StatusUnauthorized, "invalid token signature")
	}

	id, err := uuid.Parse(mediaIDStr)
	if err != nil {
		return fiber.NewError(fiber.StatusBadRequest, "invalid media ID")
	}

	m, err := h.repo.GetByID(c.Context(), id)
	if err != nil {
		return fiber.NewError(fiber.StatusNotFound, "media not found")
	}

	reader, err := h.storage.Get(c.Context(), m.StoragePath)
	if err != nil {
		return fiber.NewError(fiber.StatusInternalServerError, "failed to get file")
	}
	defer reader.Close()

	ext := filepath.Ext(m.OriginalName)
	contentType := mime.TypeByExtension(ext)
	if contentType == "" {
		contentType = m.MimeType
	}

	c.Set("Content-Type", contentType)
	c.Set("Content-Disposition", fmt.Sprintf(`attachment; filename="%s"`, m.OriginalName))
	_, err = io.Copy(c.Response().BodyWriter(), reader)
	return err
}

// getUserID extracts the authenticated user's UUID from locals.
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

func isImageMime(mime string) bool {
	return strings.HasPrefix(mime, "image/")
}

func isVideoMime(mime string) bool {
	return strings.HasPrefix(mime, "video/")
}
