package dev.gatewayui.tester;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FilterSimulatorTest {

    @Test
    void stripPrefixRemovesLeadingSegments() {
        assertThat(FilterSimulator.stripPrefix("/api/orders/42", 1)).isEqualTo("/orders/42");
        assertThat(FilterSimulator.stripPrefix("/api/orders/42", 2)).isEqualTo("/42");
    }

    @Test
    void stripPrefixKeepsTrailingSlash() {
        assertThat(FilterSimulator.stripPrefix("/api/orders/", 1)).isEqualTo("/orders/");
    }

    @Test
    void stripPrefixOfEveryPartGivesRoot() {
        assertThat(FilterSimulator.stripPrefix("/api", 1)).isEqualTo("/");
        assertThat(FilterSimulator.stripPrefix("/api/", 3)).isEqualTo("/");
    }
}
