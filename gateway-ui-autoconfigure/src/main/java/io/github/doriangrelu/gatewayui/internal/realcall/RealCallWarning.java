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
package io.github.doriangrelu.gatewayui.internal.realcall;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * Avertit au démarrage que le testeur peut envoyer de vraies requêtes (ADR 0015) : toutes les méthodes sont permises,
 * y compris celles qui modifient des données.
 *
 * <p>Le message est en anglais : il s'adresse aux équipes qui exploitent la Gateway, quelle que soit la langue de l'UI.
 */
public class RealCallWarning implements SmartInitializingSingleton {

    private static final Log LOGGER = LogFactory.getLog(RealCallWarning.class);

    private final String basePath;

    /**
     * Crée l'avertissement.
     *
     * @param basePath chemin sous lequel l'UI est exposée
     */
    public RealCallWarning(final String basePath) {
        this.basePath = basePath;
    }

    @Override
    public void afterSingletonsInstantiated() {
        LOGGER.warn("Gateway UI real calls are enabled (gateway.ui.tester.real-call.enabled): anyone who can reach "
                + basePath + "/tester can send real requests through the Gateway, with any HTTP method. Protect the UI. "
                + "See https://github.com/doriangrelu/spring-cloud-gateway-ui#en-production");
    }
}
