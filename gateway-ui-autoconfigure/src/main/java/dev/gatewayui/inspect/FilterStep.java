package dev.gatewayui.inspect;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.util.ClassUtils;

/**
 * Un filtre de la chaîne d'exécution d'une route, global ou propre à la route.
 *
 * @param ordered {@code false} quand le filtre n'a pas d'ordre explicite (il passe alors en dernier)
 */
public record FilterStep(String name, String description, int order, boolean ordered, Scope scope) {

    public enum Scope { GLOBAL, ROUTE }

    /** Format produit par {@code GatewayToStringStyler} : {@code [StripPrefix parts = 1]}. */
    private static final Pattern FACTORY_TO_STRING = Pattern.compile("^\\[(\\w+)\\s*(.*)]$", Pattern.DOTALL);

    /**
     * @param beanName nom du bean, affiché à la place du type pour les filtres déclarés en lambda
     */
    static FilterStep ofGlobal(GlobalFilter filter, String beanName) {
        Class<?> type = ClassUtils.getUserClass(filter);
        boolean anonymous = type.isSynthetic() || type.isAnonymousClass() || type.getSimpleName().contains("$$Lambda");
        String name = anonymous && beanName != null ? beanName : type.getSimpleName();
        String description = anonymous ? "Lambda déclarée dans " + type.getName().replaceAll("\\$\\$Lambda.*$", "") : type.getName();
        Integer order = null;
        if (filter instanceof Ordered ordered) {
            order = ordered.getOrder();
        }
        else {
            Order annotation = AnnotationUtils.findAnnotation(filter.getClass(), Order.class);
            if (annotation != null) {
                order = annotation.value();
            }
        }
        return new FilterStep(name, description, order != null ? order : Ordered.LOWEST_PRECEDENCE, order != null,
                Scope.GLOBAL);
    }

    public static FilterStep ofRoute(GatewayFilter filter) {
        boolean ordered = filter instanceof Ordered;
        int order = filter instanceof Ordered o ? o.getOrder() : Ordered.LOWEST_PRECEDENCE;
        GatewayFilter target = filter instanceof OrderedGatewayFilter o ? o.getDelegate() : filter;
        Matcher matcher = FACTORY_TO_STRING.matcher(String.valueOf(target));
        if (matcher.matches()) {
            return new FilterStep(matcher.group(1), matcher.group(2).trim(), order, ordered, Scope.ROUTE);
        }
        // Filtre déclaré en Java (lambda, classe anonyme) : on n'a que son type
        return new FilterStep("Filtre Java", ClassUtils.getUserClass(target).getName(), order, ordered, Scope.ROUTE);
    }

    public boolean global() {
        return scope == Scope.GLOBAL;
    }

    /** Infobulle de l'ordre : un filtre sans ordre explicite est souvent une erreur de configuration. */
    public String orderHint() {
        return ordered ? "Ordre" : "Ni Ordered ni @Order sur la classe : exécuté en dernier. "
                + "Un @Order posé sur la méthode @Bean est ignoré par la Gateway.";
    }

    public String orderText() {
        if (!ordered) {
            return "non ordonné";
        }
        // Les ordres proches des bornes s'écrivent relativement à elles, comme dans le code de la Gateway
        long fromHighest = (long) order - Ordered.HIGHEST_PRECEDENCE;
        long fromLowest = (long) Ordered.LOWEST_PRECEDENCE - order;
        if (fromHighest < 1_000_000) {
            return fromHighest == 0 ? "HIGHEST" : "HIGHEST+" + fromHighest;
        }
        if (fromLowest < 1_000_000) {
            return fromLowest == 0 ? "LOWEST" : "LOWEST-" + fromLowest;
        }
        return String.valueOf(order);
    }
}
