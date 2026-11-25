CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE media ADD COLUMN IF NOT EXISTS search_vector tsvector;

CREATE INDEX IF NOT EXISTS idx_media_search_vector ON media USING GIN(search_vector);
CREATE INDEX IF NOT EXISTS idx_media_original_name_trgm ON media USING GIN(original_name gin_trgm_ops);

CREATE OR REPLACE FUNCTION media_search_vector_update() RETURNS trigger AS $$
BEGIN
  NEW.search_vector := to_tsvector('english',
    COALESCE(NEW.original_name, '') || ' ' || COALESCE(NEW.mime_type, ''));
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS media_search_vector_trigger ON media;

CREATE TRIGGER media_search_vector_trigger
BEFORE INSERT OR UPDATE ON media
FOR EACH ROW EXECUTE FUNCTION media_search_vector_update();

-- Backfill existing rows
UPDATE media SET search_vector = to_tsvector('english',
  COALESCE(original_name, '') || ' ' || COALESCE(mime_type, ''));
