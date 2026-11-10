package bw.pulawatch.check;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Every few seconds: claim due monitors, run their checks in parallel on
 * virtual threads, and record the outcomes.
 */
@Component
@ConditionalOnProperty(name = "pulawatch.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class CheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(CheckScheduler.class);

    private final CheckStore store;
    private final HttpChecker checker;

    public CheckScheduler(CheckStore store, HttpChecker checker) {
        this.store = store;
        this.checker = checker;
    }

    @Scheduled(fixedDelayString = "${pulawatch.scheduler.tick:PT5S}")
    public void tick() {
        List<CheckStore.CheckJob> jobs = store.claimDueJobs();
        if (jobs.isEmpty()) {
            return;
        }
        log.debug("Running {} checks", jobs.size());

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (CheckStore.CheckJob job : jobs) {
                futures.add(executor.submit(() -> runOne(job)));
            }
            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (Exception e) {
                    log.warn("Check task failed", e);
                }
            }
        }
    }

    private void runOne(CheckStore.CheckJob job) {
        CheckOutcome outcome = checker.check(job.url(), job.expectedStatus());
        store.record(job, outcome);
    }
}
