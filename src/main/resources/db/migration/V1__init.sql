CREATE TABLE monitors (
    id                   BIGSERIAL PRIMARY KEY,
    name                 VARCHAR(100)  NOT NULL,
    url                  VARCHAR(2048) NOT NULL,
    interval_seconds     INTEGER       NOT NULL CHECK (interval_seconds BETWEEN 30 AND 3600),
    expected_status      INTEGER       NOT NULL DEFAULT 200,
    enabled              BOOLEAN       NOT NULL DEFAULT TRUE,
    status               VARCHAR(16)   NOT NULL DEFAULT 'UNKNOWN',
    consecutive_failures INTEGER       NOT NULL DEFAULT 0,
    next_check_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    last_checked_at      TIMESTAMPTZ,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_monitors_due ON monitors (next_check_at) WHERE enabled = TRUE;

CREATE TABLE check_results (
    id               BIGSERIAL PRIMARY KEY,
    monitor_id       BIGINT      NOT NULL REFERENCES monitors (id) ON DELETE CASCADE,
    checked_at       TIMESTAMPTZ NOT NULL,
    success          BOOLEAN     NOT NULL,
    status_code      INTEGER,
    response_time_ms BIGINT,
    error            VARCHAR(500)
);

CREATE INDEX idx_check_results_monitor_time ON check_results (monitor_id, checked_at DESC);
