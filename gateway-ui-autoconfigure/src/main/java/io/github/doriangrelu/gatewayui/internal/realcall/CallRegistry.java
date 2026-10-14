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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Appels réels en cours, et la route qui a traité chacun d'eux (ADR 0015).
 *
 * <p>L'UI inscrit un identifiant aléatoire avant l'envoi et le transmet dans l'en-tête {@value #HEADER} ;
 * {@link CallCorrelationFilter} y associe la route retenue par la Gateway. Un identifiant inconnu n'est jamais inscrit :
 * un client ne peut ni remplir le registre, ni lire la route d'un autre appel. Les entrées expirent après le délai
 * maximal d'un appel, et leur nombre est borné.
 */
public class CallRegistry {

    /** En-tête qui marque une requête envoyée par le testeur ; retiré avant la sélection de route. */
    public static final String HEADER = "X-Gateway-UI-Call";

    /** Route enregistrée quand la Gateway a répondu sans qu'aucune route ne traite la requête. */
    public static final String NO_ROUTE = "";

    /** Nombre maximal d'appels suivis en même temps : au-delà, l'appel est envoyé sans corrélation. */
    static final int MAX_CALLS = 256;

    private final Duration ttl;

    private final Clock clock;

    private final Map<String, Entry> calls = new ConcurrentHashMap<>();

    /**
     * Crée le registre.
     *
     * @param ttl durée de vie d'une entrée : le délai maximal d'un appel réel
     */
    public CallRegistry(final Duration ttl) {
        this(ttl, Clock.systemUTC());
    }

    CallRegistry(final Duration ttl, final Clock clock) {
        this.ttl = ttl;
        this.clock = clock;
    }

    /**
     * Inscrit un nouvel appel.
     *
     * @return l'identifiant à transmettre, ou vide si le registre est plein
     */
    public Optional<String> register() {
        purgeExpired();
        if (calls.size() >= MAX_CALLS) {
            return Optional.empty();
        }
        final String id = UUID.randomUUID().toString();
        calls.put(id, new Entry(null, clock.instant().plus(ttl)));
        return Optional.of(id);
    }

    /**
     * Indique si l'identifiant correspond à un appel inscrit et non expiré.
     *
     * @param id identifiant reçu
     * @return {@code true} si l'appel est connu
     */
    public boolean isKnown(final String id) {
        final Entry entry = calls.get(id);
        return entry != null && !entry.expiredAt(clock.instant());
    }

    /**
     * Associe le routage d'une requête à un appel connu ; un identifiant inconnu est ignoré.
     *
     * @param id identifiant de l'appel
     * @param routing route retenue par la Gateway et URL transmise au service
     */
    public void record(final String id, final Routing routing) {
        if (isKnown(id)) {
            calls.computeIfPresent(id, (key, entry) -> new Entry(routing, entry.expiresAt()));
        }
    }

    /**
     * Retire un appel du registre.
     *
     * @param id identifiant de l'appel
     * @return le routage enregistré, ou vide s'il est inconnu
     */
    public Optional<Routing> take(final String id) {
        final Entry entry = calls.remove(id);
        return entry == null || entry.expiredAt(clock.instant()) ? Optional.empty() : Optional.ofNullable(entry.routing());
    }

    private void purgeExpired() {
        final Instant now = clock.instant();
        calls.values().removeIf(entry -> entry.expiredAt(now));
    }

    /**
     * Routage réel d'un appel, relevé par la Gateway.
     *
     * @param routeId route retenue, ou {@link #NO_ROUTE} si aucune route n'a traité la requête
     * @param targetUrl URL transmise au service, {@code null} si la requête n'a pas été transmise
     */
    public record Routing(String routeId, String targetUrl) {

        /** Aucune route n'a traité la requête. */
        public static final Routing NONE = new Routing(NO_ROUTE, null);
    }

    /**
     * Appel suivi.
     *
     * @param routing routage enregistré, {@code null} tant que la réponse n'est pas partie
     * @param expiresAt fin de validité de l'entrée
     */
    private record Entry(Routing routing, Instant expiresAt) {

        boolean expiredAt(final Instant now) {
            return now.isAfter(expiresAt);
        }
    }
}
