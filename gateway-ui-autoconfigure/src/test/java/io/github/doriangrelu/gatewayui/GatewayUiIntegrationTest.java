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
package io.github.doriangrelu.gatewayui;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayUiIntegrationTest extends GatewayUiIntegrationTestSupport {

    @Test
    void listsRoutesInEvaluationOrder() {
        client.get().uri("/gateway-ui/routes").exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class).value(body -> assertThat(body)
                        .contains("Path=/api/orders/**")
                        .containsSubsequence(">orders<", ">users<", ">catch-all<"));
    }

    @Test
    void htmxSearchReturnsOnlyTheTableFragment() {
        client.get().uri("/gateway-ui/routes?q=users").header("HX-Request", "true").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body)
                        .startsWith("<div id=\"routes-table\"")
                        .contains(">users<")
                        .doesNotContain(">orders<"));
    }

    @Test
    void showsRouteDetailWithFilterChain() {
        client.get().uri("/gateway-ui/routes/orders").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body)
                        .contains("StripPrefix")
                        .contains("RouteToRequestUrlFilter"));
    }

    @Test
    void unknownRouteIsNotFound() {
        client.get().uri("/gateway-ui/routes/nope").exchange().expectStatus().isNotFound();
    }

    @Test
    void testerSimulatesStripPrefix() {
        client.get().uri("/gateway-ui/tester?method=GET&path=/api/orders/42").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body)
                        .contains("http://orders.test:8080/orders/42")
                        .contains("shadowed"));
    }

    @Test
    void testerSimulatesSetPathWithUriVariables() {
        client.get().uri("/gateway-ui/tester?method=GET&path=/api/users/7").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body)
                        .contains("http://users.test:8080/v2/users/7"));
    }

    @Test
    void servicesMergeDeclaredAndDiscovered() {
        client.get().uri("/gateway-ui/services").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body)
                        .contains("Facturation")
                        .contains("http://orders.test:8080"));
    }

    @Test
    void assetsAreNotShadowedByCatchAllRoute() {
        client.get().uri("/gateway-ui/assets/gateway-ui.css").exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith("text/css");
        client.get().uri("/gateway-ui/assets/htmx.min.js").exchange()
                .expectStatus().isOk();
    }

    @Test
    void uiResponsesCarrySecurityHeaders() {
        for (final String path : new String[] {"/gateway-ui/routes", "/gateway-ui/routes/nope", "/gateway-ui/assets/gateway-ui.css"}) {
            client.get().uri(path).exchange()
                    .expectHeader().valueMatches("Content-Security-Policy",
                            "default-src 'none'; script-src 'self'; style-src 'self'.*frame-ancestors 'none'")
                    .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                    .expectHeader().valueEquals("X-Frame-Options", "DENY")
                    .expectHeader().valueEquals("Referrer-Policy", "no-referrer");
        }
    }

    @Test
    void uiIsInEnglishByDefault() {
        client.get().uri("/gateway-ui/routes").exchange()
                .expectBody(String.class).value(body -> assertThat(body)
                        .contains("<html lang=\"en\">")
                        .contains("Global filters"));
    }

    @Test
    void uiFollowsTheBrowserLanguage() {
        client.get().uri("/gateway-ui/routes").header(HttpHeaders.ACCEPT_LANGUAGE, "fr-FR,fr;q=0.9").exchange()
                .expectBody(String.class).value(body -> assertThat(body)
                        .contains("<html lang=\"fr\">")
                        .contains("Filtres globaux"));
    }

    @Test
    void languageCanBeChangedFromTheUi() {
        // URI déjà encodée : ne pas passer par les modèles d'URI, qui ré-encoderaient les %
        client.get().uri(URI.create("/gateway-ui/tester?path=%2Fapi%2Forders%2F1&lang=fr")).exchange()
                .expectHeader().valueMatches(HttpHeaders.SET_COOKIE, "gateway-ui-lang=fr;.*Path=/gateway-ui.*")
                .expectBody(String.class).value(body -> assertThat(body)
                        .contains("Testeur de routes")
                        .contains("href=\"/gateway-ui/tester?path=%2Fapi%2Forders%2F1&amp;lang=en\""));
    }

    @Test
    void themeFollowsTheSystemByDefault() {
        client.get().uri("/gateway-ui/routes").exchange()
                .expectBody(String.class).value(body -> assertThat(body)
                        .contains("<html lang=\"en\">")
                        .doesNotContain("data-theme="));
    }

    @Test
    void themeCanBeChangedFromTheUiAndIsRemembered() {
        client.get().uri("/gateway-ui/services?theme=dark").exchange()
                .expectHeader().valueMatches(HttpHeaders.SET_COOKIE, "gateway-ui-theme=dark;.*Path=/gateway-ui.*")
                .expectBody(String.class).value(body -> assertThat(body)
                        .contains("data-theme=\"dark\"")
                        .contains("href=\"/gateway-ui/services?theme=light\""));
        client.get().uri("/gateway-ui/routes").cookie("gateway-ui-theme", "light").exchange()
                .expectBody(String.class).value(body -> assertThat(body).contains("data-theme=\"light\""));
    }

    @Test
    void unknownThemeIsIgnored() {
        client.get().uri("/gateway-ui/routes?theme=pink").exchange()
                .expectHeader().doesNotExist(HttpHeaders.SET_COOKIE)
                .expectBody(String.class).value(body -> assertThat(body).doesNotContain("data-theme="));
    }

    @Test
    void gatewayRoutesDoNotCarryUiSecurityHeaders() {
        client.get().uri("/no-op").exchange()
                .expectStatus().isNoContent()
                .expectHeader().doesNotExist("Content-Security-Policy")
                .expectHeader().doesNotExist("X-Frame-Options");
    }
}
