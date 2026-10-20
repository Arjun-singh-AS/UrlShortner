package com.example.URLShortner.Service;

import com.example.URLShortner.UrlData;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UrlService {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 7;
    private static final int MAX_URL_LENGTH = 2048;
    private static final Pattern VALID_CODE = Pattern.compile("[a-zA-Z0-9_-]{3,32}");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<UrlData> urlRowMapper = (rs, rowNumber) -> {
        OffsetDateTime createdAt = rs.getObject("created_at", OffsetDateTime.class);
        OffsetDateTime expiresAt = rs.getObject("expires_at", OffsetDateTime.class);
        return new UrlData(
                rs.getString("short_code"),
                rs.getString("original_url"),
                createdAt.toInstant(),
                expiresAt == null ? null : expiresAt.toInstant(),
                rs.getLong("click_count"));
    };

    public UrlService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UrlData create(String rawUrl, String rawAlias, Instant expiresAt) {
        String originalUrl = validateUrl(rawUrl);
        String alias = rawAlias == null || rawAlias.isBlank() ? null : rawAlias.trim();

        if (alias != null && !VALID_CODE.matcher(alias).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Custom aliases must be 3-32 letters, numbers, hyphens, or underscores.");
        }
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expiration must be in the future.");
        }

        if (alias != null) {
            try {
                return insert(alias, originalUrl, expiresAt);
            } catch (DuplicateKeyException exception) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "That custom alias is already in use.");
            }
        }

        for (int attempt = 0; attempt < 10; attempt++) {
            try {
                return insert(generateCode(), originalUrl, expiresAt);
            } catch (DuplicateKeyException ignored) {
                // A generated collision is rare; retry with a fresh code.
            }
        }
        throw new IllegalStateException("Could not generate a unique short code after 10 attempts.");
    }

    public List<UrlData> list() {
        return jdbcTemplate.query(
                "SELECT short_code, original_url, created_at, expires_at, click_count " +
                        "FROM shortened_urls ORDER BY created_at DESC",
                urlRowMapper);
    }

    public UrlData get(String shortCode) {
        return jdbcTemplate.query(
                        "SELECT short_code, original_url, created_at, expires_at, click_count " +
                                "FROM shortened_urls WHERE short_code = ?",
                        urlRowMapper,
                        shortCode)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Short link not found."));
    }

    public UrlData recordClickAndGet(String shortCode) {
        int updated = jdbcTemplate.update(
                "UPDATE shortened_urls SET click_count = click_count + 1 " +
                        "WHERE short_code = ? AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)",
                shortCode);
        if (updated == 0) {
            UrlData link = get(shortCode);
            if (link.isExpired()) {
                throw new ResponseStatusException(HttpStatus.GONE, "This short link has expired.");
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Short link not found.");
        }
        return get(shortCode);
    }

    public void delete(String shortCode) {
        if (jdbcTemplate.update("DELETE FROM shortened_urls WHERE short_code = ?", shortCode) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Short link not found.");
        }
    }

    private UrlData insert(String code, String originalUrl, Instant expiresAt) {
        Instant createdAt = Instant.now();
        jdbcTemplate.update(
                "INSERT INTO shortened_urls (short_code, original_url, created_at, expires_at, click_count) " +
                        "VALUES (?, ?, ?, ?, 0)",
                code,
                originalUrl,
                OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC),
                expiresAt == null ? null : OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
        return new UrlData(code, originalUrl, createdAt, expiresAt, 0);
    }

    private String validateUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank() || rawUrl.length() > MAX_URL_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a URL up to 2048 characters long.");
        }

        try {
            URI uri = URI.create(rawUrl.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Enter a complete http:// or https:// URL.");
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid URL.");
        }
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int index = 0; index < CODE_LENGTH; index++) {
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
