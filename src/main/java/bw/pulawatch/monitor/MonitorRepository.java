package bw.pulawatch.monitor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MonitorRepository extends JpaRepository<Monitor, Long> {

    /**
     * Claims due monitors with row locks. SKIP LOCKED lets several app replicas
     * run the scheduler at the same time without checking the same monitor twice.
     * Must be called inside a transaction.
     */
    @Query(value = """
            SELECT * FROM monitors
            WHERE enabled = TRUE AND next_check_at <= now()
            ORDER BY next_check_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Monitor> lockDueMonitors(@Param("limit") int limit);
}
