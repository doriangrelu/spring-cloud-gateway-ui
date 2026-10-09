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
package io.github.doriangrelu.gatewayui.tester;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FilterEffectsTest {

    @Test
    void stripPrefixRemovesLeadingSegments() {
        assertThat(FilterEffects.stripPrefix("/api/orders/42", 1)).isEqualTo("/orders/42");
        assertThat(FilterEffects.stripPrefix("/api/orders/42", 2)).isEqualTo("/42");
    }

    @Test
    void stripPrefixKeepsTrailingSlash() {
        assertThat(FilterEffects.stripPrefix("/api/orders/", 1)).isEqualTo("/orders/");
    }

    @Test
    void stripPrefixOfEveryPartGivesRoot() {
        assertThat(FilterEffects.stripPrefix("/api", 1)).isEqualTo("/");
        assertThat(FilterEffects.stripPrefix("/api/", 3)).isEqualTo("/");
    }

    @Test
    void knowsOnlySideEffectFreeFilters() {
        assertThat(FilterEffects.find("RewritePath")).isPresent();
        assertThat(FilterEffects.find("RequestRateLimiter")).isEmpty();
        assertThat(FilterEffects.isPathFilter("SetPath")).isTrue();
        assertThat(FilterEffects.isPathFilter("AddRequestHeader")).isFalse();
    }
}
