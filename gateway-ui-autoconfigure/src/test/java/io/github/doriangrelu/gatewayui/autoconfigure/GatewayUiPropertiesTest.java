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
package io.github.doriangrelu.gatewayui.autoconfigure;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayUiPropertiesTest {

    @Test
    void realCallIsDisabledByDefault() {
        final GatewayUiProperties.RealCall realCall = bind(Map.of("gateway.ui.enabled", "true")).tester().realCall();

        assertThat(realCall.enabled()).isFalse();
        assertThat(realCall.timeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(realCall.maxBodySize()).isEqualTo(DataSize.ofKilobytes(64));
    }

    @Test
    void realCallBindsCustomValues() {
        final GatewayUiProperties.RealCall realCall = bind(Map.of(
                "gateway.ui.tester.real-call.enabled", "true",
                "gateway.ui.tester.real-call.timeout", "2s",
                "gateway.ui.tester.real-call.max-body-size", "1KB")).tester().realCall();

        assertThat(realCall.enabled()).isTrue();
        assertThat(realCall.timeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(realCall.maxBodySize()).isEqualTo(DataSize.ofKilobytes(1));
    }

    @Test
    void realCallReplacesNonPositiveValuesWithDefaults() {
        final GatewayUiProperties.RealCall realCall = bind(Map.of(
                "gateway.ui.tester.real-call.timeout", "0s",
                "gateway.ui.tester.real-call.max-body-size", "0B")).tester().realCall();

        assertThat(realCall.timeout()).isEqualTo(GatewayUiProperties.RealCall.DEFAULT_TIMEOUT);
        assertThat(realCall.maxBodySize()).isEqualTo(GatewayUiProperties.RealCall.DEFAULT_MAX_BODY_SIZE);
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacyConstructorKeepsTheDefaultTesterConfiguration() {
        final GatewayUiProperties properties = new GatewayUiProperties(true, "/admin", true, null, null);

        assertThat(properties.basePath()).isEqualTo("/admin");
        assertThat(properties.tester().realCall().enabled()).isFalse();
        assertThat(properties.tester().realCall().timeout()).isEqualTo(GatewayUiProperties.RealCall.DEFAULT_TIMEOUT);
    }

    private static GatewayUiProperties bind(final Map<String, String> values) {
        return new Binder(new MapConfigurationPropertySource(values))
                .bindOrCreate(GatewayUiProperties.PREFIX, GatewayUiProperties.class);
    }
}
