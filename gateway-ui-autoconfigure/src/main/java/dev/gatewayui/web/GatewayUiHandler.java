package dev.gatewayui.web;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import dev.gatewayui.inspect.GatewayInspector;
import dev.gatewayui.inspect.ServiceCatalog;
import dev.gatewayui.tester.RouteTester;
import dev.gatewayui.tester.TestRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Pages de l'UI. Une requête HTMX ({@code HX-Request}) reçoit uniquement le fragment à remplacer,
 * une navigation classique reçoit la page complète : chaque écran reste accessible par son URL.
 */
public class GatewayUiHandler {

    private static final MediaType HTML = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);

    private final UiContext ui;

    private final TemplateRenderer renderer;

    private final GatewayInspector inspector;

    private final ServiceCatalog serviceCatalog;

    private final RouteTester routeTester;

    public GatewayUiHandler(String basePath, TemplateRenderer renderer, GatewayInspector inspector,
            ServiceCatalog serviceCatalog, RouteTester routeTester) {
        this.ui = new UiContext(basePath, "");
        this.renderer = renderer;
        this.inspector = inspector;
        this.serviceCatalog = serviceCatalog;
        this.routeTester = routeTester;
    }

    public Mono<ServerResponse> home(ServerRequest request) {
        return ServerResponse.temporaryRedirect(URI.create(ui.url("/routes"))).build();
    }

    public Mono<ServerResponse> routes(ServerRequest request) {
        String query = request.queryParam("q").orElse("");
        UiContext page = ui.page("routes");
        return inspector.routes(query).flatMap(routes -> html(isHtmx(request) ? "routesTable" : "routes",
                Map.of("ui", page, "routes", routes, "query", query)));
    }

    public Mono<ServerResponse> route(ServerRequest request) {
        UiContext page = ui.page("routes");
        return inspector.route(request.pathVariable("id"))
                .flatMap(route -> html("route", Map.of("ui", page, "route", route, "pipeline", inspector.pipeline(route))))
                .switchIfEmpty(Mono.defer(() -> notFound(page, "Route « " + request.pathVariable("id") + " » introuvable")));
    }

    public Mono<ServerResponse> services(ServerRequest request) {
        UiContext page = ui.page("services");
        return serviceCatalog.services().flatMap(services -> html("services", Map.of("ui", page, "services", services)));
    }

    public Mono<ServerResponse> globalFilters(ServerRequest request) {
        return html("globalFilters", Map.of("ui", ui.page("filters"), "filters", inspector.globalFilters()));
    }

    public Mono<ServerResponse> tester(ServerRequest request) {
        UiContext page = ui.page("tester");
        TestRequest testRequest = TestRequest.from(request.queryParams());
        String template = isHtmx(request) ? "testerResult" : "tester";
        Map<String, Object> params = new HashMap<>();
        params.put("ui", page);
        params.put("request", testRequest);
        params.put("result", null);
        params.put("error", null);
        if (TestRequest.isEmpty(request.queryParams())) {
            return html(template, params);
        }
        String error = testRequest.validate();
        if (error != null) {
            params.put("error", error);
            return html(template, params);
        }
        return routeTester.test(testRequest).flatMap(result -> {
            params.put("result", result);
            return html(template, params);
        });
    }

    private Mono<ServerResponse> notFound(UiContext page, String message) {
        return ServerResponse.status(HttpStatus.NOT_FOUND).contentType(HTML)
                .bodyValue(renderer.render("notFound", Map.of("ui", page, "message", message)));
    }

    private Mono<ServerResponse> html(String template, Map<String, Object> params) {
        return ServerResponse.ok().contentType(HTML).bodyValue(renderer.render(template, params));
    }

    private static boolean isHtmx(ServerRequest request) {
        return "true".equals(request.headers().firstHeader("HX-Request"))
                && !"true".equals(request.headers().firstHeader("HX-History-Restore-Request"));
    }
}
