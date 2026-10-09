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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * Avertit au démarrage que l'UI est exposée sans protection, quand Spring Security est absent (ADR 0010).
 *
 * <p>Le message est en anglais : il s'adresse aux équipes qui exploitent la Gateway, quelle que soit la langue de l'UI.
 */
public class UnprotectedUiWarning implements SmartInitializingSingleton {

    private static final Log LOGGER = LogFactory.getLog(UnprotectedUiWarning.class);

    private final String basePath;

    /**
     * Crée l'avertissement.
     *
     * @param basePath chemin sous lequel l'UI est exposée
     */
    public UnprotectedUiWarning(final String basePath) {
        this.basePath = basePath;
    }

    @Override
    public void afterSingletonsInstantiated() {
        LOGGER.warn("Gateway UI is exposed without protection on " + basePath + "/** (Spring Security is not on the classpath). "
                + "It shows the internal topology of the Gateway: protect it, or enable it only outside production "
                + "(gateway.ui.enabled). See https://github.com/doriangrelu/spring-cloud-gateway-ui#en-production");
    }
}
