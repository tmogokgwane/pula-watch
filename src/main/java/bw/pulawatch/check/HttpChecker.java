package bw.pulawatch.check;

import bw.pulawatch.config.CheckProperties;
import bw.pulawatch.security.UnsafeUrlException;
import bw.pulawatch.security.UrlGuard;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.util.concurrent.Executors;

/**
 * Performs a single HTTP GET check. Redirects are NOT followed automatically:
 * a redirect could point at an internal address, so a 3xx is reported as-is.
 * Uses a virtual-thread executor so thousands of slow checks stay cheap.
 */
@Component
public class HttpChecker {

    private final HttpClient client;
    private final UrlGuard urlGuard;
    private final CheckProperties properties;

    public HttpChecker(UrlGuard urlGuard, CheckProperties properties) {
        this.urlGuard = urlGuard;
        this.properties = properties;
        this.client = HttpClient.newBuilder()
                .connectTimeout(properties.requestTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();
    }

    public CheckOutcome check(String url, int expectedStatus) {
        Instant start = Instant.now();
        long startNanos = System.nanoTime();
        try {
            URI uri = urlGuard.validate(url);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(properties.requestTimeout())
                    .header("User-Agent", "PulaWatch/0.1 (+https://github.com/pula-watch)")
                    .GET()
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            long ms = elapsedMs(startNanos);
            if (response.statusCode() == expectedStatus) {
                return CheckOutcome.ok(start, response.statusCode(), ms);
            }
            return CheckOutcome.unexpectedStatus(start, response.statusCode(), ms, expectedStatus);
        } catch (UnsafeUrlException e) {
            return CheckOutcome.failed(start, elapsedMs(startNanos), "Blocked: " + e.getMessage());
        } catch (HttpTimeoutException e) {
            return CheckOutcome.failed(start, elapsedMs(startNanos), "Timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CheckOutcome.failed(start, elapsedMs(startNanos), "Interrupted");
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return CheckOutcome.failed(start, elapsedMs(startNanos), message);
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
