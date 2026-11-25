package media

import (
	"fmt"
	"io"

	"github.com/disintegration/imaging"
)

// GenerateThumbnail reads an image from src, resizes it to fit within size×size,
// and writes a JPEG to dst.
func GenerateThumbnail(src io.Reader, dst io.Writer, size int) error {
	img, err := imaging.Decode(src, imaging.AutoOrientation(true))
	if err != nil {
		return fmt.Errorf("GenerateThumbnail: decode: %w", err)
	}

	thumb := imaging.Fit(img, size, size, imaging.Lanczos)

	if err := imaging.Encode(dst, thumb, imaging.JPEG, imaging.JPEGQuality(80)); err != nil {
		return fmt.Errorf("GenerateThumbnail: encode: %w", err)
	}

	return nil
}
