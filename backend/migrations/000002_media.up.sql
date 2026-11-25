CREATE TABLE IF NOT EXISTS media (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    filename TEXT NOT NULL,
    original_name TEXT NOT NULL,
    mime_type TEXT NOT NULL,
    size BIGINT NOT NULL DEFAULT 0,
    width INT DEFAULT 0,
    height INT DEFAULT 0,
    duration FLOAT,
    storage_path TEXT NOT NULL,
    thumb_path TEXT,
    taken_at TIMESTAMPTZ,
    lat FLOAT,
    lng FLOAT,
    is_favorite BOOLEAN DEFAULT FALSE,
    is_archived BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    media_type TEXT NOT NULL DEFAULT 'photo',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS albums (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT DEFAULT '',
    cover_id UUID REFERENCES media(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS album_media (
    album_id UUID NOT NULL REFERENCES albums(id) ON DELETE CASCADE,
    media_id UUID NOT NULL REFERENCES media(id) ON DELETE CASCADE,
    added_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY(album_id, media_id)
);

CREATE TABLE IF NOT EXISTS chunk_uploads (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    original_name TEXT NOT NULL,
    mime_type TEXT NOT NULL,
    total_size BIGINT NOT NULL,
    chunk_size BIGINT NOT NULL,
    total_chunks INT NOT NULL,
    uploaded_chunks INT DEFAULT 0,
    temp_path TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_media_user_id ON media(user_id);
CREATE INDEX IF NOT EXISTS idx_media_taken_at ON media(taken_at);
CREATE INDEX IF NOT EXISTS idx_media_is_deleted ON media(is_deleted);
CREATE INDEX IF NOT EXISTS idx_media_is_favorite ON media(is_favorite);
CREATE INDEX IF NOT EXISTS idx_media_is_archived ON media(is_archived);
CREATE INDEX IF NOT EXISTS idx_album_media_album ON album_media(album_id);
CREATE INDEX IF NOT EXISTS idx_album_media_media ON album_media(media_id);
