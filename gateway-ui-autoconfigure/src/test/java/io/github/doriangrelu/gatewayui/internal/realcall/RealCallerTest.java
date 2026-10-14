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

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;

import io.github.doriangrelu.gatewayui.autoconfigure.GatewayUiProperties;
import io.github.doriangrelu.gatewayui.internal.tester.TestRequest;
import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class RealCallerTest {

    private static final GatewayUiProperties.RealCall CONFIGURATION = new GatewayUiProperties.RealCall(true, null, null);

    private static final TestRequest REQUEST = new TestRequest("GET", "/api/orders", null, null, null);

    @Test
    void cannotCallBeforeTheServerIsStarted() {
        final RealCallResult result = caller(new MockEnvironment()).call(REQUEST).block();

        assertThat(result.failed()).isTrue();
        assertThat(result.error().key()).isEqualTo("tester.call.error.notStarted");
    }

    @Test
    void reportsAnUnreachableServer() throws IOException {
        final int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        final RealCallResult result = caller(new MockEnvironment().withProperty("local.server.port", String.valueOf(closedPort)))
                .call(REQUEST).block();

        assertThat(result.error().key()).isEqualTo("tester.call.error.unreachable");
    }

    @Test
    void usesHttpsOnlyWhenTlsIsConfigured() {
        final MockEnvironment environment = new MockEnvironment().withProperty("local.server.port", "8443");

        assertThat(new LocalServerAddress(environment).resolve(REQUEST.uri()).orElseThrow())
                .hasToString("http://127.0.0.1:8443/api/orders");
        environment.setProperty("server.ssl.bundle", "web");
        assertThat(new LocalServerAddress(environment).resolve(REQUEST.uri()).orElseThrow()).hasScheme("https");
        environment.setProperty("server.ssl.enabled", "false");
        assertThat(new LocalServerAddress(environment).resolve(REQUEST.uri()).orElseThrow()).hasScheme("http");
    }

    private static RealCaller caller(final MockEnvironment environment) {
        return new RealCaller(new LocalServerAddress(environment), "/gateway-ui", CONFIGURATION,
                new CallRegistry(Duration.ofSeconds(5)));
    }
}
