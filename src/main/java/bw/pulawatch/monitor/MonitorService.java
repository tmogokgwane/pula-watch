package bw.pulawatch.monitor;

import bw.pulawatch.security.UrlGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class MonitorService {

    private final MonitorRepository repository;
    private final UrlGuard urlGuard;

    public MonitorService(MonitorRepository repository, UrlGuard urlGuard) {
        this.repository = repository;
        this.urlGuard = urlGuard;
    }

    @Transactional(readOnly = true)
    public List<MonitorResponse> list() {
        return repository.findAll().stream().map(MonitorResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MonitorResponse get(Long id) {
        return MonitorResponse.from(find(id));
    }

    @Transactional
    public MonitorResponse create(MonitorRequest request) {
        urlGuard.validate(request.url());
        Monitor monitor = new Monitor(
                request.name(),
                request.url(),
                request.intervalOrDefault(),
                request.expectedStatusOrDefault()
        );
        monitor.setEnabled(request.enabledOrDefault());
        monitor.setNextCheckAt(Instant.now());
        return MonitorResponse.from(repository.save(monitor));
    }

    @Transactional
    public MonitorResponse update(Long id, MonitorRequest request) {
        urlGuard.validate(request.url());
        Monitor monitor = find(id);
        monitor.setName(request.name());
        monitor.setUrl(request.url());
        monitor.setIntervalSeconds(request.intervalOrDefault());
        monitor.setExpectedStatus(request.expectedStatusOrDefault());
        monitor.setEnabled(request.enabledOrDefault());
        return MonitorResponse.from(monitor);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private Monitor find(Long id) {
        return repository.findById(id).orElseThrow(() -> new MonitorNotFoundException(id));
    }
}
