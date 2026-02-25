package com.serdarahmanov.music_app_backend.utility.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClientIpResolver {

    public String resolve(HttpServletRequest request) {
        String fromXForwardedFor = firstHeaderValue(request.getHeader("X-Forwarded-For"));
        if (StringUtils.hasText(fromXForwardedFor)) {
            return fromXForwardedFor;
        }

        String fromXRealIp = firstHeaderValue(request.getHeader("X-Real-IP"));
        if (StringUtils.hasText(fromXRealIp)) {
            return fromXRealIp;
        }

        String fromForwarded = forwardedForValue(request.getHeader("Forwarded"));
        if (StringUtils.hasText(fromForwarded)) {
            return fromForwarded;
        }

        return request.getRemoteAddr();
    }

    private static String firstHeaderValue(String headerValue) {
        if (!StringUtils.hasText(headerValue)) {
            return null;
        }
        String candidate = headerValue.split(",")[0].trim();
        return candidate.isEmpty() ? null : candidate;
    }

    private static String forwardedForValue(String forwardedHeader) {
        if (!StringUtils.hasText(forwardedHeader)) {
            return null;
        }
        String[] segments = forwardedHeader.split(";");
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (!trimmed.regionMatches(true, 0, "for=", 0, 4)) {
                continue;
            }
            String value = trimmed.substring(4).trim();
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                value = value.substring(1, value.length() - 1);
            }
            if (value.startsWith("[")) {
                int closing = value.indexOf(']');
                if (closing > 1) {
                    return value.substring(1, closing);
                }
            }
            int colonIndex = value.indexOf(':');
            return colonIndex > 0 ? value.substring(0, colonIndex) : value;
        }
        return null;
    }
}
