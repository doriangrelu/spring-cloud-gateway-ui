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

import java.util.Optional;

import io.github.doriangrelu.gatewayui.internal.i18n.Message;
import io.github.doriangrelu.gatewayui.internal.tester.TestRequest;

/**
 * Requête du testeur, avant un appel réel.
 *
 * @param request requête saisie dans le testeur
 * @param basePath préfixe des URL de l'UI
 */
record RealCallRequest(TestRequest request, String basePath) {

    /**
     * Vérifie que la requête peut être envoyée. Les URL de l'UI sont refusées : un appel vers {@code /tester/call}
     * relancerait un appel réel, en boucle, et l'UI n'est pas une route de la Gateway.
     *
     * @return le motif du refus, ou {@code null} si la requête peut être envoyée
     */
    Message validate() {
        final Message error = request.validate();
        if (error != null) {
            return error;
        }
        return targetsUi() ? Message.of("tester.call.error.ui", basePath) : null;
    }

    /** Corps à envoyer : seulement pour une méthode qui en porte un, et s'il a été saisi. */
    Optional<String> body() {
        return request.carriesBody() && !request.body().isEmpty() ? Optional.of(request.body()) : Optional.empty();
    }

    private boolean targetsUi() {
        final String path = request.uri().getPath();
        return path.equals(basePath) || path.startsWith(basePath + "/");
    }
}
