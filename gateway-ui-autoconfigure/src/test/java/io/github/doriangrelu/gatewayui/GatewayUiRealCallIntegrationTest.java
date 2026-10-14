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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

import io.github.doriangrelu.gatewayui.internal.realcall.CallRegistry;
import io.github.doriangrelu.gatewayui.internal.realcall.RealCaller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mode « appel réel » activé (ADR 0015) : un vrai serveur, des routes de la Gateway vers un service de test servi par
 * l'application elle-même, et des appels envoyés depuis le testeur.
 */
@SpringBootTest(classes = GatewayUiRealCallIntegrationTest.TestGateway.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "gateway.ui.enabled=true",
                "gateway.ui.tester.real-call.enabled=true",
                "gateway.ui.tester.real-call.timeout=1s",
                "gateway.ui.tester.real-call.max-body-size=64B",
                "spring.cloud.gateway.server.webflux.routes[0].id=backend",
                "spring.cloud.gateway.server.webflux.routes[0].uri=http://localhost:${server.port}",
                "spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/api/**",
                "spring.cloud.gateway.server.webflux.routes[0].filters[0]=StripPrefix=1",
                "spring.cloud.gateway.server.webflux.routes[0].filters[1]=PrefixPath=/backend",
                // Prioritaire pour le réseau interne : une IP saisie la fait retenir par la simulation, pas par l'appel réel
                "spring.cloud.gateway.server.webflux.routes[1].id=internal",
                "spring.cloud.gateway.server.webflux.routes[1].uri=http://localhost:${server.port}",
                "spring.cloud.gateway.server.webflux.routes[1].order=-1",
                "spring.cloud.gateway.server.webflux.routes[1].predicates[0]=Path=/api/**",
                "spring.cloud.gateway.server.webflux.routes[1].predicates[1]=RemoteAddr=10.0.0.0/8",
                "spring.cloud.gateway.server.webflux.routes[1].filters[0]=StripPrefix=1",
                "spring.cloud.gateway.server.webflux.routes[1].filters[1]=PrefixPath=/backend",
        })
@ExtendWith(OutputCaptureExtension.class)
class GatewayUiRealCallIntegrationTest {

    private static final int PORT = freePort();

    private static final String SECRET = "top-secret-token";

    @Autowired
    private ApplicationContext context;

    private WebTestClient client;

