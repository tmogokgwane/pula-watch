package bw.pulawatch.security;

import bw.pulawatch.config.CheckProperties;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * SSRF protection. Users give us URLs and our server requests them, so we must
 * refuse anything that points at loopback, private networks, link-local
 * (including the cloud metadata endpoint 169.254.169.254) or other internal ranges.
 *
 * The check resolves DNS and validates EVERY resolved address. It is called when a
 * monitor is saved AND again right before each request, because DNS answers can
 * change after the monitor was created (DNS rebinding).
 */
@Component
public class UrlGuard {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private final CheckProperties properties;
    private final HostResolver resolver;

    public UrlGuard(CheckProperties properties, HostResolver resolver) {
        this.properties = properties;
        this.resolver = resolver;
    }

    /**
     * Validates the URL and returns the parsed URI.
     *
     * @throws UnsafeUrlException if the URL is malformed or targets a blocked address
     */
    public URI validate(String rawUrl) {
        URI uri;
        try {
            uri = new URI(rawUrl.trim());
        } catch (URISyntaxException e) {
            throw new UnsafeUrlException("URL is not valid");
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new UnsafeUrlException("Only http and https URLs are allowed");
        }
        if (uri.getUserInfo() != null) {
            throw new UnsafeUrlException("URLs with embedded credentials are not allowed");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new UnsafeUrlException("URL must include a host");
        }

        if (properties.allowPrivateTargets()) {
            return uri;
        }

        InetAddress[] addresses;
        try {
            addresses = resolver.resolve(host);
        } catch (UnknownHostException e) {
            throw new UnsafeUrlException("Host could not be resolved: " + host);
        }
        for (InetAddress address : addresses) {
            if (isBlocked(address)) {
                throw new UnsafeUrlException("URL resolves to a private or reserved address");
            }
        }
        return uri;
    }

    static boolean isBlocked(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] b = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = b[0] & 0xFF;
            int second = b[1] & 0xFF;
            if (first == 0) {
                return true;
            }
            if (first == 100 && second >= 64 && second <= 127) {
                return true;
            }
            if (first == 192 && second == 0 && (b[2] & 0xFF) == 0) {
                return true;
            }
            if (first == 198 && (second == 18 || second == 19)) {
                return true;
            }
            return first >= 240;
        }
        if (address instanceof Inet6Address) {
            int first = b[0] & 0xFF;
            if ((first & 0xFE) == 0xFC) {
                return true;
            }
            boolean ipv4Mapped = true;
            for (int i = 0; i < 10; i++) {
                if (b[i] != 0) {
                    ipv4Mapped = false;
                    break;
                }
            }
            if (ipv4Mapped && (b[10] & 0xFF) == 0xFF && (b[11] & 0xFF) == 0xFF) {
                try {
                    byte[] v4 = {b[12], b[13], b[14], b[15]};
                    return isBlocked(InetAddress.getByAddress(v4));
                } catch (UnknownHostException e) {
                    return true;
                }
            }
        }
        return false;
    }
}
