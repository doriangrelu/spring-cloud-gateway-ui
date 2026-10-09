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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webflux.autoconfigure.WebFluxProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SetRequestUriGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.XForwardedRemoteAddrRoutePredicateFactory;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogTest {

    private static final String REFERENCE = "https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/";

    private final Catalog catalog = new Catalog(
            List.<RoutePredicateFactory<?>>of(new PathRoutePredicateFactory(new WebFluxProperties()),
                    new XForwardedRemoteAddrRoutePredicateFactory()),
            List.<GatewayFilterFactory<?>>of(new StripPrefixGatewayFilterFactory(), new SetRequestUriGatewayFilterFactory(),
                    new MaskGatewayFilterFactory()));

    @Test
    void categorisesGatewayFactoriesAndLinksToTheirDocumentation() {
        assertThat(entry("Path")).satisfies(path -> {
            assertThat(path.kind()).isEqualTo(CatalogEntry.PREDICATE);
            assertThat(path.category()).isEqualTo("path-host");
            assertThat(path.fields()).containsExactly("patterns", "matchTrailingSlash");
            assertThat(path.documentation()).isEqualTo(REFERENCE + "request-predicates-factories.html#path-route-predicate-factory");
            assertThat(path.custom()).isFalse();
        });
        assertThat(entry("StripPrefix").documentation()).isEqualTo(REFERENCE + "gatewayfilter-factories/stripprefix-factory.html");
        assertThat(entry("StripPrefix").category()).isEqualTo("path");
    }

    @Test
    void usesTheDocumentationAnchorsThatDoNotFollowTheNamingRule() {
        assertThat(entry("XForwardedRemoteAddr").documentation())
                .endsWith("request-predicates-factories.html#xforwarded-remote-addr-route-predicate-factory");
    }

    @Test
    void linksFactoriesWithoutDedicatedSectionToTheGeneralPage() {
        assertThat(entry("SetRequestUri").documentation()).isEqualTo(REFERENCE + "gatewayfilter-factories.html");
        assertThat(entry("SetRequestUri").category()).isEqualTo("path");
    }

    @Test
    void flagsCustomFactoriesWithoutDocumentationLink() {
        assertThat(entry("Mask")).satisfies(mask -> {
            assertThat(mask.custom()).isTrue();
            assertThat(mask.category()).isEqualTo("custom");
            assertThat(mask.documentation()).isNull();
            assertThat(mask.messageKey()).isEqualTo("catalog.filter.Mask");
        });
    }

    @Test
    void listsCategoriesInDisplayOrderWithOthersAndCustomLast() {
        assertThat(Catalog.categories(CatalogEntry.PREDICATE)).startsWith("path-host").endsWith("other", "custom");
        assertThat(Catalog.categories(CatalogEntry.FILTER)).startsWith("path", "request-headers");
    }

    private CatalogEntry entry(final String name) {
        return catalog.entries().stream().filter(entry -> entry.name().equals(name)).findFirst().orElseThrow();
    }

    /** Filtre maison d'une application : hors du paquet de Spring Cloud Gateway. */
    static class MaskGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

        @Override
        public GatewayFilter apply(final Object config) {
            return (exchange, chain) -> chain.filter(exchange);
        }
    }
}
