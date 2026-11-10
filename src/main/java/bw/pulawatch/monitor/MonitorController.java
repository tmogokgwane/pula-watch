package bw.pulawatch.monitor;

import bw.pulawatch.check.CheckResultResponse;
import bw.pulawatch.check.CheckResultService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monitors")
public class MonitorController {

    private final MonitorService monitorService;
    private final CheckResultService checkResultService;

    public MonitorController(MonitorService monitorService, CheckResultService checkResultService) {
        this.monitorService = monitorService;
        this.checkResultService = checkResultService;
    }

    @GetMapping
    public List<MonitorResponse> list() {
        return monitorService.list();
    }

    @GetMapping("/{id}")
    public MonitorResponse get(@PathVariable Long id) {
        return monitorService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MonitorResponse create(@Valid @RequestBody MonitorRequest request) {
        return monitorService.create(request);
    }

    @PutMapping("/{id}")
    public MonitorResponse update(@PathVariable Long id, @Valid @RequestBody MonitorRequest request) {
        return monitorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        monitorService.delete(id);
    }

    @GetMapping("/{id}/results")
    public List<CheckResultResponse> recentResults(@PathVariable Long id) {
        monitorService.get(id);
        return checkResultService.recent(id);
    }
}
