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
package io.github.doriangrelu.gatewayui.autoconfigure;

import java.util.List;
import java.util.Map;

import io.github.doriangrelu.gatewayui.internal.editor.EditedRouteSimulator;
import io.github.doriangrelu.gatewayui.internal.editor.EditedRouteSimulator.RouteDefinitionRouteLocatorFactory;
import io.github.doriangrelu.gatewayui.internal.editor.EditorService;
import io.github.doriangrelu.gatewayui.internal.editor.FactoryCatalog;
import io.github.doriangrelu.gatewayui.internal.editor.RawRouteConfiguration;
import io.github.doriangrelu.gatewayui.internal.inspect.GatewayInspector;
import io.github.doriangrelu.gatewayui.internal.inspect.ServiceCatalog;
import io.github.doriangrelu.gatewayui.internal.tester.RouteTester;
import io.github.doriangrelu.gatewayui.internal.web.EditorApiHandler;
import io.github.doriangrelu.gatewayui.internal.web.GatewayUiHandler;
import io.github.doriangrelu.gatewayui.internal.web.GatewayUiRouter;
import io.github.doriangrelu.gatewayui.internal.web.TemplateRenderer;
import io.github.doriangrelu.gatewayui.internal.web.UiContexts;
import io.github.doriangrelu.gatewayui.internal.web.UnprotectedUiWarning;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Auto-configuration de l'UI Gateway.
 *
 * <p>Rien n'est enregistré tant que {@code gateway.ui.enabled} ne vaut pas {@code true} : désactivée, l'UI n'expose
 * aucune URL et ne charge aucun bean. Elle n'est active que dans une application réactive qui embarque Spring Cloud
 * Gateway Server WebFlux.
 *
 * <p>Les beans déclarés ici sont des détails d'implémentation (paquets {@code internal}) : ils ne font pas partie de l'API
 * publique et ne sont pas prévus pour être remplacés (ADR 0008).
 */
