CREATE TABLE IF NOT EXISTS shortened_urls (
    short_code VARCHAR(32) PRIMARY KEY,
    original_url VARCHAR(2048) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    click_count BIGINT NOT NULL DEFAULT 0 CHECK (click_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_shortened_urls_created_at ON shortened_urls (created_at DESC);
