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

import org.springframework.cloud.gateway.support.ShortcutConfigurable;

/**
 * Description d'une fabrique de prédicat ou de filtre, telle que la Gateway la connaît.
 *
 * @param name nom de la fabrique, utilisé dans la configuration ({@code Path}, {@code StripPrefix}...)
 * @param fields noms des arguments, dans l'ordre de la forme raccourcie
 * @param shortcutType manière dont la forme raccourcie répartit les valeurs ({@code DEFAULT}, {@code GATHER_LIST},
 *     {@code GATHER_LIST_TAIL_FLAG}...)
 */
public record FactoryDescriptor(String name, List<String> fields, String shortcutType) {

    /**
     * Copie les champs.
     */
    public FactoryDescriptor {
        fields = List.copyOf(fields);
    }

    /**
     * Décrit une fabrique.
     *
     * @param name nom de la fabrique
     * @param factory fabrique de la Gateway
     * @return la description
     */
    public static FactoryDescriptor of(final String name, final ShortcutConfigurable factory) {
        return new FactoryDescriptor(name, factory.shortcutFieldOrder(), factory.shortcutType().name());
    }

    /**
     * Fabrique inconnue de la Gateway (filtre retiré, faute de frappe) : sans champ déclaré.
     *
     * @param name nom de la fabrique
     * @return une description sans champ
     */
    public static FactoryDescriptor unknown(final String name) {
        return new FactoryDescriptor(name, List.of(), "DEFAULT");
    }

    /**
     * Indique si la forme raccourcie regroupe toutes les valeurs dans le premier argument ({@code Path=/a, /b}).
     *
     * @return {@code true} pour une liste
     */
    public boolean gathersList() {
        return shortcutType.startsWith("GATHER_LIST");
    }

    /**
     * Indique si la liste se termine par un drapeau booléen optionnel ({@code Path=/a, /b, false}).
     *
     * @return {@code true} si un drapeau peut suivre la liste
     */
    public boolean hasTailFlag() {
        return "GATHER_LIST_TAIL_FLAG".equals(shortcutType);
    }
}
