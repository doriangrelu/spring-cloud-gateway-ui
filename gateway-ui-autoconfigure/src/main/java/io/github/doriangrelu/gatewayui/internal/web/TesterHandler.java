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

import java.util.HashMap;
import java.util.Map;

import io.github.doriangrelu.gatewayui.internal.i18n.Message;
import io.github.doriangrelu.gatewayui.internal.realcall.RealCaller;
import io.github.doriangrelu.gatewayui.internal.tester.RouteTester;
import io.github.doriangrelu.gatewayui.internal.tester.TestRequest;
import io.github.doriangrelu.gatewayui.internal.tester.TestResult;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Page du testeur de routes : simulation (ADR 0004) et, quand l'exploitant l'a activé, appel réel (ADR 0015).
 */
public class TesterHandler {

    private final UiContexts contexts;

    private final TemplateRenderer renderer;

    private final RouteTester routeTester;

    private final RealCaller realCaller;

    /**
     * Crée le handler du testeur.
     *
     * @param contexts fabrique des contextes de rendu
     * @param renderer moteur de templates
     * @param routeTester testeur de routes
     * @param realCaller émetteur des appels réels, {@code null} quand le mode est désactivé
     */
    public TesterHandler(final UiContexts contexts, final TemplateRenderer renderer, final RouteTester routeTester,
            final RealCaller realCaller) {
        this.contexts = contexts;
        this.renderer = renderer;
        this.routeTester = routeTester;
        this.realCaller = realCaller;
    }

    /**
     * Testeur de routes. Sans chemin saisi, affiche le formulaire vide.
     *
     * @param request requête HTTP, avec les paramètres du formulaire
     * @return la page, ou le seul résultat pour une soumission htmx
     */
    public Mono<ServerResponse> tester(final ServerRequest request) {
        final MultiValueMap<String, String> query = request.queryParams();
        final TestRequest testRequest = TestRequest.from(query);
        final String template = HtmlResponses.isHtmx(request) ? "testerResult" : "tester";
        return page(request, testRequest)
                .flatMap(params -> TestRequest.isEmpty(query) ? Mono.just(params) : simulate(testRequest, params))
                .flatMap(params -> HtmlResponses.ok(renderer, template, params));
    }

    /**
     * Appel réel de la requête saisie. La simulation de la même requête est calculée pour comparer la route prévue à
     * la route réellement empruntée, sans être affichée. Quand le mode est désactivé, l'URL répond 404 sans rien
     * appeler : elle reste déclarée pour qu'une route {@code Path=/**} de la Gateway ne la capte pas.
     *
     * @param request requête HTTP, avec les champs du formulaire
     * @return le résultat de l'appel, ou la page complète en mode Appel réel hors htmx
     */
    public Mono<ServerResponse> call(final ServerRequest request) {
        if (realCaller == null) {
            return HtmlResponses.notFound(renderer, contexts.create(request, "tester"), Message.of("tester.call.disabled"));
        }
        return request.formData().flatMap(form -> call(request, TestRequest.from(form)));
    }

    private Mono<ServerResponse> call(final ServerRequest request, final TestRequest testRequest) {
        final boolean htmx = HtmlResponses.isHtmx(request);
        return page(request, testRequest)
                .flatMap(params -> simulate(testRequest, params))
                .zipWith(realCaller.call(testRequest), (params, callResult) -> {
                    params.put("callResult", callResult);
                    params.put("realMode", true);
                    params.put("simulatedRouteId", simulatedRouteId(params));
                    return params;
                })
                .flatMap(params -> HtmlResponses.ok(renderer, htmx ? "testerCallResult" : "tester", params));
    }

    private Mono<Map<String, Object>> simulate(final TestRequest testRequest, final Map<String, Object> params) {
        final Message error = testRequest.validate();
        if (error != null) {
            params.put("error", error);
            return Mono.just(params);
        }
        return routeTester.test(testRequest).map(result -> {
            params.put("result", result);
            return params;
        });
    }

    private Mono<Map<String, Object>> page(final ServerRequest request, final TestRequest testRequest) {
        // HashMap : les templates attendent des paramètres présents, même nuls
        final Map<String, Object> params = new HashMap<>();
        params.put("ui", contexts.create(request, "tester"));
        params.put("request", testRequest);
        params.put("result", null);
        params.put("error", null);
        params.put("realCallEnabled", realCaller != null);
        params.put("realMode", false);
        params.put("csrfHeaders", null);
        params.put("callResult", null);
        params.put("simulatedRouteId", null);
        if (realCaller == null) {
            return Mono.just(params);
        }
        return CsrfToken.of(request).map(csrf -> {
            params.put("csrfHeaders", csrf.map(TesterHandler::hxHeaders).orElse(null));
            return params;
        });
    }

    private static String simulatedRouteId(final Map<String, Object> params) {
        return params.get("result") instanceof TestResult result ? result.selectedRouteId() : null;
    }

    /** Valeur de l'attribut {@code hx-headers} : htmx renvoie le jeton CSRF de Spring Security avec l'appel réel. */
    private static String hxHeaders(final CsrfToken csrf) {
        return "{\"" + csrf.headerName() + "\": \"" + csrf.token() + "\"}";
    }
}
