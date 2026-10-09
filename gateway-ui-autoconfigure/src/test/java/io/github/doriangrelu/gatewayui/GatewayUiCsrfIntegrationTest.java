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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.web.server.csrf.CsrfWebFilter;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'éditeur reste utilisable quand l'application hôte active la protection CSRF de Spring Security (ADR 0011) : la
 * page fournit le jeton, que les scripts renvoient dans leurs appels POST.
 */
@SpringBootTest(properties = "gateway.ui.enabled=true")
class GatewayUiCsrfIntegrationTest {

    private static final String YAML_REQUEST = "{\"routes\": [], \"comments\": []}";

    @Autowired
    private ApplicationContext context;

    @Test
    void editorPostsAreAcceptedOnlyWithTheTokenOfThePage() {
        final WebTestClient client = WebTestClient.bindToApplicationContext(context).build();
        final EntityExchangeResult<String> page = client.get().uri("/gateway-ui/editor").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult();
        final String session = page.getResponseCookies().getFirst("SESSION").getValue();
        final String header = attribute(page.getResponseBody(), "data-csrf-header");
        final String token = attribute(page.getResponseBody(), "data-csrf-token");
        assertThat(header).isEqualTo("X-CSRF-TOKEN");

        client.post().uri("/gateway-ui/api/editor/yaml").cookie("SESSION", session)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(YAML_REQUEST)
                .exchange()
                .expectStatus().isForbidden();
        client.post().uri("/gateway-ui/api/editor/yaml").cookie("SESSION", session).header(header, token)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(YAML_REQUEST)
                .exchange()
                .expectStatus().isOk();
    }

    private static String attribute(final String html, final String name) {
        final Matcher matcher = Pattern.compile(name + "=\"([^\"]+)\"").matcher(html);
        assertThat(matcher.find()).as(name).isTrue();
        return matcher.group(1);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestGateway {

        // Le filtre CSRF de Spring Security seul, sans sa configuration automatique : il protège toute l'application
        @Bean
        CsrfWebFilter csrfWebFilter() {
            return new CsrfWebFilter();
        }
    }
}
