package com.example.URLShortner.Controller;

import com.example.URLShortner.Service.UrlService;
import com.example.URLShortner.UrlData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RedirectController {
    private final UrlService urlService;

    public RedirectController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("/{shortCode:[a-zA-Z0-9_-]+}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        UrlData link = urlService.recordClickAndGet(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(link.originalUrl()))
                .build();
    }
}
