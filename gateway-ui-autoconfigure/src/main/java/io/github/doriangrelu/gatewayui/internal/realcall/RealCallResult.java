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

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.github.doriangrelu.gatewayui.internal.i18n.Message;
import org.springframework.http.HttpStatus;

/**
 * Issue d'un appel réel : la réponse obtenue, ou l'erreur qui l'a empêchée.
 *
 * @param error motif de l'échec (délai dépassé, service injoignable, refus), {@code null} si une réponse a été reçue
 * @param status statut HTTP de la réponse, {@code 0} en cas d'échec
 * @param headers en-têtes de la réponse, valeurs sensibles masquées
 * @param body extrait du corps de la réponse, {@code null} en cas d'échec
 * @param duration durée de l'appel
 * @param routing route qui a réellement traité la requête et URL transmise au service, {@code null} si inconnus
 */
public record RealCallResult(Message error, int status, List<Map.Entry<String, String>> headers, BodyExcerpt body,
        Duration duration, CallRegistry.Routing routing) {

    /**
     * Rend la liste des en-têtes immuable.
     */
    public RealCallResult {
        headers = headers == null ? List.of() : List.copyOf(headers);
    }

    /**
     * Appel qui n'a pas abouti à une réponse.
     *
     * @param error motif de l'échec
     * @param duration durée écoulée avant l'échec
     * @return le résultat
     */
    public static RealCallResult failure(final Message error, final Duration duration) {
        return new RealCallResult(error, 0, List.of(), null, duration, null);
    }

    /**
     * Indique si l'appel a échoué sans réponse.
     *
     * @return {@code true} si aucune réponse n'a été reçue
     */
    public boolean failed() {
        return error != null;
    }

    /**
     * Libellé du statut HTTP.
     *
     * @return le libellé standard, ou une chaîne vide pour un statut non standard
     */
    public String reason() {
        final HttpStatus resolved = HttpStatus.resolve(status);
        return resolved == null ? "" : resolved.getReasonPhrase();
    }

    /**
     * Indique si la réponse refuse l'accès : l'appel réel arrive sans la session de l'utilisateur de l'UI.
     *
     * @return {@code true} pour un statut 401 ou 403
     */
    public boolean unauthorized() {
        return status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value();
    }

    /**
     * Durée de l'appel, en millisecondes.
     *
     * @return la durée arrondie à la milliseconde
     */
    public long durationMillis() {
        return duration.toMillis();
    }

    /**
     * Route qui a réellement traité la requête.
     *
     * @return l'identifiant de la route, {@link CallRegistry#NO_ROUTE} si aucune, {@code null} si inconnue
     */
    public String routeId() {
        return routing == null ? null : routing.routeId();
    }

    /**
     * URL à laquelle la Gateway a transmis la requête.
     *
     * @return l'URL, ou {@code null} si elle est inconnue ou si la requête n'a pas été transmise
     */
    public String targetUrl() {
        return routing == null ? null : routing.targetUrl();
    }

    RealCallResult withRouting(final CallRegistry.Routing actualRouting) {
        return new RealCallResult(error, status, headers, body, duration, actualRouting);
    }
}
