package middleware

import (
	"sync"
	"time"

	"github.com/gofiber/fiber/v3"
)

// bucket is a simple token bucket.
type bucket struct {
	tokens    float64
	max       float64
	refillPer time.Duration
	lastRefil time.Time
	mu        sync.Mutex
}

func newBucket(max float64, refillPer time.Duration) *bucket {
	return &bucket{
		tokens:    max,
		max:       max,
		refillPer: refillPer,
		lastRefil: time.Now(),
	}
}

func (b *bucket) allow() bool {
	b.mu.Lock()
	defer b.mu.Unlock()

	now := time.Now()
	elapsed := now.Sub(b.lastRefil)
	refilled := elapsed.Seconds() / b.refillPer.Seconds()
	b.tokens += refilled * b.max
	if b.tokens > b.max {
		b.tokens = b.max
	}
	b.lastRefil = now

	if b.tokens >= 1 {
		b.tokens--
		return true
	}
	return false
}

var (
	globalBuckets sync.Map
	authBuckets   sync.Map
)

func getOrCreateBucket(m *sync.Map, key string, max float64, refillPer time.Duration) *bucket {
	if v, ok := m.Load(key); ok {
		return v.(*bucket)
	}
	b := newBucket(max, refillPer)
	actual, _ := m.LoadOrStore(key, b)
	return actual.(*bucket)
}

// GlobalRateLimit returns a middleware allowing 100 req/s per IP globally.
func GlobalRateLimit() fiber.Handler {
	return func(c fiber.Ctx) error {
		ip := c.IP()
		b := getOrCreateBucket(&globalBuckets, ip, 100, time.Second)
		if !b.allow() {
			return c.Status(fiber.StatusTooManyRequests).JSON(fiber.Map{"error": "rate limit exceeded"})
		}
		return c.Next()
	}
}

// AuthRateLimit returns a middleware allowing 10 req/s per IP for auth routes.
func AuthRateLimit() fiber.Handler {
	return func(c fiber.Ctx) error {
		ip := c.IP()
		b := getOrCreateBucket(&authBuckets, ip, 10, time.Second)
		if !b.allow() {
			return c.Status(fiber.StatusTooManyRequests).JSON(fiber.Map{"error": "auth rate limit exceeded"})
		}
		return c.Next()
	}
}
