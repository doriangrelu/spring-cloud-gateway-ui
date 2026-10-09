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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayUiEditorIntegrationTest extends GatewayUiIntegrationTestSupport {

    @Test
    void editorPageProvidesTheApiAndTheScriptMessages() {
        client.get().uri("/gateway-ui/editor").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body)
                        .contains("href=\"/gateway-ui/editor\"")
                        .contains("data-api=\"/gateway-ui/api/editor\"")
                        .contains("<script type=\"module\" src=\"/gateway-ui/assets/editor/main.js\"></script>")
                        .contains("<span data-key=\"status.new\">new</span>")
                        .doesNotContain("data-csrf-header"));
        client.get().uri("/gateway-ui/editor").header(HttpHeaders.ACCEPT_LANGUAGE, "fr").exchange()
                .expectBody(String.class).value(body -> assertThat(body)
                        .contains("Éditeur de routes")
                        .contains("<span data-key=\"status.new\">nouvelle</span>"));
    }

    @Test
    void editorScriptsAreServedByTheUi() {
        for (final String script : new String[] {"main", "editor", "api", "i18n", "model", "workspace", "palette", "canvas"}) {
            client.get().uri("/gateway-ui/assets/editor/" + script + ".js").exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentTypeCompatibleWith("text/javascript");
        }
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
    void editorTesterRendersTheTesterResultForTheEditedRoute() {
        tester("""
                {"route": {"id": "draft", "uri": "${orders.url}", "order": 0,
                           "predicates": [{"name": "Path", "values": {"patterns": "/shop/{id}"}}],
                           "filters": [{"name": "SetPath", "values": {"template": "/v3/items/{id}"}}]},
                 "routes": [], "context": false, "method": "GET", "path": "/shop/5", "host": "", "headers": "X-Id: 1"}
                """)
                .value(body -> assertThat(body)
                        .startsWith("<div id=\"tester-result\">")
                        .contains("http://orders.test:8080/v3/items/5")
                        .contains("<span class=\"route-link mono\">draft</span>")
                        .doesNotContain("href=\"/gateway-ui/routes/draft\""));
    }

    @Test
    void editorTesterEvaluatesTheEditedRouteWithinTheConfiguration() {
        tester("""
                {"route": {"id": "draft", "uri": "http://draft.test", "order": 0,
                           "predicates": [{"name": "Path", "values": {"patterns": "/api/orders/**"}}], "filters": []},
                 "routes": [{"id": "orders", "uri": "http://orders.test:8080", "order": 5,
                             "predicates": [{"name": "Path", "values": {"patterns": "/api/orders/**"}}], "filters": []}],
                 "context": true, "method": "GET", "path": "/api/orders/1"}
                """)
                .value(body -> assertThat(body)
                        .contains("http://draft.test:80/api/orders/1")
                        .containsSubsequence(">draft<", ">orders<")
                        .contains("chip-shadowed"));
    }

    @Test
    void editorTesterReportsAnInvalidRoute() {
        tester("""
                {"route": {"id": "draft", "uri": "http://draft.test", "order": 0, "predicates": [],
                           "filters": [{"name": "Nope", "values": {}}]},
                 "context": false, "method": "GET", "path": "/"}
                """)
                .value(body -> assertThat(body).contains("alert-error").contains("Invalid route"));
    }

    private WebTestClient.BodySpec<String, ?> tester(final String body) {
        return client.post().uri("/gateway-ui/api/editor/test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class);
    }
}
