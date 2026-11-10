package bw.pulawatch.check;

import bw.pulawatch.config.CheckProperties;
import bw.pulawatch.monitor.Monitor;
import bw.pulawatch.monitor.MonitorRepository;
import bw.pulawatch.monitor.MonitorStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Database side of the check cycle. Kept separate from the scheduler so each
 * method gets its own short transaction: locks are held only while claiming,
 * never while waiting on slow HTTP responses.
 */
@Component
public class CheckStore {

    private static final Logger log = LoggerFactory.getLogger(CheckStore.class);

    private final MonitorRepository monitorRepository;
    private final CheckResultRepository resultRepository;
    private final CheckProperties properties;

    public CheckStore(MonitorRepository monitorRepository,
                      CheckResultRepository resultRepository,
                      CheckProperties properties) {
        this.monitorRepository = monitorRepository;
        this.resultRepository = resultRepository;
        this.properties = properties;
    }

    /**
     * Locks due monitors, pushes their next_check_at forward, and returns
     * lightweight jobs. After commit, other replicas will not pick them up.
     */
    @Transactional
    public List<CheckJob> claimDueJobs() {
        List<Monitor> due = monitorRepository.lockDueMonitors(properties.batchSize());
        Instant now = Instant.now();
        for (Monitor m : due) {
            m.setNextCheckAt(now.plusSeconds(m.getIntervalSeconds()));
        }
        return due.stream()
                .map(m -> new CheckJob(m.getId(), m.getUrl(), m.getExpectedStatus()))
                .toList();
    }

    /**
     * Stores the result and applies the status state machine:
     * success -> UP; failure -> SUSPECT until the threshold, then DOWN.
     */
    @Transactional
    public void record(CheckJob job, CheckOutcome outcome) {
        resultRepository.save(new CheckResult(job.monitorId(), outcome));

        monitorRepository.findById(job.monitorId()).ifPresent(monitor -> {
            MonitorStatus before = monitor.getStatus();
            monitor.setLastCheckedAt(outcome.checkedAt());

            if (outcome.success()) {
                monitor.setConsecutiveFailures(0);
                monitor.setStatus(MonitorStatus.UP);
            } else {
                int failures = monitor.getConsecutiveFailures() + 1;
                monitor.setConsecutiveFailures(failures);
                monitor.setStatus(failures >= properties.failureThreshold()
                        ? MonitorStatus.DOWN
                        : MonitorStatus.SUSPECT);
            }

            if (before != monitor.getStatus()) {
                log.info("Monitor {} ({}) changed {} -> {}{}",
                        monitor.getId(), monitor.getName(), before, monitor.getStatus(),
                        outcome.error() == null ? "" : " : " + outcome.error());
            }
        });
    }

    public record CheckJob(Long monitorId, String url, int expectedStatus) {
    }
}
