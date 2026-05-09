package com.graduationdesign.newsrecommendation.common;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import org.springframework.util.StringUtils;

public final class RemoteUrlValidator {

    private RemoteUrlValidator() {
    }

    public static URI validatePublicHttpUrl(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            throw new IllegalArgumentException("Source URL cannot be blank");
        }

        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Source URL format is invalid");
        }

        String scheme = uri.getScheme();
        if (!StringUtils.hasText(scheme) || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("Source URL must use http or https");
        }

        String host = uri.getHost();
        if (!StringUtils.hasText(host)) {
            throw new IllegalArgumentException("Source URL host is invalid");
        }
        if ("localhost".equalsIgnoreCase(host)) {
            throw new IllegalArgumentException("Source URL cannot target localhost");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isPrivateOrLocalAddress(address)) {
                    throw new IllegalArgumentException("Source URL cannot target local or private network addresses");
                }
            }
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("Source URL host cannot be resolved");
        }

        return uri;
    }

    private static boolean isPrivateOrLocalAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
            || address.isSiteLocalAddress()) {
            return true;
        }

        String hostAddress = address.getHostAddress();
        return hostAddress.startsWith("127.")
            || hostAddress.startsWith("169.254.")
            || hostAddress.startsWith("10.")
            || hostAddress.startsWith("192.168.")
            || hostAddress.startsWith("172.16.")
            || hostAddress.startsWith("172.17.")
            || hostAddress.startsWith("172.18.")
            || hostAddress.startsWith("172.19.")
            || hostAddress.startsWith("172.20.")
            || hostAddress.startsWith("172.21.")
            || hostAddress.startsWith("172.22.")
            || hostAddress.startsWith("172.23.")
            || hostAddress.startsWith("172.24.")
            || hostAddress.startsWith("172.25.")
            || hostAddress.startsWith("172.26.")
            || hostAddress.startsWith("172.27.")
            || hostAddress.startsWith("172.28.")
            || hostAddress.startsWith("172.29.")
            || hostAddress.startsWith("172.30.")
            || hostAddress.startsWith("172.31.")
            || "::1".equals(hostAddress)
            || "0:0:0:0:0:0:0:1".equals(hostAddress);
    }
}
