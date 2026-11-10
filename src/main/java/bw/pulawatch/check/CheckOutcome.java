package bw.pulawatch.check;

import java.time.Instant;

/**
 * The result of one HTTP check, before it is stored.
 */
public record CheckOutcome(
        Instant checkedAt,
        boolean success,
        Integer statusCode,
        Long responseTimeMs,
        String error
) {
    public static CheckOutcome ok(Instant at, int statusCode, long ms) {
        return new CheckOutcome(at, true, statusCode, ms, null);
    }

    public static CheckOutcome unexpectedStatus(Instant at, int statusCode, long ms, int expected) {
        return new CheckOutcome(at, false, statusCode, ms,
                "Expected HTTP " + expected + " but got " + statusCode);
    }

    public static CheckOutcome failed(Instant at, long ms, String error) {
        return new CheckOutcome(at, false, null, ms, error);
    }
}
