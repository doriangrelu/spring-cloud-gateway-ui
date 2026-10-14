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

import io.github.doriangrelu.gatewayui.internal.editor.Catalog;
import io.github.doriangrelu.gatewayui.internal.editor.EditedRouteSimulator;
import io.github.doriangrelu.gatewayui.internal.editor.EditedRouteSimulator.RouteDefinitionRouteLocatorFactory;
import io.github.doriangrelu.gatewayui.internal.editor.EditorService;
import io.github.doriangrelu.gatewayui.internal.editor.FactoryCatalog;
import io.github.doriangrelu.gatewayui.internal.editor.RawRouteConfiguration;
import io.github.doriangrelu.gatewayui.internal.editor.RouteAdvisor;
import io.github.doriangrelu.gatewayui.internal.inspect.GatewayInspector;
import io.github.doriangrelu.gatewayui.internal.inspect.ServiceCatalog;
import io.github.doriangrelu.gatewayui.internal.realcall.CallCorrelationFilter;
import io.github.doriangrelu.gatewayui.internal.realcall.CallRegistry;
import io.github.doriangrelu.gatewayui.internal.realcall.LocalServerAddress;
import io.github.doriangrelu.gatewayui.internal.realcall.RealCallWarning;
import io.github.doriangrelu.gatewayui.internal.realcall.RealCaller;
import io.github.doriangrelu.gatewayui.internal.tester.RouteTester;
import io.github.doriangrelu.gatewayui.internal.web.EditorApiHandler;
import io.github.doriangrelu.gatewayui.internal.web.GatewayUiHandler;
import io.github.doriangrelu.gatewayui.internal.web.GatewayUiRouter;
import io.github.doriangrelu.gatewayui.internal.web.TemplateRenderer;
import io.github.doriangrelu.gatewayui.internal.web.TesterHandler;
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
import org.springframework.core.env.Environment;
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
     * Catalogue documenté des prédicats et filtres de la Gateway (ADR 0013).
     *
     * @param predicates fabriques de prédicats
     * @param filters fabriques de filtres
     * @return le catalogue
     */
    @Bean
    public Catalog gatewayUiCatalog(final List<RoutePredicateFactory<?>> predicates, final List<GatewayFilterFactory<?>> filters) {
        return new Catalog(predicates, filters);
    }

    /**
     * Service de l'éditeur graphique.
     *
     * @param inspector inspecteur de la Gateway
     * @param factoryCatalog fabriques de la Gateway
     * @param environment environnement, pour les valeurs brutes de la configuration des routes et les placeholders
     * @param simulator simulateur de la route éditée
     * @return le service de l'éditeur
     */
    @Bean
    public EditorService gatewayUiEditorService(final GatewayInspector inspector, final FactoryCatalog factoryCatalog,
            final ConfigurableEnvironment environment, final EditedRouteSimulator simulator) {
        return new EditorService(inspector, factoryCatalog, RawRouteConfiguration.of(environment), simulator,
                new RouteAdvisor(factoryCatalog, environment::resolvePlaceholders));
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
     * Contextes de rendu des pages : chemin de l'UI et langue.
     *
     * @param properties configuration de l'UI
     * @return la fabrique des contextes de rendu
     */
    @Bean
    public UiContexts gatewayUiContexts(final GatewayUiProperties properties) {
        return new UiContexts(properties.basePath(), properties.defaultLocale());
    }

    /**
     * Moteur des templates précompilés de l'UI ; ce n'est pas un {@code ViewResolver} et il n'interfère pas avec
     * l'application hôte.
     *
     * @return le moteur de templates
     */
    @Bean
    public TemplateRenderer gatewayUiTemplateRenderer() {
        return new TemplateRenderer();
    }

    /**
     * Pages de consultation : routes, services, filtres globaux, éditeur, catalogue.
     *
     * @param contexts contextes de rendu
     * @param renderer moteur de templates
     * @param inspector inspecteur de la Gateway
     * @param serviceCatalog catalogue des services
     * @param catalog catalogue des prédicats et filtres
     * @return le handler des pages
     */
    @Bean
    public GatewayUiHandler gatewayUiHandler(final UiContexts contexts, final TemplateRenderer renderer,
            final GatewayInspector inspector, final ServiceCatalog serviceCatalog, final Catalog catalog) {
        return new GatewayUiHandler(contexts, renderer, inspector, serviceCatalog, catalog);
    }

    /**
     * Page du testeur, simulation et appel réel.
     *
     * @param contexts contextes de rendu
     * @param renderer moteur de templates
     * @param routeTester testeur de routes
     * @param realCaller émetteur des appels réels, absent quand le mode est désactivé
     * @return le handler du testeur
     */
    @Bean
    public TesterHandler gatewayUiTesterHandler(final UiContexts contexts, final TemplateRenderer renderer,
            final RouteTester routeTester, final ObjectProvider<RealCaller> realCaller) {
        return new TesterHandler(contexts, renderer, routeTester, realCaller.getIfAvailable());
    }

    /**
     * API de l'éditeur graphique.
     *
     * @param editorService service de l'éditeur
     * @param contexts contextes de rendu
     * @param catalog catalogue des prédicats et filtres
     * @param renderer moteur de templates
     * @return le handler de l'API
     */
    @Bean
    public EditorApiHandler gatewayUiEditorApiHandler(final EditorService editorService, final UiContexts contexts,
            final Catalog catalog, final TemplateRenderer renderer) {
        return new EditorApiHandler(editorService, contexts, catalog, renderer);
    }

    /**
     * Routes HTTP des pages et ressources de l'UI.
     *
     * @param properties configuration de l'UI
     * @param handler handler des pages
     * @param tester handler du testeur
     * @param editorApi API de l'éditeur
     * @return la fonction de routage de l'UI
     */
    @Bean
    public RouterFunction<ServerResponse> gatewayUiRouterFunction(final GatewayUiProperties properties,
            final GatewayUiHandler handler, final TesterHandler tester, final EditorApiHandler editorApi) {
        return GatewayUiRouter.create(properties.basePath(), handler, tester, editorApi);
    }

    /**
     * Mode « appel réel » du testeur (ADR 0015) : ses beans n'existent que si l'exploitant l'a activé.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = GatewayUiProperties.PREFIX + ".tester.real-call", name = "enabled", havingValue = "true")
    static class RealCallConfiguration {

        @Bean
        LocalServerAddress gatewayUiLocalServerAddress(final Environment environment) {
            return new LocalServerAddress(environment);
        }

        @Bean
        CallRegistry gatewayUiCallRegistry(final GatewayUiProperties properties) {
            return new CallRegistry(properties.tester().realCall().timeout());
        }

        @Bean
        CallCorrelationFilter gatewayUiCallCorrelationFilter(final CallRegistry registry) {
            return new CallCorrelationFilter(registry);
        }

        @Bean
        RealCaller gatewayUiRealCaller(final LocalServerAddress address, final GatewayUiProperties properties,
                final CallRegistry registry) {
            return new RealCaller(address, properties.basePath(), properties.tester().realCall(), registry);
        }

        @Bean
        RealCallWarning gatewayUiRealCallWarning(final GatewayUiProperties properties) {
            return new RealCallWarning(properties.basePath());
        }
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
