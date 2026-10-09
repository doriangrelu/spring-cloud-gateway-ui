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
package io.github.doriangrelu.gatewayui.internal.editor;

import java.util.List;
import java.util.Objects;

import io.github.doriangrelu.gatewayui.internal.inspect.GatewayInspector;
import io.github.doriangrelu.gatewayui.internal.tester.TestRequest;
import org.springframework.cloud.gateway.route.Route;
import reactor.core.publisher.Mono;

/**
 * Point d'entrée de l'éditeur côté serveur (ADR 0011) : ce que la Gateway sait des routes et des fabriques, et la
 * génération du YAML. L'éditeur ne modifie jamais la Gateway.
 */
public class EditorService {

    private final GatewayInspector inspector;

    private final FactoryCatalog catalog;

    private final EditableRoutes editableRoutes;

    private final RouteYaml yaml;

    private final RawRouteConfiguration rawConfiguration;

    private final EditedRouteSimulator simulator;

    /**
     * Crée le service.
     *
     * @param inspector inspecteur de la Gateway
     * @param catalog fabriques de la Gateway
     * @param rawConfiguration valeurs brutes de la configuration, pour rétablir les placeholders
     * @param simulator simulation de la route éditée
     */
    public EditorService(final GatewayInspector inspector, final FactoryCatalog catalog,
            final RawRouteConfiguration rawConfiguration, final EditedRouteSimulator simulator) {
        this.inspector = inspector;
        this.catalog = catalog;
        this.editableRoutes = new EditableRoutes(catalog);
        this.yaml = new RouteYaml(catalog);
        this.rawConfiguration = rawConfiguration;
        this.simulator = simulator;
    }

    /**
     * Routes déclarées de la Gateway, éditables, dans l'ordre d'évaluation, avec leurs placeholders d'origine.
     *
     * @return les routes éditables
     */
    public Mono<List<EditableRoute>> routes() {
        return Mono.zip(routeIds(), inspector.definitions())
                .map(tuple -> tuple.getT1().stream()
                        .map(tuple.getT2()::get)
                        .filter(Objects::nonNull)
                        .map(definition -> editableRoutes.of(definition, rawConfiguration))
                        .toList());
    }

    /**
     * Routes déclarées en Java : sans définition, elles ne sont ni éditables ni exportables.
     *
     * @return les identifiants des routes Java, dans l'ordre d'évaluation
     */
    public Mono<List<String>> javaRoutes() {
        return Mono.zip(routeIds(), inspector.definitions())
                .map(tuple -> tuple.getT1().stream().filter(id -> !tuple.getT2().containsKey(id)).toList());
    }

    /**
     * Fabriques de prédicats et de filtres disponibles.
     *
     * @return les fabriques
     */
    public Factories factories() {
        return new Factories(catalog.predicates(), catalog.filters());
    }

    /**
     * YAML d'une liste de routes.
     *
     * @param request routes et commentaires d'en-tête
     * @return le document YAML
     */
    public String yaml(final YamlRequest request) {
        return yaml.toYaml(request.routes(), request.comments());
    }

    /**
     * Teste une requête contre la route éditée, construite par la Gateway sans lui être ajoutée.
     *
     * @param route route éditée
     * @param request requête à tester, préalablement validée
     * @return le résultat, ou l'erreur de construction de la route
     */
    public Mono<EditedRouteSimulator.Outcome> simulate(final EditableRoute route, final TestRequest request) {
        return simulator.simulate(route, request);
    }

    private Mono<List<String>> routeIds() {
        return inspector.rawRoutes().map(Route::getId).collectList();
    }

    /**
     * Fabriques de la Gateway, telles que l'éditeur les propose.
     *
     * @param predicates fabriques de prédicats
     * @param filters fabriques de filtres
     */
    public record Factories(List<FactoryDescriptor> predicates, List<FactoryDescriptor> filters) {
    }

    /**
     * Demande de génération de YAML.
     *
     * @param routes routes à écrire
     * @param comments commentaires d'en-tête (avertissements, routes retirées...)
     */
    public record YamlRequest(List<EditableRoute> routes, List<String> comments) {

        /**
         * Remplace les listes absentes par des listes vides.
         */
        public YamlRequest {
            routes = routes == null ? List.of() : List.copyOf(routes);
            comments = comments == null ? List.of() : List.copyOf(comments);
        }
    }
}
