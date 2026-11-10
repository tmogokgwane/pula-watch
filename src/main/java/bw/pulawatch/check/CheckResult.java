package bw.pulawatch.check;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "check_results")
public class CheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monitor_id", nullable = false)
    private Long monitorId;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(length = 500)
    private String error;

    protected CheckResult() {
    }

    public CheckResult(Long monitorId, CheckOutcome outcome) {
        this.monitorId = monitorId;
        this.checkedAt = outcome.checkedAt();
        this.success = outcome.success();
        this.statusCode = outcome.statusCode();
        this.responseTimeMs = outcome.responseTimeMs();
        this.error = outcome.error() == null ? null
                : outcome.error().substring(0, Math.min(500, outcome.error().length()));
    }

    public Long getId() {
        return id;
    }

    public Long getMonitorId() {
        return monitorId;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public String getError() {
        return error;
    }
}
