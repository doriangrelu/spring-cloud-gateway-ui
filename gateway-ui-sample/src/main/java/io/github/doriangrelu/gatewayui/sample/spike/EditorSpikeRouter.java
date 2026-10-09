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
package io.github.doriangrelu.gatewayui.sample.spike;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.support.ShortcutConfigurable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * SPIKE #28, branche jetable : prototypes de l'éditeur graphique, sous {@code /gateway-ui/editor-proto}.
 *
 * <p>Fournit la page, ses scripts, les définitions des routes et la description des fabriques de prédicats et de filtres.
 */
@Configuration(proxyBeanMethods = false)
public class EditorSpikeRouter {

    private static final String BASE = "/gateway-ui/editor-proto";

    /** Même politique de contenu que l'UI : les prototypes doivent la respecter. */
    private static final String CSP = "default-src 'none'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
            + "connect-src 'self'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'";

    /**
     * Routes HTTP du spike.
     *
     * @param definitions définitions des routes déclaratives
     * @param predicates fabriques de prédicats de la Gateway
     * @param filters fabriques de filtres de la Gateway
     * @return la fonction de routage
     */
    @Bean
    RouterFunction<ServerResponse> editorSpike(final RouteDefinitionLocator definitions,
            final List<RoutePredicateFactory<?>> predicates, final List<GatewayFilterFactory<?>> filters) {
        return RouterFunctions.route()
                .GET(BASE, request -> ServerResponse.ok().contentType(MediaType.TEXT_HTML)
                        .body(BodyInserters.fromResource(new ClassPathResource("spike/editor.html"))))
                .GET(BASE + "/api/routes", request -> ServerResponse.ok()
                        .body(definitions.getRouteDefinitions().map(EditorSpikeRouter::route), Map.class))
                .GET(BASE + "/api/factories", request -> ServerResponse.ok()
                        .bodyValue(Map.of("predicates", describe(predicates), "filters", describe(filters))))
                .add(RouterFunctions.resources(BASE + "/static/**", new ClassPathResource("spike/")))
                .add(RouterFunctions.resources(BASE + "/drawflow/**",
                        new ClassPathResource("META-INF/resources/webjars/drawflow/0.0.60/dist/")))
                .build()
                .filter(securityHeaders());
    }

    private static Map<String, Object> route(final RouteDefinition definition) {
        return Map.of("id", definition.getId(), "uri", String.valueOf(definition.getUri()), "order", definition.getOrder(),
                "predicates", definition.getPredicates().stream().map(p -> Map.of("name", p.getName(), "args", p.getArgs())).toList(),
                "filters", definition.getFilters().stream().map(f -> Map.of("name", f.getName(), "args", f.getArgs())).toList());
    }

    private static List<Map<String, Object>> describe(final List<? extends ShortcutConfigurable> factories) {
        return factories.stream()
                .map(factory -> Map.<String, Object>of("name", name(factory), "fields", factory.shortcutFieldOrder(),
                        "shortcutType", factory.shortcutType().name()))
                .sorted((left, right) -> String.valueOf(left.get("name")).compareTo(String.valueOf(right.get("name"))))
                .toList();
    }

    private static String name(final ShortcutConfigurable factory) {
        if (factory instanceof final RoutePredicateFactory<?> predicate) {
            return predicate.name();
        }
        return ((GatewayFilterFactory<?>) factory).name();
    }

    private static HandlerFilterFunction<ServerResponse, ServerResponse> securityHeaders() {
        return (request, next) -> {
            request.exchange().getResponse().getHeaders().set("Content-Security-Policy", CSP);
            return next.handle(request);
        };
    }
}
