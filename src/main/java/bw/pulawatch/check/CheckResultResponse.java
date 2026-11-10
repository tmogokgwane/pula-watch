package bw.pulawatch.check;

import java.time.Instant;

public record CheckResultResponse(
        Instant checkedAt,
        boolean success,
        Integer statusCode,
        Long responseTimeMs,
        String error
) {
    public static CheckResultResponse from(CheckResult r) {
        return new CheckResultResponse(
                r.getCheckedAt(),
                r.isSuccess(),
                r.getStatusCode(),
                r.getResponseTimeMs(),
                r.getError()
        );
    }
}