@AutoConfiguration(after = GatewayAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass({ RouteLocator.class, RouterFunction.class })
@ConditionalOnProperty(prefix = GatewayUiProperties.PREFIX, name = "enabled", havingValue = "true")
@ConditionalOnBean({ RouteLocator.class, RouteDefinitionLocator.class })
@EnableConfigurationProperties(GatewayUiProperties.class)
public class GatewayUiAutoConfiguration {

    /**
     * Lecture de la configuration effective de la Gateway.
     *
     * @param routeLocator routes effectives (le locator principal, mis en cache par la Gateway)
     * @param routeDefinitionLocator définitions des routes déclaratives
     * @param globalFilters filtres globaux, dans l'ordre où la Gateway les reçoit
     * @param globalFilterBeans filtres globaux indexés par nom de bean, pour nommer les lambdas
     * @param gatewayProperties configuration de la Gateway, pour les {@code default-filters}
     * @return l'inspecteur de la Gateway
     */
    @Bean
    public GatewayInspector gatewayUiInspector(final RouteLocator routeLocator,
            final RouteDefinitionLocator routeDefinitionLocator, final List<GlobalFilter> globalFilters,
            final Map<String, GlobalFilter> globalFilterBeans, final ObjectProvider<GatewayProperties> gatewayProperties) {
        return new GatewayInspector(routeLocator, routeDefinitionLocator, globalFilters, globalFilterBeans,
                gatewayProperties.getIfAvailable());
    }

    /**
     * Regroupement des routes par service cible.
     *
     * @param inspector inspecteur de la Gateway
     * @param properties configuration de l'UI
     * @return le catalogue des services
     */
    @Bean
    public ServiceCatalog gatewayUiServiceCatalog(final GatewayInspector inspector, final GatewayUiProperties properties) {
        return new ServiceCatalog(inspector, properties);
    }

    /**
     * Fabriques de prédicats et de filtres de la Gateway, pour l'éditeur.
     *
     * @param predicates fabriques de prédicats
     * @param filters fabriques de filtres
     * @return le catalogue des fabriques
     */
    @Bean
    public FactoryCatalog gatewayUiFactoryCatalog(final List<RoutePredicateFactory<?>> predicates,
            final List<GatewayFilterFactory<?>> filters) {
        return new FactoryCatalog(predicates, filters);
    }

    /**
     * Service de l'éditeur graphique.
     *
     * @param inspector inspecteur de la Gateway
     * @param factoryCatalog fabriques de la Gateway
     * @param environment environnement, pour les valeurs brutes de la configuration des routes
     * @param simulator simulateur de la route éditée
     * @return le service de l'éditeur
     */
    @Bean
    public EditorService gatewayUiEditorService(final GatewayInspector inspector, final FactoryCatalog factoryCatalog,
            final ConfigurableEnvironment environment, final EditedRouteSimulator simulator) {
        return new EditorService(inspector, factoryCatalog, RawRouteConfiguration.of(environment), simulator);
    }

    /**
     * Constructeur de routes de la Gateway, pour la route éditée : mêmes fabriques, mêmes {@code default-filters}.
     *
     * @param predicates fabriques de prédicats
     * @param filters fabriques de filtres
     * @param gatewayProperties configuration de la Gateway
     * @param configurationService liaison des arguments de la Gateway
     * @return la fabrique de constructeurs de routes
     */
    @Bean
    public RouteDefinitionRouteLocatorFactory gatewayUiRouteLocatorFactory(final List<RoutePredicateFactory<?>> predicates,
            final List<GatewayFilterFactory<?>> filters, final ObjectProvider<GatewayProperties> gatewayProperties,
            final ConfigurationService configurationService) {
        return RouteDefinitionRouteLocatorFactory.of(predicates, filters, gatewayProperties.getIfAvailable(), configurationService);
    }

    /**
     * Simulateur de la route éditée.
     *
     * @param factoryCatalog fabriques de la Gateway
     * @param routeTester testeur de routes
     * @param locators constructeur de routes de la Gateway
     * @param environment environnement, pour résoudre les placeholders
     * @return le simulateur
     */
    @Bean
    public EditedRouteSimulator gatewayUiEditedRouteSimulator(final FactoryCatalog factoryCatalog, final RouteTester routeTester,
            final RouteDefinitionRouteLocatorFactory locators, final ConfigurableEnvironment environment) {
        return new EditedRouteSimulator(factoryCatalog, routeTester, locators, environment::resolvePlaceholders);
    }

    /**
     * Testeur de routes.
     *
     * @param inspector inspecteur de la Gateway
     * @return le testeur de routes
     */
    @Bean
    public RouteTester gatewayUiRouteTester(final GatewayInspector inspector) {
        return new RouteTester(inspector);
    }

    /**
     * Routes HTTP des pages et ressources de l'UI.
     *
     * @param properties configuration de l'UI
     * @param inspector inspecteur de la Gateway
     * @param serviceCatalog catalogue des services
     * @param routeTester testeur de routes
     * @param editorService service de l'éditeur
     * @return la fonction de routage de l'UI
     */
    @Bean
    public RouterFunction<ServerResponse> gatewayUiRouterFunction(final GatewayUiProperties properties,
            final GatewayInspector inspector, final ServiceCatalog serviceCatalog, final RouteTester routeTester,
            final EditorService editorService) {
        final UiContexts contexts = new UiContexts(properties.basePath(), properties.defaultLocale());
        final GatewayUiHandler handler = new GatewayUiHandler(contexts, new TemplateRenderer(), inspector, serviceCatalog,
                routeTester);
        return GatewayUiRouter.create(properties.basePath(), handler, new EditorApiHandler(editorService, contexts));
    }

    /**
     * Avertissement au démarrage quand l'UI est exposée sans Spring Security (ADR 0010).
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.security.web.server.SecurityWebFilterChain")
    static class UnprotectedUiWarningConfiguration {

        @Bean
        UnprotectedUiWarning gatewayUiUnprotectedWarning(final GatewayUiProperties properties) {
            return new UnprotectedUiWarning(properties.basePath());
        }
    }
}
