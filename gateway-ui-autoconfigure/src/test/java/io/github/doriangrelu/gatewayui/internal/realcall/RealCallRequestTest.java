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

import io.github.doriangrelu.gatewayui.internal.i18n.Message;
import io.github.doriangrelu.gatewayui.internal.tester.TestRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RealCallRequestTest {

    private static final String BASE_PATH = "/gateway-ui";

    @Test
    void acceptsAGatewayPath() {
        assertThat(validate("/api/orders/42")).isNull();
        assertThat(validate("/gateway-uix/orders")).isNull();
    }

    @Test
    void refusesThePathsOfTheUi() {
        assertThat(validate("/gateway-ui").key()).isEqualTo("tester.call.error.ui");
        assertThat(validate("/gateway-ui/tester/call").key()).isEqualTo("tester.call.error.ui");
    }

    @Test
    void refusesAnInvalidRequest() {
        final TestRequest invalid = new TestRequest("G-T", "/api", null, null, null);

        assertThat(new RealCallRequest(invalid, BASE_PATH).validate().key()).isEqualTo("tester.error.method");
    }

    @Test
    void sendsTheBodyOnlyForWriteMethods() {
        assertThat(new RealCallRequest(new TestRequest("POST", "/api", null, null, null, "{}"), BASE_PATH).body())
                .contains("{}");
        assertThat(new RealCallRequest(new TestRequest("GET", "/api", null, null, null, "{}"), BASE_PATH).body()).isEmpty();
        assertThat(new RealCallRequest(new TestRequest("POST", "/api", null, null, null, ""), BASE_PATH).body()).isEmpty();
    }

    private static Message validate(final String path) {
        return new RealCallRequest(new TestRequest("GET", path, null, null, null), BASE_PATH).validate();
    }
}
