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

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * Configuration de l'UI Gateway, sous le préfixe {@code gateway.ui}.
 *
 * <pre>
 * gateway:
 *   ui:
 *     enabled: true
 *     base-path: /gateway-ui
 *     discover-services: true
 *     default-locale: en
 *     services:
 *       orders:
 *         url: http://orders.shop.svc.cluster.local:8080
 *         display-name: Commandes
 *     tester:
 *       real-call:
 *         enabled: true
 *         timeout: 5s
 *         max-body-size: 64KB
 * </pre>
 *
 * @param enabled active l'UI ; désactivée par défaut pour ne jamais l'exposer par accident
 * @param basePath préfixe sous lequel l'UI et ses ressources sont servies, {@code /gateway-ui} par défaut
 * @param discoverServices déduit les services à partir des URI des routes
 * @param services services déclarés explicitement, ou surcharges des services déduits, indexés par nom
 * @param defaultLocale langue de l'UI quand ni le sélecteur ni le navigateur n'en demandent une supportée ({@code en} ou {@code fr})
 * @param tester configuration du testeur de routes
 */
@ConfigurationProperties(GatewayUiProperties.PREFIX)
public record GatewayUiProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue(GatewayUiProperties.DEFAULT_BASE_PATH) String basePath,
        @DefaultValue("true") boolean discoverServices,
        Map<String, Service> services,
        @DefaultValue("en") Locale defaultLocale,
        @DefaultValue Tester tester) {

    /** Préfixe des propriétés de l'UI. */
    public static final String PREFIX = "gateway.ui";

    /** Préfixe par défaut des URL de l'UI. */
    public static final String DEFAULT_BASE_PATH = "/gateway-ui";

    /**
     * Normalise le chemin de base et rend la liste des services immuable en conservant l'ordre de déclaration.
     *
     * @throws IllegalArgumentException si le chemin de base est la racine, ce qui masquerait les routes de la Gateway
     */
    @ConstructorBinding
    public GatewayUiProperties {
        basePath = normalizeBasePath(basePath);
        services = services == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(services));
        defaultLocale = defaultLocale == null ? Locale.ENGLISH : defaultLocale;
        tester = tester == null ? new Tester(null) : tester;
    }

    /**
     * Constructeur d'avant la configuration du testeur, conservé pour la compatibilité (ADR 0008) : le testeur reçoit
     * sa configuration par défaut.
     *
     * @param enabled active l'UI
     * @param basePath préfixe des URL de l'UI
     * @param discoverServices déduit les services à partir des URI des routes
     * @param services services déclarés explicitement
     * @param defaultLocale langue par défaut de l'UI
     * @deprecated depuis 1.2.0, utiliser le constructeur qui prend aussi la configuration du testeur
     */
    @Deprecated(since = "1.2.0")
    public GatewayUiProperties(final boolean enabled, final String basePath, final boolean discoverServices,
            final Map<String, Service> services, final Locale defaultLocale) {
        this(enabled, basePath, discoverServices, services, defaultLocale, null);
    }

    /**
     * Configuration du testeur de routes, sous {@code gateway.ui.tester}.
     *
     * @param realCall mode « appel réel » (ADR 0015)
     */
    public record Tester(@DefaultValue RealCall realCall) {

        /**
         * Remplace une configuration absente par celle par défaut.
         */
        public Tester {
            realCall = realCall == null ? new RealCall(false, null, null) : realCall;
        }
    }

    /**
     * Mode « appel réel » du testeur, sous {@code gateway.ui.tester.real-call} (ADR 0015) : la requête saisie est
     * réellement envoyée à la Gateway.
     *
     * @param enabled active le mode ; désactivé par défaut, l'UI n'émet alors aucune requête
     * @param timeout délai maximal d'un appel, {@code 5s} par défaut
     * @param maxBodySize taille maximale de l'extrait du corps de réponse affiché, {@code 64KB} par défaut
     */
    public record RealCall(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("5s") Duration timeout,
            @DefaultValue("64KB") DataSize maxBodySize) {

        /** Délai maximal par défaut d'un appel réel. */
        public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

        /** Taille maximale par défaut de l'extrait du corps de réponse. */
        public static final DataSize DEFAULT_MAX_BODY_SIZE = DataSize.ofKilobytes(64);

        /**
         * Remplace les valeurs absentes ou non strictement positives par les valeurs par défaut.
         */
        public RealCall {
            timeout = timeout == null || timeout.isNegative() || timeout.isZero() ? DEFAULT_TIMEOUT : timeout;
            maxBodySize = maxBodySize == null || maxBodySize.toBytes() <= 0 ? DEFAULT_MAX_BODY_SIZE : maxBodySize;
        }
    }

    /**
     * Service cible de la Gateway, déclaré dans {@code gateway.ui.services}.
     *
     * @param url URL de base du service, comparée aux URI des routes (seuls schéma, hôte et port comptent)
     * @param displayName libellé affiché dans l'UI, le nom du service à défaut
     * @param routeIds routes rattachées explicitement au service, en plus de celles qui pointent vers son URL
     */
    public record Service(URI url, String displayName, List<String> routeIds) {

        /**
         * Remplace une liste de routes absente par une liste vide.
         */
        public Service {
            routeIds = routeIds == null ? List.of() : List.copyOf(routeIds);
        }
    }

    private static String normalizeBasePath(final String path) {
        final String normalized = stripTrailingSlashes(withLeadingSlash(path));
        if ("/".equals(normalized)) {
            throw new IllegalArgumentException(PREFIX + ".base-path ne peut pas être la racine : l'UI masquerait les routes de la Gateway");
        }
        return normalized;
    }

    private static String withLeadingSlash(final String path) {
        final String trimmed = path == null || path.isBlank() ? DEFAULT_BASE_PATH : path.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    private static String stripTrailingSlashes(final String path) {
        String result = path;
        while (result.length() > 1 && result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
