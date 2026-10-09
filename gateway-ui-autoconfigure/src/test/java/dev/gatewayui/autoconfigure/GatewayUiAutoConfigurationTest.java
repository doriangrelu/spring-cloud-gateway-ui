package dev.gatewayui.autoconfigure;

import dev.gatewayui.inspect.GatewayInspector;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayUiAutoConfigurationTest {

    private final ReactiveWebApplicationContextRunner runner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GatewayUiAutoConfiguration.class));

    @Test
    void disabledByDefault() {
        runner.run(context -> assertThat(context).doesNotHaveBean(GatewayInspector.class)
                .doesNotHaveBean(GatewayUiProperties.class));
    }

    @Test
    void disabledExplicitly() {
        runner.withPropertyValues("gateway.ui.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(GatewayInspector.class));
    }

    @Test
    void requiresAGateway() {
        runner.withPropertyValues("gateway.ui.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(GatewayInspector.class));
    }
}
