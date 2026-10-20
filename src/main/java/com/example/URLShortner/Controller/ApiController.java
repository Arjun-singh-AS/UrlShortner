package com.example.URLShortner.Controller;

import com.example.URLShortner.Service.UrlService;
import com.example.URLShortner.UrlData;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final UrlService urlService;

    public ApiController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping("/shorten")
    public UrlResponse shorten(@RequestBody CreateUrlRequest request, HttpServletRequest httpRequest) {
        UrlData created = urlService.create(request.url(), request.customAlias(), request.expiresAt());
        return toResponse(created, httpRequest);
    }

    @GetMapping("/urls")
    public List<UrlResponse> list(HttpServletRequest request) {
        return urlService.list().stream().map(url -> toResponse(url, request)).toList();
    }

    @GetMapping("/urls/{shortCode}")
    public UrlResponse get(@PathVariable String shortCode, HttpServletRequest request) {
        return toResponse(urlService.get(shortCode), request);
    }

    @DeleteMapping("/urls/{shortCode}")
    public ResponseEntity<Void> delete(@PathVariable String shortCode) {
        urlService.delete(shortCode);
        return ResponseEntity.noContent().build();
    }

    private UrlResponse toResponse(UrlData url, HttpServletRequest request) {
        int port = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(request.getScheme()) && port == 80)
                || ("https".equalsIgnoreCase(request.getScheme()) && port == 443);
        String shortUrl = UriComponentsBuilder.newInstance()
                .scheme(request.getScheme())
                .host(request.getServerName())
                .port(defaultPort ? -1 : port)
                .pathSegment(url.shortCode())
                .build()
                .toUriString();
        return new UrlResponse(
                url.shortCode(),
                url.originalUrl(),
                shortUrl,
                url.createdAt(),
                url.expiresAt(),
                url.clickCount(),
                url.isExpired());
    }

    public record CreateUrlRequest(String url, String customAlias, Instant expiresAt) {
    }

    public record UrlResponse(
            String shortCode,
            String originalUrl,
            String shortUrl,
            Instant createdAt,
            Instant expiresAt,
            long clickCount,
            boolean expired) {
    }
}
