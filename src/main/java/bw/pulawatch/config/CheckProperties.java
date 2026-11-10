package bw.pulawatch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Tunables for the check engine, bound from the "pulawatch.checks" prefix.
 *
 * @param batchSize          how many due monitors one scheduler tick claims
 * @param requestTimeout     per-request HTTP timeout
 * @param failureThreshold   consecutive failures before a monitor is marked DOWN
 * @param allowPrivateTargets set true ONLY in local dev to allow checking localhost/private IPs
 */
@ConfigurationProperties(prefix = "pulawatch.checks")
public record CheckProperties(
        int batchSize,
        Duration requestTimeout,
        int failureThreshold,
        boolean allowPrivateTargets
) {
    public CheckProperties {
        if (batchSize <= 0) {
            batchSize = 50;
        }
        if (requestTimeout == null) {
            requestTimeout = Duration.ofSeconds(10);
        }
        if (failureThreshold <= 0) {
            failureThreshold = 3;
        }
    }
}
