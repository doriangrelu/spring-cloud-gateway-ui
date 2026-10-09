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

/**
 * Route en cours d'édition (ADR 0012).
 *
 * @param id identifiant de la route
 * @param uri URI cible
 * @param order ordre d'évaluation
 * @param predicates prédicats, dans l'ordre de déclaration
 * @param filters filtres, dans l'ordre de déclaration
 */
public record EditableRoute(String id, String uri, int order, List<EditableStep> predicates, List<EditableStep> filters) {

    /**
     * Copie les listes, absentes comprises.
     */
    public EditableRoute {
        id = id == null ? "" : id;
        uri = uri == null ? "" : uri;
        predicates = predicates == null ? List.of() : List.copyOf(predicates);
        filters = filters == null ? List.of() : List.copyOf(filters);
    }
}
