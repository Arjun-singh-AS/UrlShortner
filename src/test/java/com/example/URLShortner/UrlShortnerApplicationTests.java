package com.example.URLShortner;

import com.example.URLShortner.Service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:urlshortener-test;DB_CLOSE_DELAY=-1")
class UrlShortnerApplicationTests {

    @Autowired
    private UrlService urlService;

    @Test
    void createsShortLinkAndTracksRedirects() {
        UrlData created = urlService.create("https://example.com/a-long-path", null, null);

        assertThat(created.shortCode()).matches("[a-zA-Z0-9]{7}");
        assertThat(created.originalUrl()).isEqualTo("https://example.com/a-long-path");
        assertThat(created.clickCount()).isZero();

        UrlData visited = urlService.recordClickAndGet(created.shortCode());
        assertThat(visited.clickCount()).isEqualTo(1);
    }

    @Test
    void createsCustomAliasAndRejectsDuplicateAlias() {
        String alias = "test-" + UUID.randomUUID().toString().substring(0, 8);
        UrlData created = urlService.create("https://example.com", alias, null);

        assertThat(created.shortCode()).isEqualTo(alias);
        assertThatThrownBy(() -> urlService.create("https://another.example", alias, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void rejectsInvalidUrlsAliasesAndPastExpirations() {
        assertThatThrownBy(() -> urlService.create("javascript:alert(1)", null, null))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> urlService.create("https://example.com", "x", null))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> urlService.create("https://example.com", null, Instant.now().minusSeconds(1)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void expiredLinksCannotBeRedirected() throws InterruptedException {
        String alias = "old-" + UUID.randomUUID().toString().substring(0, 8);
        UrlData created = urlService.create("https://example.com", alias, Instant.now().plusMillis(100));

        Thread.sleep(200);
        assertThatThrownBy(() -> urlService.recordClickAndGet(created.shortCode()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.GONE));
    }

    @Test
    void deletesLinksAndReportsMissingLinks() {
        String alias = "del-" + UUID.randomUUID().toString().substring(0, 8);
        urlService.create("https://example.com", alias, null);

        urlService.delete(alias);

        assertThatThrownBy(() -> urlService.get(alias))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
