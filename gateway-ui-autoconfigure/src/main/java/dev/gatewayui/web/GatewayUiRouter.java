package dev.gatewayui.web;

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
 * <p>
 * Tout, ressources statiques comprises, passe par une {@link RouterFunction} : son mapping est prioritaire sur
 * celui de la Gateway, alors que le handler de ressources statiques de Spring Boot passe après. Une route
 * {@code Path=/**} masquerait sinon le CSS et le JavaScript de l'UI.
 */
public final class GatewayUiRouter {

    private static final String HTMX_POM = "META-INF/maven/org.webjars.npm/htmx.org/pom.properties";

    private GatewayUiRouter() {
    }

    public static RouterFunction<ServerResponse> create(String basePath, GatewayUiHandler handler) {
        Resource htmx = htmxScript();
        CacheControl assetsCache = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();
        return RouterFunctions.route()
                .GET(basePath, handler::home)
                .GET(basePath + "/", handler::home)
                .GET(basePath + "/routes", handler::routes)
                .GET(basePath + "/routes/{id}", handler::route)
                .GET(basePath + "/services", handler::services)
                .GET(basePath + "/filters", handler::globalFilters)
                .GET(basePath + "/tester", handler::tester)
                .GET(basePath + "/assets/htmx.min.js", request -> ServerResponse.ok()
                        .contentType(new MediaType("text", "javascript"))
                        .cacheControl(assetsCache)
                        .body(BodyInserters.fromResource(htmx)))
                .add(RouterFunctions.resources(basePath + "/assets/**", new ClassPathResource("dev/gatewayui/assets/")))
                .build();
    }

    /** Le webjar htmx est versionné dans son chemin : on lit la version embarquée plutôt que de la dupliquer. */
    private static Resource htmxScript() {
        ClassPathResource pom = new ClassPathResource(HTMX_POM, GatewayUiRouter.class.getClassLoader());
        try (InputStream input = pom.getInputStream()) {
            Properties properties = new Properties();
            properties.load(input);
            String path = "META-INF/resources/webjars/htmx.org/" + properties.getProperty("version") + "/dist/htmx.min.js";
            return new ClassPathResource(path, GatewayUiRouter.class.getClassLoader());
        }
        catch (IOException ex) {
            throw new UncheckedIOException("Webjar htmx.org introuvable dans le classpath", ex);
        }
    }
}
