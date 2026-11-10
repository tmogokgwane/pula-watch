package bw.pulawatch.monitor;

public class MonitorNotFoundException extends RuntimeException {

    public MonitorNotFoundException(Long id) {
        super("Monitor " + id + " not found");
    }
}
