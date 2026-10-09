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

import static org.assertj.core.api.Assertions.assertThat;

class EditableRoutesTest {

    private static final FactoryDescriptor REWRITE_PATH = new FactoryDescriptor("RewritePath", List.of("regexp", "replacement"), "DEFAULT");

    private static final FactoryDescriptor PATH = new FactoryDescriptor("Path", List.of("patterns", "matchTrailingSlash"),
            "GATHER_LIST_TAIL_FLAG");

    @Test
    void mapsShortcutArgumentsToFactoryFields() {
        final EditableStep step = EditableRoutes.step("RewritePath", shortcut("/api/(?<s>.*)", "/$\\{s}"), REWRITE_PATH);

        assertThat(step.values()).containsExactly(Map.entry("regexp", "/api/(?<s>.*)"), Map.entry("replacement", "/$\\{s}"));
    }

    @Test
    void gathersListIntoFirstFieldAndKeepsTailFlag() {
        assertThat(EditableRoutes.step("Path", shortcut("/a/**", "/b/**"), PATH).values())
                .containsExactly(Map.entry("patterns", "/a/**, /b/**"));
        assertThat(EditableRoutes.step("Path", shortcut("/a/**", "false"), PATH).values())
                .containsExactly(Map.entry("patterns", "/a/**"), Map.entry("matchTrailingSlash", "false"));
    }

    @Test
    void keepsNamedArguments() {
        final Map<String, String> args = new LinkedHashMap<>();
        args.put("redis-rate-limiter.replenishRate", "10");
        final FactoryDescriptor rateLimiter = new FactoryDescriptor("RequestRateLimiter", List.of(), "DEFAULT");

        assertThat(EditableRoutes.step("RequestRateLimiter", args, rateLimiter).values())
                .containsExactly(Map.entry("redis-rate-limiter.replenishRate", "10"));
    }

    @Test
    void namesArgumentsOfUnknownFactoriesByPosition() {
        assertThat(EditableRoutes.step("Custom", shortcut("a", "b"), FactoryDescriptor.unknown("Custom")).values())
                .containsExactly(Map.entry("arg0", "a"), Map.entry("arg1", "b"));
    }

    private static Map<String, String> shortcut(final String... values) {
        final Map<String, String> args = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i++) {
            args.put("_genkey_" + i, values[i]);
        }
        return args;
    }
}
