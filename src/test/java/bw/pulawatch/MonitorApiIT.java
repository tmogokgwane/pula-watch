package bw.pulawatch;

import bw.pulawatch.check.CheckOutcome;
import bw.pulawatch.check.CheckStore;
import bw.pulawatch.monitor.Monitor;
import bw.pulawatch.monitor.MonitorRepository;
import bw.pulawatch.monitor.MonitorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "pulawatch.scheduler.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
class MonitorApiIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvc mvc;

    @Autowired
    MonitorRepository monitors;

    @Autowired
    CheckStore checkStore;

    @BeforeEach
    void clean() {
        monitors.deleteAll();
    }

    @Test
    void rejectsMetadataEndpoint() throws Exception {
        mvc.perform(post("/api/monitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"evil","url":"http://169.254.169.254/latest/meta-data"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidInterval() throws Exception {
        mvc.perform(post("/api/monitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"too fast","url":"https://example.com","intervalSeconds":5}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns404ForUnknownMonitor() throws Exception {
        mvc.perform(get("/api/monitors/999999")).andExpect(status().isNotFound());
    }

    @Test
    void claimsDueMonitorsOnceAndAppliesStateMachine() {
        Monitor m = new Monitor("site", "https://example.com", 60, 200);
        m.setNextCheckAt(Instant.now().minusSeconds(1));
        m = monitors.save(m);

        List<CheckStore.CheckJob> first = checkStore.claimDueJobs();
        List<CheckStore.CheckJob> second = checkStore.claimDueJobs();
        assertThat(first).hasSize(1);
        assertThat(second).isEmpty();

        CheckStore.CheckJob job = first.getFirst();
        for (int i = 0; i < 3; i++) {
            checkStore.record(job, CheckOutcome.failed(Instant.now(), 10, "Connection refused"));
        }
        assertThat(monitors.findById(m.getId()).orElseThrow().getStatus()).isEqualTo(MonitorStatus.DOWN);

        checkStore.record(job, CheckOutcome.ok(Instant.now(), 200, 42));
        assertThat(monitors.findById(m.getId()).orElseThrow().getStatus()).isEqualTo(MonitorStatus.UP);
    }

    @Test
    void listsMonitors() throws Exception {
        monitors.save(new Monitor("a", "https://example.com", 60, 200));
        mvc.perform(get("/api/monitors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("a"));
    }
}
