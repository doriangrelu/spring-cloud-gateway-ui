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
package io.github.doriangrelu.gatewayui.internal.realcall;

import java.util.Objects;

/**
 * Route prévue par la simulation face à la route réellement empruntée (US3) : un écart révèle un filtre non simulé, une
 * IP saisie, ou une sécurité de l'application hôte qui a répondu avant la Gateway.
 *
 * @param simulatedRouteId route retenue par la simulation, {@code null} si aucune
 * @param actualRouteId route réellement empruntée, {@link CallRegistry#NO_ROUTE} si aucune, {@code null} si inconnue
 */
public record RouteComparison(String simulatedRouteId, String actualRouteId) {

    /**
     * Indique si la route réellement empruntée est connue.
     *
     * @return {@code false} si l'appel n'a pas pu être suivi
     */
    public boolean known() {
        return actualRouteId != null;
    }

    /**
     * Indique si une route a réellement traité la requête.
     *
     * @return {@code true} si une route a été retenue par la Gateway
     */
    public boolean routed() {
        return known() && !CallRegistry.NO_ROUTE.equals(actualRouteId);
    }

    /**
     * Indique si la réalité correspond à la simulation.
     *
     * @return {@code true} si les deux routes sont les mêmes, ou si aucune route n'a été retenue des deux côtés
     */
    public boolean matches() {
        return routed() ? Objects.equals(simulatedRouteId, actualRouteId) : simulatedRouteId == null;
    }
}
