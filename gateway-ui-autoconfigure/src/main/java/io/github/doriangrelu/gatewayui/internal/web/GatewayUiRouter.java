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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Properties;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Déclare les routes HTTP de l'UI.
 *
 * <p>Tout, ressources statiques comprises, passe par une {@link RouterFunction} : son mapping est prioritaire sur
 * celui de la Gateway, alors que le handler de ressources statiques de Spring Boot passe après. Une route
 * {@code Path=/**} masquerait sinon le CSS et le JavaScript de l'UI.
 */
public class GatewayUiRouter {

    private static final String HTMX_POM = "META-INF/maven/org.webjars.npm/htmx.org/pom.properties";

    private static final String ASSETS_LOCATION = "io/github/doriangrelu/gatewayui/assets/";

    private static final MediaType JAVASCRIPT = new MediaType("text", "javascript");

    private static final CacheControl ASSETS_CACHE = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();

    private GatewayUiRouter() {
    }

    /**
     * Crée les routes de l'UI.
     *
     * @param basePath préfixe des URL de l'UI
     * @param handler handler des pages
     * @param editorApi API de l'éditeur
     * @return la fonction de routage
     */
    public static RouterFunction<ServerResponse> create(final String basePath, final GatewayUiHandler handler,
            final EditorApiHandler editorApi) {
        return pages(basePath, handler).and(editorApi(basePath, editorApi)).and(assets(basePath)).filter(SecurityHeaders.filter());
    }

    private static RouterFunction<ServerResponse> editorApi(final String basePath, final EditorApiHandler api) {
        final String root = basePath + "/api/editor";
        return RouterFunctions.route()
                .GET(root + "/routes", api::routes)
                .GET(root + "/java-routes", api::javaRoutes)
                .GET(root + "/factories", api::factories)
                .POST(root + "/yaml", api::yaml)
                .build();
    }

    private static RouterFunction<ServerResponse> pages(final String basePath, final GatewayUiHandler handler) {
        return RouterFunctions.route()
                .GET(basePath, handler::home)
                .GET(basePath + "/", handler::home)
                .GET(basePath + "/routes", handler::routes)
                .GET(basePath + "/routes/{id}", handler::route)
                .GET(basePath + "/services", handler::services)
                .GET(basePath + "/filters", handler::globalFilters)
                .GET(basePath + "/tester", handler::tester)
                .build();
    }

    private static RouterFunction<ServerResponse> assets(final String basePath) {
        final Resource htmx = htmxScript();
        return RouterFunctions.route()
                .GET(basePath + "/assets/htmx.min.js", request -> ServerResponse.ok()
                        .contentType(JAVASCRIPT)
                        .cacheControl(ASSETS_CACHE)
                        .body(BodyInserters.fromResource(htmx)))
                .add(RouterFunctions.resources(basePath + "/assets/**", new ClassPathResource(ASSETS_LOCATION)))
                .build();
    }

    /** Le webjar htmx est versionné dans son chemin : on lit la version embarquée plutôt que de la dupliquer. */
    private static Resource htmxScript() {
        final ClassLoader classLoader = GatewayUiRouter.class.getClassLoader();
        try (InputStream input = new ClassPathResource(HTMX_POM, classLoader).getInputStream()) {
            final Properties properties = new Properties();
            properties.load(input);
            final String version = properties.getProperty("version");
            return new ClassPathResource("META-INF/resources/webjars/htmx.org/" + version + "/dist/htmx.min.js", classLoader);
        }
        catch (final IOException ex) {
            throw new UncheckedIOException("Webjar htmx.org introuvable dans le classpath", ex);
        }
    }
}
