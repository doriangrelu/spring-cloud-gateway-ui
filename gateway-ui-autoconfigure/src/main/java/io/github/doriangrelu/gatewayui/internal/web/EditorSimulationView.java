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

import java.util.List;
import java.util.Map;

import io.github.doriangrelu.gatewayui.internal.editor.EditedRouteSimulator.Outcome;
import io.github.doriangrelu.gatewayui.internal.i18n.Message;
import io.github.doriangrelu.gatewayui.internal.tester.TestResult;
import io.github.doriangrelu.gatewayui.internal.tester.TestResult.Evaluation;
import io.github.doriangrelu.gatewayui.internal.tester.TestResult.Simulation;
import io.github.doriangrelu.gatewayui.internal.tester.TestResult.Step;

/**
 * Résultat de la simulation d'une route éditée, avec les textes traduits dans la langue de l'utilisateur.
 *
 * @param error erreur qui empêche de construire la route ou de tester la requête, {@code null} sinon
 * @param matched la route prend la requête
 * @param predicate prédicats de la route, tels qu'évalués
 * @param predicateError erreur pendant l'évaluation des prédicats, {@code null} sinon
 * @param targetUrl URL appelée par la Gateway, {@code null} si la route ne prend pas la requête
 * @param variables variables extraites par les prédicats
 * @param steps effet de chaque filtre
 * @param notes limites de la simulation
 */
public record EditorSimulationView(String error, boolean matched, String predicate, String predicateError, String targetUrl,
        Map<String, String> variables, List<StepView> steps, List<String> notes) {

    /**
     * Effet d'un filtre.
     *
     * @param filter nom du filtre
     * @param args arguments du filtre
     * @param status issue de la simulation ({@code APPLIED}, {@code RESPONSE_ONLY}, {@code NOT_SIMULATED})
     * @param pathBefore chemin avant le filtre, {@code null} s'il n'a pas été rejoué
     * @param pathAfter chemin après le filtre, {@code null} s'il n'a pas été rejoué
     * @param note effet sur les en-têtes ou raison de l'absence de simulation, traduit ; {@code null} sinon
     */
    public record StepView(String filter, String args, String status, String pathBefore, String pathAfter, String note) {
    }

    /**
     * Erreur, traduite.
     *
     * @param error erreur
     * @param ui contexte de la page, pour la langue
     * @return la vue
     */
    static EditorSimulationView failure(final Message error, final UiContext ui) {
        return new EditorSimulationView(ui.message(error), false, null, null, null, Map.of(), List.of(), List.of());
    }

    /**
     * Vue d'un résultat de simulation.
     *
     * @param outcome résultat
     * @param ui contexte de la page, pour la langue
     * @return la vue
     */
    static EditorSimulationView of(final Outcome outcome, final UiContext ui) {
        if (outcome.error() != null) {
            return failure(outcome.error(), ui);
        }
        final TestResult result = outcome.result();
        final Evaluation evaluation = result.evaluations().getFirst();
        final Simulation simulation = result.simulation();
        if (simulation == null) {
            return new EditorSimulationView(null, false, evaluation.predicate(), evaluation.error(), null, Map.of(), List.of(), List.of());
        }
        return new EditorSimulationView(null, true, evaluation.predicate(), null, simulation.targetUrl(), simulation.uriVariables(),
                simulation.steps().stream().map(step -> step(step, ui)).toList(),
                simulation.notes().stream().map(ui::message).toList());
    }

    private static StepView step(final Step step, final UiContext ui) {
        return new StepView(step.filter(), step.args(), step.status().name(), step.pathBefore(), step.pathAfter(),
                step.note() == null ? null : ui.message(step.note()));
    }
}
