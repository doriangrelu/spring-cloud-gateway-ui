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

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Relève la route qui a réellement traité un appel réel du testeur (ADR 0015).
 *
 * <p>{@code RoutePredicateHandlerMapping} dépose la route retenue dans l'attribut d'échange
 * {@link ServerWebExchangeUtils#GATEWAY_ROUTE_ATTR}, invisible pour le client. Ce filtre le lit juste avant l'envoi de la
 * réponse ({@code beforeCommit}) : la route est donc enregistrée avant que l'UI ne reçoive la réponse.
 *
 * <p>C'est un {@link WebFilter} et non un {@code GlobalFilter} : il n'entre pas dans la chaîne de filtres des routes,
 * que l'UI affiche et simule. Il n'agit que sur les requêtes qui portent l'en-tête {@value CallRegistry#HEADER}, qu'il
 * retire avant la sélection de route : ni la Gateway ni le service ne le voient.
 */
public class CallCorrelationFilter implements WebFilter, Ordered {

    private final CallRegistry registry;

    /**
     * Crée le filtre.
     *
     * @param registry registre des appels réels en cours
     */
    public CallCorrelationFilter(final CallRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        final String id = exchange.getRequest().getHeaders().getFirst(CallRegistry.HEADER);
        if (id == null) {
            return chain.filter(exchange);
        }
        final ServerWebExchange unmarked = exchange.mutate()
                .request(request -> request.headers(headers -> headers.remove(CallRegistry.HEADER)))
                .build();
        if (registry.isKnown(id)) {
            unmarked.getResponse().beforeCommit(() -> Mono.fromRunnable(() -> record(id, unmarked)));
        }
        return chain.filter(unmarked);
    }

    /** Avant toute sécurité de l'application hôte : le marqueur ne doit être vu par aucun autre filtre. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /** {@code GATEWAY_REQUEST_URL_ATTR} porte l'URL finale, une fois appliqués les filtres et l'équilibrage de charge. */
    private void record(final String id, final ServerWebExchange exchange) {
        final Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        final URI target = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        registry.record(id, route == null ? CallRegistry.Routing.NONE
                : new CallRegistry.Routing(route.getId(), target == null ? null : target.toString()));
    }
}
