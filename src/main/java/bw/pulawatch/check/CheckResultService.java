package bw.pulawatch.check;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CheckResultService {

    private final CheckResultRepository repository;

    public CheckResultService(CheckResultRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CheckResultResponse> recent(Long monitorId) {
        return repository.findTop50ByMonitorIdOrderByCheckedAtDesc(monitorId).stream()
                .map(CheckResultResponse::from)
                .toList();
    }
}
