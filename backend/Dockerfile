# ── Build stage ──────────────────────────────────────────────
FROM golang:1.22-alpine AS builder

WORKDIR /app

# Install build dependencies
RUN apk add --no-cache git ca-certificates tzdata

# Cache dependencies
COPY go.mod go.sum ./
RUN go mod download

# Copy source
COPY . .

# Build the binary
RUN CGO_ENABLED=0 GOOS=linux go build \
    -ldflags="-s -w" \
    -o pixelvault \
    ./cmd/server

# ── Run stage ────────────────────────────────────────────────
FROM alpine:3.21

RUN apk add --no-cache ca-certificates tzdata

WORKDIR /app

# Copy binary and migrations
COPY --from=builder /app/pixelvault .
COPY --from=builder /app/migrations ./migrations

# Create storage directory
RUN mkdir -p /app/storage

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/health || exit 1

CMD ["./pixelvault"]
