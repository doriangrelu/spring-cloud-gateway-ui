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

import io.github.doriangrelu.gatewayui.internal.editor.Advice;

/**
 * Conseil sur la route éditée, avec le texte traduit dans la langue de l'utilisateur.
 *
 * @param level gravité : {@code ERROR} ou {@code WARNING}
 * @param target élément concerné ({@code id}, {@code uri}, {@code predicates[0].regexp}, {@code filters[1].value}...)
 * @param message texte du conseil, traduit
 */
public record EditorAdviceView(String level, String target, String message) {

    /**
     * Vue d'un conseil.
     *
     * @param advice conseil
     * @param ui contexte de la page, pour la langue
     * @return la vue
     */
    static EditorAdviceView of(final Advice advice, final UiContext ui) {
        return new EditorAdviceView(advice.level().name(), advice.target(), ui.message(advice.message()));
    }
}
