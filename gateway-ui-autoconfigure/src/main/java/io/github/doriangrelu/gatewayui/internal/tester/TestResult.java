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

import java.util.List;
import java.util.Map;

/**
 * Résultat du testeur : évaluation de chaque route puis simulation de la route retenue.
 *
 * @param request requête testée
 * @param requestUri URI complète de la requête testée
 * @param evaluations évaluation de chaque route, dans l'ordre d'évaluation de la Gateway
 * @param selectedRouteId route qui recevrait la requête, {@code null} si aucune ne correspond
 * @param simulation transformation de la requête par la route retenue, {@code null} si aucune ne correspond
 */
public record TestResult(TestRequest request, String requestUri, List<Evaluation> evaluations, String selectedRouteId,
        Simulation simulation) {

    /**
     * Résultat de l'évaluation des prédicats d'une route.
     */
    public enum Outcome {
        /** Tous les prédicats correspondent. */
        MATCH,
        /** Au moins un prédicat ne correspond pas. */
        NO_MATCH,
        /** L'évaluation a échoué (prédicat dépendant d'un contexte non simulé, délai dépassé...). */
        ERROR
    }

    /**
     * Évaluation d'une route.
     *
     * @param routeId identifiant de la route
     * @param order ordre de la route
     * @param predicate résumé des prédicats
     * @param outcome résultat de l'évaluation
     * @param error message d'erreur si l'évaluation a échoué
     * @param selected la route est celle qui recevrait la requête
     * @param shadowed la route correspond aussi mais une route évaluée avant elle a été retenue
     */
    public record Evaluation(String routeId, int order, String predicate, Outcome outcome, String error, boolean selected,
            boolean shadowed) {
    }

    /**
     * Issue de la simulation d'un filtre.
     */
    public enum StepStatus {
        /** L'effet du filtre a été rejoué. */
        APPLIED,
        /** Le filtre n'agit que sur la réponse : aucun effet sur la requête transmise. */
        RESPONSE_ONLY,
        /** Le filtre n'est pas simulé : son effet sur la requête est inconnu. */
        NOT_SIMULATED
    }

    /**
     * Simulation d'un filtre de la route retenue.
     *
     * @param filter nom du filtre
     * @param args arguments du filtre
     * @param status issue de la simulation
     * @param pathBefore chemin avant le filtre, {@code null} si le filtre n'a pas été rejoué
     * @param pathAfter chemin après le filtre, {@code null} si le filtre n'a pas été rejoué
     * @param note effet du filtre sur les en-têtes ou les paramètres, ou raison de l'absence de simulation
     */
    public record Step(String filter, String args, StepStatus status, String pathBefore, String pathAfter, String note) {

        /**
         * Indique si le filtre a modifié le chemin.
         *
         * @return {@code true} si le chemin a changé
         */
        public boolean changedPath() {
            return pathBefore != null && !pathBefore.equals(pathAfter);
        }
    }

    /**
     * Transformation de la requête par la route retenue.
     *
     * @param uriVariables variables extraites par les prédicats ({@code Path=/users/{id}}...)
     * @param steps simulation de chaque filtre de la route, dans l'ordre d'exécution
     * @param targetUrl URL appelée par la Gateway
     * @param headers en-têtes transmis au service (hors en-têtes X-Forwarded ajoutés par la Gateway)
     * @param declarative {@code false} pour une route Java DSL : les filtres ne peuvent pas être simulés
     * @param notes limites de la simulation à signaler
     */
    public record Simulation(Map<String, String> uriVariables, List<Step> steps, String targetUrl,
            List<Map.Entry<String, String>> headers, boolean declarative, List<String> notes) {
    }
}
