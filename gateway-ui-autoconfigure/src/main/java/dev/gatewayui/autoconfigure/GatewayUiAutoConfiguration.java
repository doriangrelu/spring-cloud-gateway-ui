package dev.gatewayui.autoconfigure;

import java.util.List;
import java.util.Map;

import dev.gatewayui.inspect.GatewayInspector;
import dev.gatewayui.inspect.ServiceCatalog;
import dev.gatewayui.tester.RouteTester;
import dev.gatewayui.web.GatewayUiHandler;
import dev.gatewayui.web.GatewayUiRouter;
import dev.gatewayui.web.TemplateRenderer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Auto-configuration de l'UI Gateway.
 * <p>
 * Rien n'est enregistré tant que {@code gateway.ui.enabled} ne vaut pas {@code true} : désactivée, l'UI n'expose
 * aucune URL et ne charge aucun bean.
 */
@AutoConfiguration(after = GatewayAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(RouteLocator.class)
@ConditionalOnProperty(prefix = GatewayUiProperties.PREFIX, name = "enabled", havingValue = "true")
@ConditionalOnBean({ RouteLocator.class, RouteDefinitionLocator.class })
@EnableConfigurationProperties(GatewayUiProperties.class)
public class GatewayUiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GatewayInspector gatewayUiInspector(RouteLocator routeLocator, RouteDefinitionLocator routeDefinitionLocator,
            List<GlobalFilter> globalFilters, Map<String, GlobalFilter> globalFilterBeans,
            ObjectProvider<GatewayProperties> gatewayProperties) {
        return new GatewayInspector(routeLocator, routeDefinitionLocator, globalFilters, globalFilterBeans,
                gatewayProperties.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceCatalog gatewayUiServiceCatalog(GatewayInspector inspector, GatewayUiProperties properties) {
        return new ServiceCatalog(inspector, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RouteTester gatewayUiRouteTester(GatewayInspector inspector) {
        return new RouteTester(inspector);
    }

    @Bean
    public RouterFunction<ServerResponse> gatewayUiRouterFunction(GatewayUiProperties properties, GatewayInspector inspector,
            ServiceCatalog serviceCatalog, RouteTester routeTester) {
        GatewayUiHandler handler = new GatewayUiHandler(properties.basePath(), new TemplateRenderer(), inspector,
                serviceCatalog, routeTester);
        return GatewayUiRouter.create(properties.basePath(), handler);
    }
}
