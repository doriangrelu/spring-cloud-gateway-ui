package dev.gatewayui.tester;

import java.util.List;
import java.util.Map;

/**
 * Résultat du testeur : évaluation de chaque route puis simulation de la route retenue.
 *
 * @param selectedRouteId route qui recevrait la requête, {@code null} si aucune ne correspond
 * @param simulation transformation de la requête par la route retenue, {@code null} si aucune ne correspond
 */
public record TestResult(TestRequest request, String requestUri, List<Evaluation> evaluations, String selectedRouteId,
        Simulation simulation) {

    public enum Outcome { MATCH, NO_MATCH, ERROR }

    /**
     * @param shadowed la route correspond aussi mais une route évaluée avant elle a été retenue
     */
    public record Evaluation(String routeId, int order, String predicate, Outcome outcome, String error, boolean selected,
            boolean shadowed) {
    }

    public enum StepStatus { APPLIED, RESPONSE_ONLY, NOT_SIMULATED }

    public record Step(String filter, String args, StepStatus status, String pathBefore, String pathAfter, String note) {

        public boolean changedPath() {
            return pathBefore != null && !pathBefore.equals(pathAfter);
        }
    }

    /**
     * @param uriVariables variables extraites par les prédicats ({@code Path=/users/{id}}...)
     * @param targetUrl URL appelée par la Gateway
     * @param headers en-têtes transmis au service (hors en-têtes X-Forwarded ajoutés par la Gateway)
     * @param declarative {@code false} pour une route Java DSL : les filtres ne peuvent pas être simulés
     */
    public record Simulation(Map<String, String> uriVariables, List<Step> steps, String targetUrl,
            List<Map.Entry<String, String>> headers, boolean declarative, List<String> notes) {
    }
}
