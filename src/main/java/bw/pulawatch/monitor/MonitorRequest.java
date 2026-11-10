package bw.pulawatch.monitor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MonitorRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 2048) String url,
        @Min(30) @Max(3600) Integer intervalSeconds,
        @Min(100) @Max(599) Integer expectedStatus,
        Boolean enabled
) {
    public int intervalOrDefault() {
        return intervalSeconds == null ? 60 : intervalSeconds;
    }

    public int expectedStatusOrDefault() {
        return expectedStatus == null ? 200 : expectedStatus;
    }

    public boolean enabledOrDefault() {
        return enabled == null || enabled;
    }
}
