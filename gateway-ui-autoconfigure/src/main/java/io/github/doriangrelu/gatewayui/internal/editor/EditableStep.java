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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Prédicat ou filtre en cours d'édition, ses arguments rattachés aux champs de sa fabrique.
 *
 * @param name nom de la fabrique
 * @param values valeurs des arguments, par nom de champ, dans l'ordre des champs ; une liste de la forme raccourcie
 *     ({@code GATHER_LIST}) est une seule valeur, séparée par des virgules
 */
public record EditableStep(String name, Map<String, String> values) {

    /**
     * Copie les valeurs en conservant leur ordre.
     */
    public EditableStep {
        values = values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
    }
}
