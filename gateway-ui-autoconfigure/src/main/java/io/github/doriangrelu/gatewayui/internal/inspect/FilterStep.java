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
package io.github.doriangrelu.gatewayui.internal.inspect;

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
 * @param name nom court du filtre ({@code StripPrefix}, {@code NettyRoutingFilter}, nom du bean pour une lambda...)
 * @param description arguments du filtre de route, type du filtre global, ou classe qui déclare une lambda
 * @param order ordre effectif dans la chaîne
 * @param ordered {@code false} quand le filtre n'a pas d'ordre explicite (il passe alors en dernier)
 * @param scope portée du filtre
 * @param lambda filtre global déclaré en lambda : {@code description} est la classe qui le déclare
 */
public record FilterStep(String name, String description, int order, boolean ordered, Scope scope, boolean lambda) {

    /** Format produit par {@code GatewayToStringStyler} : {@code [StripPrefix parts = 1]}. */
    private static final Pattern FACTORY_TO_STRING = Pattern.compile("^\\[(\\w+)\\s*(.*)]$", Pattern.DOTALL);

    /** Distance à une borne en deçà de laquelle l'ordre est affiché relativement à elle. */
    private static final long RELATIVE_ORDER_RANGE = 1_000_000L;

    /** Nom d'un filtre de route déclaré en Java, dont on ne connaît que le type (terme technique, non traduit). */
    private static final String JAVA_FILTER_NAME = "Java DSL";

    /**
     * Portée d'un filtre.
     */
    public enum Scope {
        /** Filtre global, appliqué à toutes les routes. */
        GLOBAL,
        /** Filtre déclaré sur la route ou dans les {@code default-filters}. */
        ROUTE
    }

    /**
     * Décrit un filtre global, en calculant son ordre comme {@code FilteringWebHandler} : {@link Ordered}, sinon
     * {@link Order} sur la classe, sinon dernier.
     *
     * @param filter filtre global
     * @param beanName nom du bean, affiché à la place du type pour les filtres déclarés en lambda ; peut être {@code null}
     * @return la description du filtre
     */
    public static FilterStep ofGlobal(final GlobalFilter filter, final String beanName) {
        final Class<?> type = ClassUtils.getUserClass(filter);
        final Integer order = explicitOrder(filter);
        final boolean anonymous = isAnonymous(type);
        final String name = anonymous && beanName != null ? beanName : type.getSimpleName();
        final String description = anonymous ? declaringClassName(type) : type.getName();
        return new FilterStep(name, description, order != null ? order : Ordered.LOWEST_PRECEDENCE, order != null,
                Scope.GLOBAL, anonymous);
    }

    /**
     * Décrit un filtre de route à partir de sa représentation textuelle produite par la Gateway.
     *
     * @param filter filtre de route, éventuellement enveloppé dans un {@link OrderedGatewayFilter}
     * @return la description du filtre
     */
    public static FilterStep ofRoute(final GatewayFilter filter) {
        final boolean ordered = filter instanceof Ordered;
        final int order = filter instanceof final Ordered o ? o.getOrder() : Ordered.LOWEST_PRECEDENCE;
        final GatewayFilter target = filter instanceof final OrderedGatewayFilter o ? o.getDelegate() : filter;
        final Matcher matcher = FACTORY_TO_STRING.matcher(String.valueOf(target));
        if (matcher.matches()) {
            return new FilterStep(matcher.group(1), matcher.group(2).trim(), order, ordered, Scope.ROUTE, false);
        }
        return new FilterStep(JAVA_FILTER_NAME, ClassUtils.getUserClass(target).getName(), order, ordered, Scope.ROUTE,
                false);
    }

    /**
     * Indique si le filtre est global.
     *
     * @return {@code true} pour un filtre global
     */
    public boolean global() {
        return scope == Scope.GLOBAL;
    }

    /**
     * Ordre lisible : les valeurs proches des bornes s'écrivent relativement à elles, comme dans le code de la
     * Gateway ({@code HIGHEST+1000}, {@code LOWEST-1}). Un filtre sans ordre explicite vaut {@code LOWEST}.
     *
     * @return l'ordre mis en forme
     */
    public String orderText() {
        final long fromHighest = (long) order - Ordered.HIGHEST_PRECEDENCE;
        final long fromLowest = (long) Ordered.LOWEST_PRECEDENCE - order;
        if (fromHighest < RELATIVE_ORDER_RANGE) {
            return relative("HIGHEST", "+", fromHighest);
        }
        return fromLowest < RELATIVE_ORDER_RANGE ? relative("LOWEST", "-", fromLowest) : String.valueOf(order);
    }

    private static String relative(final String bound, final String sign, final long distance) {
        return distance == 0 ? bound : bound + sign + distance;
    }

    private static Integer explicitOrder(final GlobalFilter filter) {
        if (filter instanceof final Ordered ordered) {
            return ordered.getOrder();
        }
        final Order annotation = AnnotationUtils.findAnnotation(filter.getClass(), Order.class);
        return annotation != null ? annotation.value() : null;
    }

    private static boolean isAnonymous(final Class<?> type) {
        return type.isSynthetic() || type.isAnonymousClass() || type.getSimpleName().contains("$$Lambda");
    }

    private static String declaringClassName(final Class<?> type) {
        return type.getName().replaceAll("\\$\\$Lambda.*$", "");
    }
}
