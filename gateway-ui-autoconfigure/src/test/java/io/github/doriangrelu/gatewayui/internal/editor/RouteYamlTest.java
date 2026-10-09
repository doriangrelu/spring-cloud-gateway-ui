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
package io.github.doriangrelu.gatewayui.internal.editor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.webflux.autoconfigure.WebFluxProperties;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;

import static org.assertj.core.api.Assertions.assertThat;

class RouteYamlTest {

    private final RouteYaml yaml = new RouteYaml(new FactoryCatalog(
            List.<RoutePredicateFactory<?>>of(new PathRoutePredicateFactory(new WebFluxProperties())),
            List.<GatewayFilterFactory<?>>of(new StripPrefixGatewayFilterFactory(), new RewritePathGatewayFilterFactory())));

    @Test
    void writesRoutesInShortcutForm() {
        final EditableRoute route = new EditableRoute("users", "http://users:8080", 0,
                List.of(step("Path", "patterns", "/api/users/**")),
                List.of(step("StripPrefix", "parts", "1"),
                        new EditableStep("RewritePath", values("regexp", "/(?<s>.*)", "replacement", "/v2/$\\{s}"))));

        assertThat(yaml.toYaml(List.of(route), List.of("Généré par Gateway UI"))).isEqualTo("""
                # Généré par Gateway UI
                spring:
                  cloud:
                    gateway:
                      server:
                        webflux:
                          routes:
                            - id: users
                              uri: http://users:8080
                              predicates:
                                - Path=/api/users/**
                              filters:
                                - StripPrefix=1
                                - RewritePath=/(?<s>.*), /v2/$\\{s}
                """);
    }

    @Test
    void writesOrderOnlyWhenNotZeroAndKeepsPlaceholders() {
        final EditableRoute fallback = new EditableRoute("fallback", "${FALLBACK_URL}", 100, List.of(), List.of());
        final String document = yaml.toYaml(List.of(fallback), List.of());

        assertThat(document).contains("uri: ${FALLBACK_URL}").contains("order: 100");
        assertThat(yaml.toYaml(List.of(new EditableRoute("a", "http://a", 0, List.of(), List.of())), List.of())).doesNotContain("order:");
    }

    @Test
    void dropsTrailingEmptyArguments() {
        final EditableStep step = new EditableStep("Path", values("patterns", "/a/**", "matchTrailingSlash", ""));

        assertThat(RouteYaml.shortcut(step, new FactoryDescriptor("Path", List.of("patterns", "matchTrailingSlash"), "GATHER_LIST")))
                .isEqualTo("Path=/a/**");
    }

    @Test
    void usesExpandedFormForArgumentsUnknownToTheShortcut() {
        final EditableRoute route = new EditableRoute("limited", "http://a", 0, List.of(),
                List.of(step("RequestRateLimiter", "redis-rate-limiter.replenishRate", "10")));

        assertThat(yaml.toYaml(List.of(route), List.of())).contains("""
                                - name: RequestRateLimiter
                                  args:
                                    redis-rate-limiter.replenishRate: 10
                """);
    }

    @Test
    void quotesValuesThatYamlWouldInterpret() {
        assertThat(RouteYaml.value("Header=X-Token, .+")).isEqualTo("Header=X-Token, .+");
        assertThat(RouteYaml.value("a: b")).isEqualTo("'a: b'");
        assertThat(RouteYaml.value("*.example.com")).isEqualTo("'*.example.com'");
        assertThat(RouteYaml.value("it's")).isEqualTo("it's");
        assertThat(RouteYaml.value("")).isEqualTo("''");
        assertThat(RouteYaml.value("'quoted'")).isEqualTo("'''quoted'''");
    }

    @Test
    void writesAnEmptyListWhenThereIsNoRoute() {
        assertThat(yaml.toYaml(List.of(), List.of())).endsWith("routes:\n            []\n");
    }

    private static EditableStep step(final String name, final String field, final String value) {
        return new EditableStep(name, values(field, value));
    }

    private static Map<String, String> values(final String... pairs) {
        final Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            values.put(pairs[i], pairs[i + 1]);
        }
        return values;
    }
}
