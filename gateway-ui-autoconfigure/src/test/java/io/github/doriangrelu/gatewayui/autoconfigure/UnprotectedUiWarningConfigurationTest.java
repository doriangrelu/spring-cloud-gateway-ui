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

import io.github.doriangrelu.gatewayui.internal.web.UnprotectedUiWarning;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class UnprotectedUiWarningConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(GatewayUiAutoConfiguration.UnprotectedUiWarningConfiguration.class)
            .withBean(GatewayUiProperties.class, () -> new GatewayUiProperties(true, "/admin/gateway", true, null, null));

    @Test
    void warnsWhenSpringSecurityIsAbsent(final CapturedOutput output) {
        runner.withClassLoader(new FilteredClassLoader(SecurityWebFilterChain.class))
                .run(context -> assertThat(context).hasSingleBean(UnprotectedUiWarning.class));

        assertThat(output).contains("Gateway UI is exposed without protection on /admin/gateway/**");
    }

    @Test
    void staysSilentWhenSpringSecurityIsPresent(final CapturedOutput output) {
        runner.run(context -> assertThat(context).doesNotHaveBean(UnprotectedUiWarning.class));

        assertThat(output).doesNotContain("Gateway UI is exposed without protection");
    }
}
