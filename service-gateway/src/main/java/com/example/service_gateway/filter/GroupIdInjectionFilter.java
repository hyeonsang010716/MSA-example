package com.example.service_gateway.filter;

import io.micrometer.observation.Observation;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.handler.TracingObservationHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

@Slf4j
@Component
public class GroupIdInjectionFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return Mono.deferContextual(contextView -> {
            String traceId = extractTraceId(contextView);
            if (traceId != null) {
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header("X-Group-Id", traceId)
                        .build();

                log.info("Injected X-Group-Id: {}", traceId);
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }

            return chain.filter(exchange);
        });
    }

    private String extractTraceId(ContextView contextView) {
        Observation observation = contextView.getOrDefault(
                ObservationThreadLocalAccessor.KEY, null);
        if (observation == null) return null;

        TracingObservationHandler.TracingContext tracingContext =
                observation.getContext().get(TracingObservationHandler.TracingContext.class);
        if (tracingContext == null) return null;

        return tracingContext.getSpan().context().traceId();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
