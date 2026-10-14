/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.doriangrelu.gatewayui.internal.realcall;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

class CallCorrelationFilterTest {

    private final CallRegistry registry = new CallRegistry(Duration.ofSeconds(5));

    private final CallCorrelationFilter filter = new CallCorrelationFilter(registry);

    @Test
    void removesTheMarkerAndRecordsTheRouteBeforeTheResponseIsSent() {
        final String id = registry.register().orElseThrow();
        final AtomicReference<ServerWebExchange> seen = new AtomicReference<>();

        filter.filter(exchange(id), routedTo("orders", seen)).block();

        assertThat(seen.get().getRequest().getHeaders().containsHeader(CallRegistry.HEADER)).isFalse();
        assertThat(registry.take(id)).contains(new CallRegistry.Routing("orders", "http://orders.test/orders/42"));
    }

    @Test
    void recordsThatNoRouteHandledTheRequest() {
        final String id = registry.register().orElseThrow();

        filter.filter(exchange(id), exchange -> exchange.getResponse().setComplete()).block();

        assertThat(registry.take(id)).contains(CallRegistry.Routing.NONE);
    }

    @Test
    void removesAForgedMarkerWithoutRecordingAnything() {
        final AtomicReference<ServerWebExchange> seen = new AtomicReference<>();

        filter.filter(exchange("forged"), routedTo("orders", seen)).block();

        assertThat(seen.get().getRequest().getHeaders().containsHeader(CallRegistry.HEADER)).isFalse();
        assertThat(registry.isKnown("forged")).isFalse();
    }

    @Test
    void leavesOtherRequestsUntouched() {
        final MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders"));
        final AtomicReference<ServerWebExchange> seen = new AtomicReference<>();

        filter.filter(exchange, routedTo("orders", seen)).block();

        assertThat(seen.get()).isSameAs(exchange);
    }

    private static MockServerWebExchange exchange(final String id) {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders").header(CallRegistry.HEADER, id));
    }

    /** Chaîne qui reproduit la Gateway : la route est posée sur l'échange, puis la réponse est envoyée. */
    private static WebFilterChain routedTo(final String routeId, final AtomicReference<ServerWebExchange> seen) {
        return exchange -> {
            seen.set(exchange);
            final Route route = Route.async().id(routeId).uri("http://orders.test").predicate(e -> true).build();
            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);
            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, URI.create("http://orders.test/orders/42"));
            return exchange.getResponse().setComplete().then(Mono.empty());
        };
    }
}
