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
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.doriangrelu.gatewayui.internal.editor.Catalog;
import io.github.doriangrelu.gatewayui.internal.editor.CatalogEntry;
import io.github.doriangrelu.gatewayui.internal.i18n.Messages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayUiCatalogIntegrationTest extends GatewayUiIntegrationTestSupport {

    private static final Pattern FACTORY_KEY = Pattern.compile(
            "catalog\\.(predicate|filter)\\.([A-Za-z]+)\\.(summary|details|example|arg\\.(.+))");

    @Autowired
    private Catalog catalog;

    @Test
    void catalogPageDocumentsFactoriesInTheUserLanguage() {
        client.get().uri("/gateway-ui/catalog").header(HttpHeaders.ACCEPT_LANGUAGE, "fr").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body)
                        .contains("href=\"/gateway-ui/catalog\"")
                        .contains("<h2>Filtres · Chemin</h2>")
                        .contains("id=\"filter-StripPrefix\"")
                        .contains("Retire les premiers segments du chemin")
                        .contains("href=\"https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/"
                                + "gatewayfilter-factories/stripprefix-factory.html\"")
                        .contains("non documenté"));
    }

    @Test
    void editorApiServesTheTranslatedCatalog() {
        client.get().uri("/gateway-ui/api/editor/catalog").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[?(@.name == 'StripPrefix')].category").isEqualTo(List.of("Path"))
                .jsonPath("$[?(@.name == 'StripPrefix')].documented").isEqualTo(List.of(true))
                .jsonPath("$[?(@.name == 'StripPrefix')].args.parts").isEqualTo(
                        List.of("Number of segments to remove from the start, 1 by default."))
                .jsonPath("$[?(@.name == 'StripPrefix')].anchor").isEqualTo(List.of("filter-StripPrefix"));
    }

    /** Une faute de frappe dans un nom de fabrique ou de champ laisserait un texte invisible : on la détecte ici. */
    @Test
    void documentedTextsMatchRealFactoriesAndFields() {
        new Messages().keys(Locale.ENGLISH).stream()
                .filter(key -> key.startsWith("catalog.predicate.") || key.startsWith("catalog.filter."))
                .forEach(key -> {
                    final Matcher matcher = FACTORY_KEY.matcher(key);
                    assertThat(matcher.matches()).as(key).isTrue();
                    final Optional<CatalogEntry> entry = entry(matcher.group(1), matcher.group(2));
                    assertThat(entry).as(key).isPresent();
                    if (matcher.group(4) != null) {
                        assertThat(entry.get().fields()).as(key).contains(matcher.group(4));
                    }
                });
    }

    @Test
    void documentedFactoriesDescribeEveryField() {
        final Messages messages = new Messages();
        catalog.entries().stream()
                .filter(entry -> messages.contains(Locale.ENGLISH, entry.messageKey() + ".summary"))
                .forEach(entry -> {
                    assertThat(messages.contains(Locale.ENGLISH, entry.messageKey() + ".example")).as(entry.name()).isTrue();
                    entry.fields().forEach(field ->
                            assertThat(messages.contains(Locale.ENGLISH, entry.messageKey() + ".arg." + field))
                                    .as(entry.name() + "." + field).isTrue());
                });
    }

    private Optional<CatalogEntry> entry(final String kind, final String name) {
        return catalog.entries().stream().filter(entry -> entry.kind().equals(kind) && entry.name().equals(name)).findFirst();
    }
}
