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

import java.util.Map;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;

/**
 * Rendu des templates JTE précompilés de l'UI.
 *
 * <p>Le moteur est privé à l'UI (paquet dédié, aucun {@code ViewResolver} enregistré) pour ne pas interférer avec le
 * rendu de l'application hôte. Les templates sont compilés au build : aucun compilateur n'est embarqué à l'exécution.
 */
public class TemplateRenderer {

    /** Paquet des classes générées à partir des templates, distinct de celui qu'utiliserait l'application hôte. */
    static final String TEMPLATE_PACKAGE = "io.github.doriangrelu.gatewayui.internal.jte";

    private final TemplateEngine engine;

    /**
     * Charge les templates précompilés depuis le classpath.
     */
    public TemplateRenderer() {
        this.engine = TemplateEngine.createPrecompiled(null, ContentType.Html, TemplateRenderer.class.getClassLoader(),
                TEMPLATE_PACKAGE);
    }

    /**
     * Rend un template en HTML.
     *
     * @param template nom du template, sans extension (par ex. {@code routes})
     * @param params paramètres du template
     * @return le HTML produit
     */
    public String render(final String template, final Map<String, Object> params) {
        final StringOutput output = new StringOutput();
        engine.render(template + ".jte", params, output);
        return output.toString();
    }
}
