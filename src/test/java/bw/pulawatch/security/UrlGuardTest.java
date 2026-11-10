package bw.pulawatch.security;

import bw.pulawatch.config.CheckProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlGuardTest {

    private static final CheckProperties STRICT =
            new CheckProperties(50, Duration.ofSeconds(5), 3, false);

    private static UrlGuard guardResolvingTo(String ip) {
        HostResolver fake = new HostResolver() {
            @Override
            public InetAddress[] resolve(String host) throws UnknownHostException {
                return new InetAddress[]{InetAddress.getByName(ip)};
            }
        };
        return new UrlGuard(STRICT, fake);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "127.0.0.1", "10.0.0.5", "172.16.0.1", "192.168.1.10",
            "169.254.169.254", "0.0.0.0", "100.64.0.1", "::1", "fd00::1",
            "::ffff:127.0.0.1"
    })
    void blocksPrivateAndReservedAddresses(String ip) {
        UrlGuard guard = guardResolvingTo(ip);
        assertThatThrownBy(() -> guard.validate("https://example.test/"))
                .isInstanceOf(UnsafeUrlException.class);
    }

    @Test
    void allowsPublicAddress() {
        UrlGuard guard = guardResolvingTo("93.184.215.14");
        assertThat(guard.validate("https://example.com/health").getHost()).isEqualTo("example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://example.com", "file:///etc/passwd", "gopher://x", "not a url", "https://user:pw@example.com"})
    void rejectsBadSchemesAndCredentials(String url) {
        UrlGuard guard = guardResolvingTo("93.184.215.14");
        assertThatThrownBy(() -> guard.validate(url)).isInstanceOf(UnsafeUrlException.class);
    }

    @Test
    void devModeAllowsLocalhost() {
        CheckProperties dev = new CheckProperties(50, Duration.ofSeconds(5), 3, true);
        UrlGuard guard = new UrlGuard(dev, new HostResolver());
        assertThat(guard.validate("http://localhost:8080").getHost()).isEqualTo("localhost");
    }
}
