package bw.pulawatch.security;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Thin wrapper around DNS resolution so tests can substitute fake answers.
 */
@Component
public class HostResolver {

    public InetAddress[] resolve(String host) throws UnknownHostException {
        return InetAddress.getAllByName(host);
    }
}
