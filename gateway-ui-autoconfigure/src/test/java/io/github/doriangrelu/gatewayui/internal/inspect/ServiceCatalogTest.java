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
package io.github.doriangrelu.gatewayui.internal.inspect;

import java.net.URI;
import java.util.List;
import java.util.Map;

import io.github.doriangrelu.gatewayui.autoconfigure.GatewayUiProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceCatalogTest {

    @Test
    void baseUrlMakesDefaultPortExplicit() {
        assertThat(ServiceCatalog.baseUrl(URI.create("http://Orders.svc/api"))).isEqualTo("http://orders.svc:80");
        assertThat(ServiceCatalog.baseUrl(URI.create("https://orders.svc:8443"))).isEqualTo("https://orders.svc:8443");
        assertThat(ServiceCatalog.baseUrl(URI.create("lb://orders"))).isEqualTo("lb://orders");
    }

    @Test
    void groupsDeclaredServicesFirstThenDiscoveredOnes() {
        final GatewayUiProperties.Service orders = new GatewayUiProperties.Service(
                URI.create("http://orders.shop.svc:8080"), "Commandes", null);
        final ServiceCatalog catalog = catalog(true, Map.of("orders", orders));

        final List<ServiceView> services = catalog.group(List.of(
                route("orders-v1", "http://orders.shop.svc:8080"),
                route("users", "http://users.shop.svc:8080"),
                route("users-v2", "http://users.shop.svc:8080/v2"),
                route("local", "forward:/fallback")));

        assertThat(services).extracting(ServiceView::name).containsExactly("orders", "users");
        assertThat(services.getFirst().displayName()).isEqualTo("Commandes");
        assertThat(services.get(1).routes()).extracting(RouteView::id).containsExactly("users", "users-v2");
    }

    @Test
    void discoveryCanBeDisabled() {
        final List<ServiceView> services = catalog(false, Map.of()).group(List.of(route("users", "http://users:8080")));

        assertThat(services).isEmpty();
    }

    private static ServiceCatalog catalog(final boolean discover, final Map<String, GatewayUiProperties.Service> services) {
        return new ServiceCatalog(null, new GatewayUiProperties(true, null, discover, services));
    }

    private static RouteView route(final String id, final String uri) {
        return new RouteView(id, URI.create(uri), 0, "", List.of(), List.of(), List.of(), Map.of(), true);
    }
}
