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
package io.github.doriangrelu.gatewayui.internal.web;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;

import static org.assertj.core.api.Assertions.assertThat;

class UiLocaleResolverTest {

    private final UiLocaleResolver resolver = new UiLocaleResolver(Locale.ENGLISH, "/gateway-ui");

    @Test
    void languageChosenInTheUiWinsAndIsRemembered() {
        final MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/gateway-ui/routes?lang=fr")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en"));

        assertThat(resolver.resolve(request(exchange))).isEqualTo(Locale.FRENCH);
        final ResponseCookie cookie = exchange.getResponse().getCookies().getFirst(UiLocaleResolver.COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo("fr");
        assertThat(cookie.getPath()).isEqualTo("/gateway-ui");
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void rememberedLanguageWinsOverTheBrowser() {
        final MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/gateway-ui/routes")
                .cookie(new HttpCookie(UiLocaleResolver.COOKIE, "fr"))
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en"));

        assertThat(resolver.resolve(request(exchange))).isEqualTo(Locale.FRENCH);
    }

    @Test
    void browserLanguageIsUsedWhenSupported() {
        assertThat(resolve("de-DE,fr;q=0.8,en;q=0.5")).isEqualTo(Locale.FRENCH);
    }

    @Test
    void defaultLanguageWhenNothingMatches() {
        assertThat(resolve("de-DE")).isEqualTo(Locale.ENGLISH);
        assertThat(new UiLocaleResolver(Locale.FRENCH, "/ui").resolve(request(exchange(MockServerHttpRequest.get("/ui")))))
                .isEqualTo(Locale.FRENCH);
    }

    @Test
    void unsupportedChoiceAndMalformedHeaderAreIgnored() {
        final MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/gateway-ui/routes?lang=xx")
                .header(HttpHeaders.ACCEPT_LANGUAGE, ";;;"));

        assertThat(resolver.resolve(request(exchange))).isEqualTo(Locale.ENGLISH);
        assertThat(exchange.getResponse().getCookies()).isEmpty();
    }

    private Locale resolve(final String acceptLanguage) {
        return resolver.resolve(request(exchange(MockServerHttpRequest.get("/gateway-ui/routes")
                .header(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage))));
    }

    private static MockServerWebExchange exchange(final MockServerHttpRequest.BaseBuilder<?> request) {
        return MockServerWebExchange.from(request);
    }

    private static ServerRequest request(final MockServerWebExchange exchange) {
        return ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());
    }
}
