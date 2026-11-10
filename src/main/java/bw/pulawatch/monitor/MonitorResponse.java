package bw.pulawatch.monitor;

import java.time.Instant;

public record MonitorResponse(
        Long id,
        String name,
        String url,
        int intervalSeconds,
        int expectedStatus,
        boolean enabled,
        MonitorStatus status,
        Instant lastCheckedAt,
        Instant createdAt
) {
    public static MonitorResponse from(Monitor m) {
        return new MonitorResponse(
                m.getId(),
                m.getName(),
                m.getUrl(),
                m.getIntervalSeconds(),
                m.getExpectedStatus(),
                m.isEnabled(),
                m.getStatus(),
                m.getLastCheckedAt(),
                m.getCreatedAt()
        );
    }
}
