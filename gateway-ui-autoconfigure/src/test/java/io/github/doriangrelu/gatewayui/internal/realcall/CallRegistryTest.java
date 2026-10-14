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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CallRegistryTest {

    private static final CallRegistry.Routing ORDERS = new CallRegistry.Routing("orders", "http://orders.test/orders/42");

    private Instant now = Instant.parse("2026-10-13T12:00:00Z");

    private final CallRegistry registry = new CallRegistry(Duration.ofSeconds(5), new Clock() {
        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    });

    @Test
    void recordsTheRouteOfARegisteredCall() {
        final String id = registry.register().orElseThrow();

        registry.record(id, ORDERS);

        assertThat(registry.take(id)).contains(ORDERS);
    }

    @Test
    void takesARouteOnlyOnce() {
        final String id = registry.register().orElseThrow();
        registry.record(id, ORDERS);

        registry.take(id);

        assertThat(registry.take(id)).isEmpty();
    }

    @Test
    void ignoresAnUnknownCall() {
        registry.record("forged", ORDERS);

        assertThat(registry.take("forged")).isEmpty();
        assertThat(registry.isKnown("forged")).isFalse();
    }

    @Test
    void forgetsAnExpiredCall() {
        final String id = registry.register().orElseThrow();

        now = now.plusSeconds(6);
        registry.record(id, ORDERS);

        assertThat(registry.isKnown(id)).isFalse();
        assertThat(registry.take(id)).isEmpty();
    }

    @Test
    void staysBounded() {
        IntStream.range(0, CallRegistry.MAX_CALLS).forEach(i -> registry.register());

        assertThat(registry.register()).isEmpty();
        now = now.plusSeconds(6);
        assertThat(registry.register()).isPresent();
    }

    @Test
    void generatesUnguessableIdentifiers() {
        assertThat(registry.register().orElseThrow()).isNotEqualTo(registry.register().orElseThrow()).hasSize(36);
    }
}
