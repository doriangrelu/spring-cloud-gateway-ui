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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "gateway.ui.enabled=true",
        "gateway.ui.services.billing.url=http://billing.test:8080",
        "gateway.ui.services.billing.display-name=Facturation",
        "spring.cloud.gateway.server.webflux.routes[0].id=orders",
        "spring.cloud.gateway.server.webflux.routes[0].uri=http://orders.test:8080",
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
                        .contains("masquée"));
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
