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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "gateway.ui.enabled=true",
        "gateway.ui.services.billing.url=http://billing.test:8080",
        "gateway.ui.services.billing.display-name=Facturation",
        "spring.cloud.gateway.server.webflux.routes[0].id=orders",
        "orders.url=http://orders.test:8080",
        "spring.cloud.gateway.server.webflux.routes[0].uri=${orders.url}",
        "spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/api/orders/**",
        "spring.cloud.gateway.server.webflux.routes[0].filters[0]=StripPrefix=1",
        "spring.cloud.gateway.server.webflux.routes[1].id=users",
        "spring.cloud.gateway.server.webflux.routes[1].uri=http://users.test:8080",
        "spring.cloud.gateway.server.webflux.routes[1].predicates[0]=Path=/api/users/{id}",
        "spring.cloud.gateway.server.webflux.routes[1].filters[0]=SetPath=/v2/users/{id}",
        // Route attrape-tout : ne doit masquer ni les pages ni les ressources de l'UI
        "spring.cloud.gateway.server.webflux.routes[2].id=catch-all",
        "spring.cloud.gateway.server.webflux.routes[2].uri=http://fallback.test:8080",
        "spring.cloud.gateway.server.webflux.routes[2].order=100",
        "spring.cloud.gateway.server.webflux.routes[2].predicates[0]=Path=/**",
        // Route qui répond sans appel réseau : ses réponses ne doivent pas porter les en-têtes de l'UI
        "spring.cloud.gateway.server.webflux.routes[3].id=no-op",
        "spring.cloud.gateway.server.webflux.routes[3].uri=no://op",
        "spring.cloud.gateway.server.webflux.routes[3].predicates[0]=Path=/no-op",
        "spring.cloud.gateway.server.webflux.routes[3].filters[0]=SetStatus=204",
        // Placeholders dans les arguments, en forme raccourcie et développée : l'éditeur doit les rétablir
        "spring.cloud.gateway.server.webflux.routes[4].id=secured",
        "spring.cloud.gateway.server.webflux.routes[4].uri=http://secured.test:8080",
        "spring.cloud.gateway.server.webflux.routes[4].predicates[0]=Path=/secured/**",
        "spring.cloud.gateway.server.webflux.routes[4].filters[0]=AddRequestHeader=Authorization, Bearer ${api.token:dev-token}",
        "spring.cloud.gateway.server.webflux.routes[4].filters[1].name=SetRequestHeader",
        "spring.cloud.gateway.server.webflux.routes[4].filters[1].args.name=X-Env",
        "spring.cloud.gateway.server.webflux.routes[4].filters[1].args.value=${env.name:dev}",
        // Filtre par défaut : la simulation de la route éditée doit le rejouer
        "spring.cloud.gateway.server.webflux.default-filters[0]=AddRequestHeader=X-Gateway, ui",
})
class GatewayUiIntegrationTest {

