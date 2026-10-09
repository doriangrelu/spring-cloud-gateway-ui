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
package io.github.doriangrelu.gatewayui.internal.tester;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.doriangrelu.gatewayui.internal.inspect.Definition;
import io.github.doriangrelu.gatewayui.internal.inspect.FilterStep;
import io.github.doriangrelu.gatewayui.internal.tester.TestResult.Simulation;
import io.github.doriangrelu.gatewayui.internal.tester.TestResult.Step;
import io.github.doriangrelu.gatewayui.internal.tester.TestResult.StepStatus;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * Rejoue sur le papier les filtres de la route retenue et calcule la requête transmise au service.
 *
 * <p>Les filtres ne sont jamais exécutés. L'ordre vient de la route effective ({@link Route#getFilters()}), les
 * arguments de sa déclaration ({@code default-filters} puis filtres de la route) ; l'effet de chaque filtre connu est
 * fourni par {@link FilterEffects}.
 */
class FilterSimulator {

    private static final String GLOBAL_FILTERS_NOTE =
            "Les filtres globaux (X-Forwarded-*, load balancer, métriques...) ne sont pas simulés.";

    private static final String JAVA_ROUTE_NOTE =
            "Route définie en Java (DSL) : seuls ses prédicats peuvent être évalués, ses filtres ne sont pas simulés.";

    private FilterSimulator() {
    }

    /**
     * Simule la transformation de la requête par la route retenue.
     *
     * @param route route retenue
     * @param routeDefinition déclaration de la route, {@code null} pour une route Java DSL
     * @param defaultFilters filtres appliqués par défaut aux routes déclaratives
     * @param exchange échange sur lequel les prédicats de la route ont été évalués
     * @return la simulation
     */
    static Simulation simulate(final Route route, final RouteDefinition routeDefinition,
            final List<FilterDefinition> defaultFilters, final ServerWebExchange exchange) {
        final SimulationState state = new SimulationState(exchange);
        final List<Step> steps = simulateFilters(route, declaredFilters(routeDefinition, defaultFilters),
                routeDefinition != null, state);
        final URI target = targetUri(route.getUri());
        final String targetUrl = targetUrl(target, state);
        applyTargetHost(target, state);
        return new Simulation(new LinkedHashMap<>(state.uriVariables()), steps, targetUrl, flatten(state.headers()),
                routeDefinition != null, notes(routeDefinition != null, target));
    }

    private static List<Definition> declaredFilters(final RouteDefinition routeDefinition,
            final List<FilterDefinition> defaultFilters) {
        final List<Definition> declared = new ArrayList<>();
        if (routeDefinition != null) {
            defaultFilters.forEach(definition -> declared.add(Definition.of(definition)));
            routeDefinition.getFilters().forEach(definition -> declared.add(Definition.of(definition)));
        }
        return declared;
    }

    private static List<Step> simulateFilters(final Route route, final List<Definition> declared,
            final boolean declarative, final SimulationState state) {
        final List<Step> steps = new ArrayList<>();
        for (final GatewayFilter filter : route.getFilters()) {
            final FilterStep described = FilterStep.ofRoute(filter);
            final Definition definition = take(declared, described.name());
            steps.add(definition != null ? apply(definition, state) : undeclared(described, declarative));
        }
        return steps;
    }

    /** Retire et retourne la première déclaration portant ce nom : deux filtres identiques se suivent dans l'ordre. */
    private static Definition take(final List<Definition> declared, final String name) {
        final Iterator<Definition> iterator = declared.iterator();
        while (iterator.hasNext()) {
            final Definition definition = iterator.next();
            if (definition.name().equals(name)) {
                iterator.remove();
                return definition;
            }
        }
        return null;
    }

    private static Step apply(final Definition definition, final SimulationState state) {
        final Optional<FilterEffect> effect = FilterEffects.find(definition.name());
        if (effect.isEmpty()) {
            return unsupported(definition);
        }
        final String before = state.path();
        try {
            final String note = effect.get().apply(definition, state);
            return new Step(definition.name(), definition.argsText(), StepStatus.APPLIED, before, state.path(), note);
        }
        catch (final RuntimeException ex) {
            return new Step(definition.name(), definition.argsText(), StepStatus.NOT_SIMULATED, null, null,
                    "Simulation impossible : " + ex.getMessage());
        }
    }

    private static Step unsupported(final Definition definition) {
        final boolean responseOnly = definition.name().contains("Response");
        return new Step(definition.name(), definition.argsText(),
                responseOnly ? StepStatus.RESPONSE_ONLY : StepStatus.NOT_SIMULATED, null, null,
                responseOnly ? "Agit sur la réponse" : "Effet non simulé");
    }

    private static Step undeclared(final FilterStep described, final boolean declarative) {
        final String note = declarative ? "Filtre personnalisé : effet non simulé" : "Route définie en Java : effet inconnu";
        return new Step(described.name(), described.description(), StepStatus.NOT_SIMULATED, null, null, note);
    }

    /** Une URI comme {@code lb:ws://service} porte son vrai schéma après le préfixe. */
    private static URI targetUri(final URI routeUri) {
        final String schemeSpecificPart = routeUri.getSchemeSpecificPart();
        if (routeUri.getHost() == null && schemeSpecificPart != null && schemeSpecificPart.contains("://")) {
            return URI.create(schemeSpecificPart);
        }
        return routeUri;
    }

    /** Comme {@code RouteToRequestUrlFilter} : seuls le schéma, l'hôte et le port de la route remplacent ceux de la requête. */
    private static String targetUrl(final URI target, final SimulationState state) {
        final String query = StringUtils.hasText(state.query()) ? "?" + state.query() : "";
        final String scheme = target.getScheme() == null ? "" : target.getScheme();
        return switch (scheme) {
            case "forward" -> "forward:" + state.path() + query;
            case "no" -> "no://op (aucun appel, la réponse est produite par les filtres)";
            default -> scheme + "://" + authority(target) + state.path() + query;
        };
    }

    /** Sans {@code PreserveHostHeader}, la Gateway envoie le {@code Host} de la cible. */
    private static void applyTargetHost(final URI target, final SimulationState state) {
        if (!state.preserveHost() && target.getHost() != null) {
            state.headers().set(HttpHeaders.HOST, authority(target));
        }
    }

    private static String authority(final URI target) {
        return target.getPort() == -1 ? target.getHost() : target.getHost() + ":" + target.getPort();
    }

    private static List<Map.Entry<String, String>> flatten(final HttpHeaders headers) {
        final List<Map.Entry<String, String>> entries = new ArrayList<>();
        headers.forEach((name, values) -> values.forEach(value -> entries.add(Map.entry(name, value))));
        return entries;
    }

    private static List<String> notes(final boolean declarative, final URI target) {
        final List<String> notes = new ArrayList<>();
        if (!declarative) {
            notes.add(JAVA_ROUTE_NOTE);
        }
        notes.add(GLOBAL_FILTERS_NOTE);
        if ("lb".equalsIgnoreCase(target.getScheme())) {
            notes.add("URI lb:// : l'hôte final est choisi par le load balancer parmi les instances de « "
                    + target.getHost() + " ».");
        }
        return notes;
    }
}
