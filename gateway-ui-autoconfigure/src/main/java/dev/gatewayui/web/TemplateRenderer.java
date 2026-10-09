package dev.gatewayui.web;

import java.util.Map;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;

/**
 * Rendu des templates JTE précompilés de l'UI.
 * <p>
 * Le moteur est privé à l'UI (paquet dédié, aucun {@code ViewResolver} enregistré) pour ne pas interférer avec
 * le rendu de l'application hôte.
 */
public class TemplateRenderer {

    static final String TEMPLATE_PACKAGE = "dev.gatewayui.jte";

    private final TemplateEngine engine;

    public TemplateRenderer() {
        this.engine = TemplateEngine.createPrecompiled(null, ContentType.Html, TemplateRenderer.class.getClassLoader(),
                TEMPLATE_PACKAGE);
    }

    public String render(String template, Map<String, Object> params) {
        StringOutput output = new StringOutput();
        engine.render(template + ".jte", params, output);
        return output.toString();
    }
}