    @DynamicPropertySource
    static void serverPort(final DynamicPropertyRegistry registry) {
        registry.add("server.port", () -> PORT);
    }

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + PORT).responseTimeout(Duration.ofSeconds(10)).build();
    }

    @Test
    void declaresTheRealCallBeans() {
        assertThat(context.getBeanProvider(RealCaller.class).getIfAvailable()).isNotNull();
    }

    @Test
    void testerPageOffersTheRealCall() {
        final String page = client.get().uri("/gateway-ui/tester").exchange()
                .expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();

        assertThat(page).contains("Send for real", "id=\"tester-call-body\"", "really be sent to the Gateway")
                .contains("hx-params=\"not tester-mode,body\"", "value=\"simulation\" checked", "hx-push-url=\"false\"")
                .doesNotContain("value=\"real\" checked");
    }

    @Test
    void sendsAGetAndShowsTheResponseOfTheService(final CapturedOutput output) {
        final String html = call(Map.of("method", "GET", "path", "/api/users/42",
                "headers", "Authorization: Bearer " + SECRET), true);

        assertThat(html).contains("200 OK", "{\"id\": \"42\"}", "X-Backend", "••••")
                .contains("Forwarded to:", "http://localhost:" + PORT + "/backend/users/42")
                .doesNotContain("id=\"tester-result\"")
                .doesNotContain("SESSION=abc");
        assertThat(output.getAll()).doesNotContain(SECRET);
    }

    @Test
    void sendsTheBodyOfAWriteMethod() {
        assertThat(call(Map.of("method", "POST", "path", "/api/echo", "body", "{\"status\": \"paid\"}"), true))
                .contains("200 OK", "echo:{\"status\": \"paid\"}");
    }

    @Test
    void showsTheResponseOfTheGatewayWhenNoRouteMatches() {
        assertThat(call(Map.of("path", "/nothing"), true)).contains("404 Not Found");
    }

    @Test
    void stopsASlowCallAtTheTimeout() {
        assertThat(call(Map.of("path", "/api/slow"), true)).contains("Timeout: no response within 1000 ms.");
    }

    @Test
    void truncatesALongBody() {
        assertThat(call(Map.of("path", "/api/long"), true)).contains("Truncated body");
    }

    @Test
    void explainsAnUnauthorizedResponse() {
        assertThat(call(Map.of("path", "/api/secured"), true)).contains("401 Unauthorized", "does not carry your UI session");
    }

    @Test
    void refusesToCallTheUi() {
        assertThat(call(Map.of("path", "/gateway-ui/tester"), true)).contains("cannot be called from the tester");
    }

    @Test
    void signalsThatTheClientIpOnlyAppliesToTheSimulation() {
        assertThat(call(Map.of("path", "/api/users/1", "remoteAddress", "10.0.0.1"), true))
                .contains("The client IP you entered (10.0.0.1) only applies to the simulation");
    }

    @Test
    void showsTheRouteReallyTakenWhenItMatchesTheSimulation() {
        assertThat(call(Map.of("path", "/api/users/1"), true))
                .contains("Route taken:", "/gateway-ui/routes/backend", "Same as the simulation");
    }

    @Test
    void signalsAGapBetweenTheSimulationAndTheRealCall() {
        assertThat(call(Map.of("path", "/api/users/1", "remoteAddress", "10.0.0.2"), true))
                .contains("/gateway-ui/routes/backend", "Differs from the simulation, which expected internal");
    }

    @Test
    void reportsThatNoRouteHandledTheRequest() {
        assertThat(call(Map.of("path", "/nothing"), true)).contains("No route handled the request.", "Same as the simulation");
    }

    @Test
    void neverForwardsTheCorrelationMarker() {
        assertThat(call(Map.of("path", "/api/headers"), true)).contains("200 OK", "host").doesNotContainIgnoringCase(CallRegistry.HEADER);
    }

    @Test
    void doesNotAddAGlobalFilter() {
        final String page = client.get().uri("/gateway-ui/filters").exchange()
                .expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();

        assertThat(page).doesNotContain("CallCorrelation");
    }

    @Test
    void rendersTheFullPageWithoutHtmx() {
        assertThat(call(Map.of("path", "/api/users/7"), false)).contains("<html", "id=\"tester-form\"", "{\"id\": \"7\"}")
                .contains("value=\"real\" checked");
    }

    private String call(final Map<String, String> fields, final boolean htmx) {
        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        fields.forEach(form::add);
        return client.post().uri("/gateway-ui/tester/call")
                .headers(headers -> headers.addAll(htmx ? htmxHeaders() : new HttpHeaders()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
    }

    private static HttpHeaders htmxHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add("HX-Request", "true");
        return headers;
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
        catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestGateway {

        /** Service de test, appelé à travers la route {@code backend} de la Gateway. */
        @Bean
        RouterFunction<ServerResponse> backend() {
            return RouterFunctions.route()
                    .GET("/backend/users/{id}", request -> ServerResponse.ok()
                            .header("X-Backend", "users")
                            .header(HttpHeaders.SET_COOKIE, "SESSION=abc")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue("{\"id\": \"" + request.pathVariable("id") + "\"}"))
                    .POST("/backend/echo", request -> request.bodyToMono(String.class)
                            .flatMap(body -> ServerResponse.ok().contentType(MediaType.TEXT_PLAIN).bodyValue("echo:" + body)))
                    .GET("/backend/slow", request -> Mono.delay(Duration.ofSeconds(3)).then(ServerResponse.ok().bodyValue("late")))
                    .GET("/backend/long", request -> ServerResponse.ok().contentType(MediaType.TEXT_PLAIN).bodyValue("x".repeat(500)))
                    .GET("/backend/secured", request -> ServerResponse.status(HttpStatus.UNAUTHORIZED).build())
                    .GET("/backend/headers", request -> ServerResponse.ok().contentType(MediaType.TEXT_PLAIN)
                            .bodyValue(String.join(",", request.headers().asHttpHeaders().headerNames()).toLowerCase(Locale.ROOT)))
                    .build();
        }
    }
}
