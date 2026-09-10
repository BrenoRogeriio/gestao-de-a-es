package com.jeferson.gestaoacoes.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestObservabilityFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final int MAX_REQUEST_ID_LENGTH = 64;
    private static final int MAX_LOGGED_PATH_LENGTH = 512;
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,63}");
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestObservabilityFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = requestIdSeguro(request.getHeader(REQUEST_ID_HEADER));
        long inicio = System.nanoTime();
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put(REQUEST_ID_MDC_KEY, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;
            LOGGER.info("requestId={} method={} path={} status={} durationMs={}",
                    requestId, request.getMethod(), pathSeguro(request.getRequestURI()),
                    response.getStatus(), duracaoMs);
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    private String requestIdSeguro(String recebido) {
        if (recebido != null && recebido.length() <= MAX_REQUEST_ID_LENGTH
                && SAFE_REQUEST_ID.matcher(recebido).matches()) {
            return recebido;
        }
        return UUID.randomUUID().toString();
    }

    private String pathSeguro(String path) {
        String semQuebras = path == null ? "" : path.replace("\r", "").replace("\n", "");
        return semQuebras.length() <= MAX_LOGGED_PATH_LENGTH
                ? semQuebras
                : semQuebras.substring(0, MAX_LOGGED_PATH_LENGTH);
    }
}
