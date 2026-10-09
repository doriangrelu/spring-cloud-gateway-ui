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

import io.github.doriangrelu.gatewayui.internal.inspect.Definition;

/**
 * Effet d'un filtre de la Gateway sur la requête transmise, rejoué à partir de sa déclaration.
 */
@FunctionalInterface
interface FilterEffect {

    /**
     * Applique l'effet du filtre.
     *
     * @param definition déclaration du filtre, qui porte ses arguments
     * @param state requête en cours de transformation
     * @return une note décrivant l'effet, ou {@code null} si le changement de chemin suffit à le décrire
     */
    String apply(Definition definition, SimulationState state);
}
