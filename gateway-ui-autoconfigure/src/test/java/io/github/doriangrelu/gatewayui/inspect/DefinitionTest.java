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
package io.github.doriangrelu.gatewayui.inspect;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.gateway.filter.FilterDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class DefinitionTest {

    @Test
    void readsShortcutArgumentsByPosition() {
        final Definition definition = Definition.of(new FilterDefinition("RewritePath=/api/(?<s>.*), /$\\{s}"));

        assertThat(definition.arg("regexp", 0)).isEqualTo("/api/(?<s>.*)");
        assertThat(definition.arg("replacement", 1)).isEqualTo("/$\\{s}");
        assertThat(definition.argsText()).isEqualTo("/api/(?<s>.*), /$\\{s}");
    }

    @Test
    void readsNamedArgumentsByName() {
        final Map<String, String> args = new LinkedHashMap<>();
        args.put("name", "X-Version");
        args.put("value", "2");
        final Definition definition = new Definition("AddRequestHeader", args);

        assertThat(definition.arg("value", 0)).isEqualTo("2");
        assertThat(definition.argsText()).isEqualTo("name: X-Version, value: 2");
    }

    @Test
    void missingArgumentIsNull() {
        assertThat(new Definition("StripPrefix", null).arg("parts", 0)).isNull();
    }

    @Test
    void summarizesPredicates() {
        final List<Definition> predicates = List.of(
                new Definition("Path", Map.of("_genkey_0", "/api/**")),
                new Definition("Method", Map.of("_genkey_0", "GET")));

        assertThat(Definition.summary(predicates)).isEqualTo("Path=/api/** && Method=GET");
    }
}
