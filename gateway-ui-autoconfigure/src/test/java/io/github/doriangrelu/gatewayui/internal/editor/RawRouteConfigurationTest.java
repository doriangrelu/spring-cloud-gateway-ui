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

import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class RawRouteConfigurationTest {

    private static final String ROUTES = "spring.cloud.gateway.server.webflux.routes";

    private final RawRouteConfiguration raw = new RawRouteConfiguration(List.of(new MapConfigurationPropertySource(properties())),
            value -> value.replace("${ORDERS_ID:orders}", "orders"));

    @Test
    void findsTheRouteByItsResolvedIdentifier() {
        assertThat(raw.routePrefix("users")).contains(ROUTES + "[0]");
        assertThat(raw.routePrefix("orders")).contains(ROUTES + "[1]");
        assertThat(raw.routePrefix("unknown")).isEmpty();
    }

    @Test
    void readsValuesAsWritten() {
        assertThat(raw.raw(ROUTES + "[0].uri")).contains("${USERS_URL}");
        assertThat(raw.raw(ROUTES + "[0].order")).isEmpty();
    }

    @Test
    void readsShortcutArguments() {
        assertThat(raw.args(ROUTES + "[0].filters[0]", false, Map.of()))
                .containsExactly(Map.entry("_genkey_0", "Authorization"), Map.entry("_genkey_1", "Bearer ${TOKEN}"));
    }

    @Test
    void readsExpandedArgumentsByName() {
        final Map<String, String> resolved = new LinkedHashMap<>();
        resolved.put("name", "X-Env");
        resolved.put("value", "prod");

        assertThat(raw.args(ROUTES + "[0].filters[1]", false, resolved))
                .containsExactly(Map.entry("name", "X-Env"), Map.entry("value", "${ENV_NAME:dev}"));
    }

    private static Map<String, String> properties() {
        final Map<String, String> properties = new LinkedHashMap<>();
        properties.put(ROUTES + "[0].id", "users");
        properties.put(ROUTES + "[0].uri", "${USERS_URL}");
        properties.put(ROUTES + "[0].filters[0]", "AddRequestHeader=Authorization, Bearer ${TOKEN}");
        properties.put(ROUTES + "[0].filters[1].name", "SetRequestHeader");
        properties.put(ROUTES + "[0].filters[1].args.name", "X-Env");
        properties.put(ROUTES + "[0].filters[1].args.value", "${ENV_NAME:dev}");
        properties.put(ROUTES + "[1].id", "${ORDERS_ID:orders}");
        properties.put(ROUTES + "[1].uri", "http://orders");
        return properties;
    }
}
