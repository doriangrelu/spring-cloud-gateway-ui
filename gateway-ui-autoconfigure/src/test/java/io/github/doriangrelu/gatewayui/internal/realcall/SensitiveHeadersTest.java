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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveHeadersTest {

    @Test
    void recognizesSensitiveNamesWhateverTheCase() {
        assertThat(List.of("Authorization", "proxy-authorization", "COOKIE", "Set-Cookie", "X-Auth-Token",
                "X-Client-Secret", "X-Password", "X-Api-Key", "api_key", "X-Credentials"))
                .allMatch(SensitiveHeaders::isSensitive);
        assertThat(List.of("Content-Type", "X-Request-Id", "Accept", "Host")).noneMatch(SensitiveHeaders::isSensitive);
    }

    @Test
    void masksOnlySensitiveValues() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.add(HttpHeaders.SET_COOKIE, "SESSION=abc");
        headers.add(HttpHeaders.SET_COOKIE, "theme=dark");

        assertThat(SensitiveHeaders.masked(headers)).containsExactly(
                Map.entry(HttpHeaders.CONTENT_TYPE, "application/json"),
                Map.entry(HttpHeaders.SET_COOKIE, SensitiveHeaders.MASK),
                Map.entry(HttpHeaders.SET_COOKIE, SensitiveHeaders.MASK));
    }
}