    @Autowired
    private ApplicationContext context;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToApplicationContext(context).build();
    }

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
    void editorApiReadsDeclaredRoutesAsEditableRoutes() {
        client.get().uri("/gateway-ui/api/editor/routes").exchange()
                .expectStatus().isOk()
                .expectHeader().exists("Content-Security-Policy")
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("orders")
                .jsonPath("$[0].predicates[0].values.patterns").isEqualTo("/api/orders/**")
                .jsonPath("$[0].filters[0].values.parts").isEqualTo("1")
                .jsonPath("$[1].filters[0].name").isEqualTo("SetPath")
                .jsonPath("$[1].filters[0].values.template").isEqualTo("/v2/users/{id}")
                .jsonPath("$[?(@.id == 'catch-all')].order").isEqualTo(List.of(100));
    }

    @Test
    void editorApiRestoresPlaceholdersOfTheConfiguration() {
        client.get().uri("/gateway-ui/api/editor/routes").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[?(@.id == 'orders')].uri").isEqualTo(List.of("${orders.url}"))
                .jsonPath("$[?(@.id == 'secured')].filters[0].values.value").isEqualTo(List.of("Bearer ${api.token:dev-token}"))
                .jsonPath("$[?(@.id == 'secured')].filters[1].values.value").isEqualTo(List.of("${env.name:dev}"))
                .jsonPath("$[?(@.id == 'secured')].filters[1].values.name").isEqualTo(List.of("X-Env"));
    }

    @Test
    void editorApiListsFactoriesAndJavaRoutes() {
        client.get().uri("/gateway-ui/api/editor/factories").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.filters[?(@.name == 'StripPrefix')].fields[0]").isEqualTo(List.of("parts"))
                .jsonPath("$.predicates[?(@.name == 'Path')].shortcutType").isEqualTo(List.of("GATHER_LIST_TAIL_FLAG"));
        client.get().uri("/gateway-ui/api/editor/java-routes").exchange()
                .expectStatus().isOk()
                .expectBody().json("[]");
    }

    @Test
    void editorApiGeneratesYaml() {
        client.post().uri("/gateway-ui/api/editor/yaml")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"comments": ["Export"],
                         "routes": [{"id": "orders", "uri": "http://orders:8080", "order": 0,
                                     "predicates": [{"name": "Path", "values": {"patterns": "/api/orders/**"}}],
                                     "filters": [{"name": "StripPrefix", "values": {"parts": "1"}}]}]}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body)
                        .startsWith("# Export\nspring:")
                        .contains("- Path=/api/orders/**")
                        .contains("- StripPrefix=1"));
    }

    @Test
    void editorApiSimulatesTheEditedRouteWithTheGatewayFactories() {
        simulate("""
                {"route": {"id": "", "uri": "${orders.url}", "order": 0,
                           "predicates": [{"name": "Path", "values": {"patterns": "/shop/{id}"}}],
                           "filters": [{"name": "SetPath", "values": {"template": "/v3/items/{id}"}}]},
                 "method": "GET", "path": "/shop/5"}
                """, "en")
                .jsonPath("$.error").doesNotExist()
                .jsonPath("$.matched").isEqualTo(true)
                .jsonPath("$.targetUrl").isEqualTo("http://orders.test:8080/v3/items/5")
                .jsonPath("$.variables.id").isEqualTo("5")
                .jsonPath("$.steps[?(@.filter == 'AddRequestHeader')].note").isNotEmpty()
                .jsonPath("$.steps[?(@.filter == 'SetPath')].pathAfter").isEqualTo(List.of("/v3/items/5"));
    }

    @Test
    void editorApiReportsAnEditedRouteThatDoesNotMatch() {
        simulate("""
                {"route": {"id": "draft", "uri": "http://draft.test", "order": 0,
                           "predicates": [{"name": "Path", "values": {"patterns": "/draft/**"}}], "filters": []},
                 "method": "GET", "path": "/other"}
                """, "en")
                .jsonPath("$.error").doesNotExist()
                .jsonPath("$.matched").isEqualTo(false)
                .jsonPath("$.predicate").isEqualTo("Path=/draft/**");
    }

    @Test
    void editorApiReportsAnInvalidEditedRouteInTheUserLanguage() {
        simulate("""
                {"route": {"id": "draft", "uri": "http://draft.test", "order": 0,
                           "predicates": [{"name": "Path", "values": {"patterns": "/draft/**"}}],
                           "filters": [{"name": "Nope", "values": {}}]},
                 "method": "GET", "path": "/draft/1"}
                """, "fr")
                .jsonPath("$.matched").isEqualTo(false)
                .jsonPath("$.error").value(error -> assertThat((String) error).startsWith("Route invalide : ").contains("Nope"));
    }

    @Test
    void editorApiValidatesTheTestedRequest() {
        simulate("""
                {"route": {"id": "draft", "uri": "http://draft.test", "order": 0, "predicates": [], "filters": []},
                 "method": "FETCH IT", "path": "/draft"}
                """, "en")
                .jsonPath("$.error").value(error -> assertThat((String) error).contains("FETCH IT"));
    }

    @Test
    void editorApiAdvisesOnTheEditedRouteInEnglish() {
        advise("en")
                .jsonPath("$.length()").isEqualTo(4)
                .jsonPath("$[0].level").isEqualTo("ERROR")
                .jsonPath("$[0].target").isEqualTo("id")
                .jsonPath("$[0].message").isEqualTo("The identifier « orders » is already used by another route.")
                .jsonPath("$[1].target").isEqualTo("predicates[0].regexp")
                .jsonPath("$[1].message").value(message -> assertThat((String) message)
                        .startsWith("Invalid regular expression in « regexp »: "))
                .jsonPath("$[2].level").isEqualTo("WARNING")
                .jsonPath("$[2].target").isEqualTo("predicates[1].patterns")
                .jsonPath("$[3].target").isEqualTo("filters[0].value")
                .jsonPath("$[3].message").isEqualTo(
                        "The value of « value » looks like a secret written in clear: use a placeholder instead.");
    }

    @Test
    void editorApiAdvisesOnTheEditedRouteInFrench() {
        advise("fr")
                .jsonPath("$[0].message").isEqualTo("L'identifiant « orders » est déjà utilisé par une autre route.")
                .jsonPath("$[2].message").isEqualTo(
                        "Path=/** prend toutes les requêtes : les routes évaluées après celle-ci ne sont jamais atteintes.");
    }

    private WebTestClient.BodyContentSpec advise(final String language) {
        return client.post().uri("/gateway-ui/api/editor/advice")
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"route": {"id": "orders", "uri": "${orders.url}", "order": 0,
                                   "predicates": [{"name": "Header", "values": {"header": "X-Env", "regexp": "[a-z"}},
                                                  {"name": "Path", "values": {"patterns": "/**"}}],
                                   "filters": [{"name": "AddRequestHeader",
                                                "values": {"name": "Authorization", "value": "Bearer eyJhbGciOiJIUzI1NiJ9"}}]},
                         "routes": [{"id": "orders", "uri": "http://orders.test", "order": 0, "predicates": [], "filters": []}]}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody();
    }

    private WebTestClient.BodyContentSpec simulate(final String body, final String language) {
        return client.post().uri("/gateway-ui/api/editor/simulate")
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody();
    }

    @Test
    void gatewayRoutesDoNotCarryUiSecurityHeaders() {
        client.get().uri("/no-op").exchange()
                .expectStatus().isNoContent()
                .expectHeader().doesNotExist("Content-Security-Policy")
                .expectHeader().doesNotExist("X-Frame-Options");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestGateway {
    }
}
