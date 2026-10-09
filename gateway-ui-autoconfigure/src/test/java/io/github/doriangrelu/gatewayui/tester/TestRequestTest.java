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

import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

class TestRequestTest {

    @Test
    void appliesDefaults() {
        final TestRequest request = new TestRequest(null, "api/orders", " ", null, null);

        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.pathAndQuery()).isEqualTo("/api/orders");
        assertThat(request.host()).isEqualTo("localhost");
        assertThat(request.remoteAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void parsesHeaderLinesAndAddsHost() {
        final TestRequest request = new TestRequest("get", "/", "api.example.com", "X-Version: 2\nmalformed\nAccept: */*", null);

        final HttpHeaders headers = request.httpHeaders();

        assertThat(headers.getFirst("X-Version")).isEqualTo("2");
        assertThat(headers.getFirst(HttpHeaders.ACCEPT)).isEqualTo("*/*");
        assertThat(headers.getFirst(HttpHeaders.HOST)).isEqualTo("api.example.com");
        assertThat(headers.containsHeader("malformed")).isFalse();
    }

    @Test
    void keepsQueryString() {
        final TestRequest request = new TestRequest("GET", "/search?q=a b", null, null, null);

        assertThat(request.validate()).isNull();
        assertThat(request.uri().getRawQuery()).isEqualTo("q=a%20b");
    }

    @Test
    void rejectsInvalidMethodAndHost() {
        assertThat(new TestRequest("G-T", "/", null, null, null).validate()).startsWith("Méthode HTTP invalide");
        assertThat(new TestRequest("GET", "/", "bad host", null, null).validate()).isNotNull();
    }

    @Test
    void neverResolvesHostNames() {
        assertThat(new TestRequest("GET", "/", null, null, "10.0.0.1").inetRemoteAddress().isUnresolved()).isFalse();
        assertThat(new TestRequest("GET", "/", null, null, "example.com").inetRemoteAddress().isUnresolved()).isTrue();
    }
}
