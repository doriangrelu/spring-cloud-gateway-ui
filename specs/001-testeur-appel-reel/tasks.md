---

description: "Liste des tâches du mode « appel réel » du testeur"
---

# Tasks: Mode « appel réel » du testeur

**Input**: Design documents from `/specs/001-testeur-appel-reel/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: obligatoires (constitution, principe IV) : tests unitaires pour `internal.realcall` et
`TestRequest`, tests d'intégration pour l'URL et la page.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

**✅ Préalable levé** : ADR 0015 accepté le 2026-10-13.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- `main/` = `gateway-ui-autoconfigure/src/main/java/io/github/doriangrelu/gatewayui/`
- `test/` = `gateway-ui-autoconfigure/src/test/java/io/github/doriangrelu/gatewayui/`
- `jte/` = `gateway-ui-autoconfigure/src/main/jte/`
- `i18n/` = `gateway-ui-autoconfigure/src/main/resources/io/github/doriangrelu/gatewayui/internal/i18n/`
- Toute clé de message est ajoutée dans `messages_en.properties` **et** `messages_fr.properties`
  (apostrophe doublée `''` dans un message avec arguments).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Créer le paquet de l'appel réel

- [X] T001 Créer le paquet `internal.realcall` avec sa Javadoc (rôle : envoyer une requête de test à la Gateway elle-même, ADR 0015 ; seul paquet hors `web` autorisé à faire du HTTP) dans `main/internal/realcall/package-info.java`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Configuration, saisie du corps et URL d'appel toujours déclarée (garde-fou R6)

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T002 Ajouter à `GatewayUiProperties` le composant `Tester tester` et les records imbriqués `Tester(RealCall realCall)` et `Tester.RealCall(boolean enabled, Duration timeout, DataSize maxBodySize)` avec `@DefaultValue` : `enabled` = `false`, `timeout` = `5s` (« Strictement positif »), `maxBodySize` = `64KB` (« Strictement positif ; borne l'extrait affiché ») ; valeurs absentes ou non positives remplacées par les défauts dans le constructeur compact ; Javadoc `@param` et exemple YAML de la classe mis à jour, dans `main/autoconfigure/GatewayUiProperties.java`
- [X] T003 Conserver l'ancien constructeur à 5 arguments de `GatewayUiProperties`, `@Deprecated(since = "1.2.0")`, qui délègue avec `Tester` par défaut, et annoter le constructeur canonique `@ConstructorBinding` (R10, ADR 0008) dans `main/autoconfigure/GatewayUiProperties.java`
- [X] T004 [P] Tester la liaison des propriétés : défauts, valeurs personnalisées, valeurs invalides remplacées, ancien constructeur, dans `test/autoconfigure/GatewayUiPropertiesTest.java`
- [X] T005 [P] Ajouter le champ `body` à `TestRequest` (« Texte libre, vide par défaut. Ignoré par la simulation. Envoyé seulement pour POST, PUT, PATCH, DELETE »), lu par `from(params)` depuis le paramètre `body`, et une méthode `carriesBody()` ; adapter les appels existants du constructeur (`RouteTester`, `EditedRouteSimulator`, tests) ; `GatewayUiEditorIntegrationTest` doit rester vert sans changement de comportement de l'éditeur, dans `main/internal/tester/TestRequest.java`
- [X] T006 [P] Tester `body` : défaut vide, `carriesBody()` par méthode, simulation inchangée avec un corps saisi (SC-004), dans `test/internal/tester/TestRequestTest.java`
- [X] T007 Déclarer `POST {basePath}/tester/call` dans `GatewayUiRouter.pages`, **toujours**, vers `GatewayUiHandler::testerCall` ; dans le handler, répondre 404 avec la page `notFound` quand le mode est désactivé, sans rien appeler (R6), dans `main/internal/web/GatewayUiRouter.java` et `main/internal/web/GatewayUiHandler.java`
- [X] T008 Transmettre `GatewayUiProperties.tester().realCall()` au `GatewayUiHandler` et exposer à la page un drapeau `realCallEnabled`, dans `main/autoconfigure/GatewayUiAutoConfiguration.java` et `main/internal/web/GatewayUiHandler.java`

**Checkpoint**: configuration en place, URL protégée contre la capture par une route `Path=/**`

---

## Phase 3: User Story 1 - Envoyer réellement la requête testée (Priority: P1) 🎯 MVP

**Goal**: avec le mode activé, un clic envoie la requête saisie à la Gateway et affiche la réponse
réelle à côté de la simulation.

**Independent Test**: mode activé, `GET` sur une route vers le service de test → statut, en-têtes,
corps et durée affichés (quickstart, scénarios 2 et 3).

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T009 [P] [US1] Tests de `SensitiveHeaders` : `Authorization`, `Proxy-Authorization`, `Cookie`, `Set-Cookie` et noms contenant `token`, `secret`, `password`, `api-key`, `credential` masqués, insensibles à la casse ; autres en-têtes intacts, dans `test/internal/realcall/SensitiveHeadersTest.java`
- [X] T010 [P] [US1] Tests de `BodyExcerptReader` : corps texte complet, corps tronqué à `maxBodySize` avec `truncated`, contenu binaire non affiché, charset déclaré et UTF-8 par défaut, tampons libérés, dans `test/internal/realcall/BodyExcerptReaderTest.java`
- [X] T011 [P] [US1] Tests de `RealCallRequest` : refus d'un chemin sous `base-path` (R5), refus d'une requête invalide, corps envoyé seulement pour POST, PUT, PATCH, DELETE, dans `test/internal/realcall/RealCallRequestTest.java`
- [X] T012 [P] [US1] Test d'intégration, mode activé : service de test servi par une `RouterFunction` de l'application de test et routes de la Gateway qui y mènent (R12) ; `POST /gateway-ui/tester/call` en `GET` et en `POST` avec corps → statut, en-têtes, corps et durée du service ; `Authorization` masqué ; service lent avec `timeout=1s` → « délai dépassé » ; route vers un port fermé → « service injoignable » ; aucune route ne correspond → réponse 404 de la Gateway affichée telle quelle ; route protégée renvoyant 401 → aide `tester.call.hint.unauthorized` affichée ; journal capturé (`OutputCaptureExtension`) sans la valeur d'`Authorization` ; chemin `/gateway-ui/tester` → refus sans envoi ; avec et sans `HX-Request` ; jeton CSRF (même approche que `GatewayUiCsrfIntegrationTest`), dans `test/GatewayUiRealCallIntegrationTest.java`

### Implementation for User Story 1

- [X] T013 [P] [US1] Créer `SensitiveHeaders` (liste R9, masquage `••••`, aucun journal), utilisé **uniquement** pour le résultat de l'appel réel : `testerResult` (simulation) et le formulaire restent inchangés (FR-011), dans `main/internal/realcall/SensitiveHeaders.java`
- [X] T014 [P] [US1] Créer le record `BodyExcerpt(String text, long receivedBytes, boolean truncated, boolean binary)` (« `text` vide si `binary` ») et `BodyExcerptReader` (lecture du flux de `DataBuffer` jusqu'à la limite puis annulation, types textuels `text/*`, `*json`, `*xml`, formulaire) dans `main/internal/realcall/BodyExcerpt.java` et `main/internal/realcall/BodyExcerptReader.java`
- [X] T015 [P] [US1] Créer `RealCallResult` avec les cas `Response(status, headers, BodyExcerpt body, Duration duration, String routeId)` (en-têtes déjà masqués, `routeId` nul si inconnu) et `Failure(Message reason, Duration duration)` dans `main/internal/realcall/RealCallResult.java`
- [X] T016 [P] [US1] Créer `RealCallRequest` (la `TestRequest` validée ; refus si chemin sous `base-path` ou requête invalide, retour d'un `Message`) dans `main/internal/realcall/RealCallRequest.java`
- [X] T017 [P] [US1] Créer `LocalServerAddress` : port effectif lu à l'événement `WebServerInitializedEvent` (correct avec `server.port=0`), schéma `https` si `server.ssl.enabled`, hôte `127.0.0.1` (R1) dans `main/internal/realcall/LocalServerAddress.java`
- [X] T018 [US1] Créer `RealCaller` : `WebClient` dédié sur un `ReactorClientHttpConnector` neuf (pas le builder de l'hôte, pas de redirection suivie, R2), `HttpClient` sans *wiretap* et aucun journal des en-têtes ni du corps (FR-011), en-tête `Host` saisi, en-têtes saisis, corps si `carriesBody()`, `Mono.timeout(timeout)`, mesure de la durée, conversion des erreurs (délai, connexion refusée, TLS) en `Failure` ; méthodes ≤ 30 lignes, dans `main/internal/realcall/RealCaller.java` (dépend de T013–T017)
- [X] T019 [US1] Créer `RealCallWarning` (`SmartInitializingSingleton`, message `WARN` en anglais : l'UI peut envoyer de vraies requêtes, toutes méthodes comprises ; renvoi à la documentation, R11) dans `main/internal/realcall/RealCallWarning.java`
- [X] T020 [US1] Déclarer `LocalServerAddress`, `RealCaller` et `RealCallWarning` seulement si `gateway.ui.tester.real-call.enabled=true` ; injecter `RealCaller` dans le handler via `ObjectProvider` dans `main/autoconfigure/GatewayUiAutoConfiguration.java`
- [X] T021 [US1] Implémenter `GatewayUiHandler.testerCall` : lecture du formulaire, simulation (comme `tester`), appel réel, fragment `testerCallResult` si `HX-Request`, sinon page `tester` complète avec les deux résultats ; signaler une IP saisie différente de la boucle locale (R3) dans `main/internal/web/GatewayUiHandler.java`
- [X] T022 [P] [US1] Créer le fragment `testerCallResult.jte` : statut, durée, en-têtes masqués, extrait du corps (troncature ou « binaire » signalés), erreur lisible, mention de l'IP non appliquée, aide quand le statut est 401 ou 403 (l'appel arrive sans la session de l'utilisateur : saisir ses en-têtes d'authentification), dans `jte/testerCallResult.jte`
- [X] T023 [US1] Dans `tester.jte`, quand `realCallEnabled` : bouton « Envoyer réellement » (`hx-post` vers `/tester/call`, jeton CSRF via `CsrfToken`, jamais déclenché automatiquement, FR-009), avertissement des effets de bord (FR-010), champ « Corps » dans les options avancées ; zone `#tester-call-result` sous le résultat de la simulation, dans `jte/tester.jte`
- [X] T024 [P] [US1] Ajouter les clés `tester.call.*` (bouton, avertissement, corps, statut, durée, en-têtes, troncature, binaire, délai dépassé, injoignable, erreur TLS, chemin de l'UI refusé, IP non appliquée, `tester.call.hint.unauthorized`) dans `i18n/messages_en.properties` et `i18n/messages_fr.properties`
- [X] T025 [P] [US1] Styles du résultat d'appel (cohérents avec `testerResult`, thème sombre compris) dans `gateway-ui-autoconfigure/src/main/resources/io/github/doriangrelu/gatewayui/assets/gateway-ui.css`

**Checkpoint**: l'appel réel fonctionne de bout en bout, mode activé

---

## Phase 4: User Story 2 - Rester en lecture seule tant que l'exploitant n'a rien activé (Priority: P1)

**Goal**: sans configuration, aucune trace de l'appel réel et aucun appel possible.

**Independent Test**: UI activée sans le mode → pas de bouton, `POST /tester/call` en 404, aucun
service appelé (quickstart, scénario 1, SC-001).

### Tests for User Story 2 ⚠️

- [X] T026 [P] [US2] Test d'intégration, mode désactivé, avec une route `Path=/**` vers le service de test : la page testeur n'affiche ni bouton, ni avertissement, ni champ corps ; `POST /gateway-ui/tester/call` répond 404 et le service de test n'a reçu aucune requête ; résultats de simulation identiques à ceux du mode activé (SC-004), en-têtes compris, affichés comme aujourd'hui sans masquage (FR-011), dans `test/GatewayUiIntegrationTest.java`
- [X] T027 [P] [US2] Test du contexte : sans la propriété, aucun bean `RealCaller`, `LocalServerAddress`, `RealCallWarning`, `CallCorrelationFilter` ; avec, ils existent, dans `test/autoconfigure/GatewayUiAutoConfigurationTest.java`

### Implementation for User Story 2

- [X] T028 [US2] Vérifier que T007, T020 et T023 couvrent tous les cas des tests T026–T027 ; corriger sinon (aucun texte, aucun bean, aucune requête quand le mode est désactivé) dans `main/internal/web/GatewayUiHandler.java` et `jte/tester.jte`

**Checkpoint**: US1 et US2 fonctionnent indépendamment ; la promesse de l'ADR 0004 tient par défaut

---

## Phase 5: User Story 3 - Comparer simulation et réalité (Priority: P2)

**Goal**: afficher la route réellement empruntée et signaler un écart avec la simulation.

**Independent Test**: appel réel sur un chemin dont la route réelle diffère de la route simulée
(par exemple via `RemoteAddr` et une IP saisie) → écart signalé (quickstart, scénario 4).

### Tests for User Story 3 ⚠️

- [X] T029 [P] [US3] Tests de `CallRegistry` : inscription, enregistrement, `take` à usage unique, expiration après `timeout`, taille bornée, identifiant inconnu jamais inscrit, dans `test/internal/realcall/CallRegistryTest.java`
- [X] T030 [P] [US3] Tests de `CallCorrelationFilter` : en-tête `X-Gateway-UI-Call` retiré avant la suite de la chaîne (connu ou non) ; route enregistrée au `beforeCommit` ; requête sans en-tête strictement inchangée, dans `test/internal/realcall/CallCorrelationFilterTest.java`
- [X] T031 [P] [US3] Test d'intégration : la route réelle est affichée ; écart signalé quand elle diffère de la route simulée ; le service de test ne reçoit jamais `X-Gateway-UI-Call` ; la page « Filtres globaux » est identique avec et sans le mode, dans `test/GatewayUiRealCallIntegrationTest.java`

### Implementation for User Story 3

- [X] T032 [P] [US3] Créer `CallRegistry` (UUID aléatoire → route, borné par une constante nommée, expiration à `timeout`) dans `main/internal/realcall/CallRegistry.java`
- [X] T033 [US3] Créer `CallCorrelationFilter` (`WebFilter`, ordre `Ordered.HIGHEST_PRECEDENCE`, retire l'en-tête, lit `ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR` dans `beforeCommit` ; Javadoc citant `RoutePredicateHandlerMapping`) dans `main/internal/realcall/CallCorrelationFilter.java`
- [X] T034 [US3] Déclarer `CallRegistry` et `CallCorrelationFilter` seulement quand le mode est activé, dans `main/autoconfigure/GatewayUiAutoConfiguration.java`
- [X] T035 [US3] Dans `RealCaller`, générer l'identifiant, l'inscrire, poser l'en-tête, récupérer la route à la réception et la placer dans `Response.routeId` dans `main/internal/realcall/RealCaller.java`
- [X] T036 [P] [US3] Créer `RouteComparison(String simulatedRouteId, String actualRouteId)` avec `matches()` dans `main/internal/realcall/RouteComparison.java`
- [X] T037 [US3] Afficher la route réelle (lien vers sa page) et l'écart éventuel dans `jte/testerCallResult.jte`, clés `tester.call.route.*` dans `i18n/messages_en.properties` et `i18n/messages_fr.properties`

**Checkpoint**: toutes les user stories fonctionnent indépendamment

---

## Phase 5b: Ergonomie — sélecteur de mode (demande du mainteneur, 2026-10-13)

**Goal**: remplacer le bloc « Appel réel » séparé par un sélecteur « Simulation | Appel réel » dans
le formulaire du testeur (spec, US1 scénarios 2 et 4).

- [X] T046 [US1] Sélecteur de mode dans `jte/tester.jte` (radios hors URL via `hx-params`, toujours sur Simulation sauf page complète renvoyée par un appel réel), bandeau, champ corps et bouton « Envoyer réellement » (type `button`) visibles en mode Appel réel seulement ; `testerCallResult` renvoyé seul en htmx, sans la simulation (`TesterHandler`, suppression de `testerCall.jte` et du paramètre `oob`) ; clés `tester.mode.*` dans `i18n/messages_en.properties` et `i18n/messages_fr.properties`
- [X] T047 [P] [US1] Styles du sélecteur et visibilité par mode (`:has()`), cadre d'avertissement en mode Appel réel, dans `gateway-ui.css`
- [X] T048 [P] [US1] Tests d'intégration : sélecteur présent et sur Simulation, absent quand le mode est désactivé, réponse htmx sans simulation, page complète en mode Appel réel, dans `test/GatewayUiRealCallIntegrationTest.java` et `test/GatewayUiIntegrationTest.java`

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: documentation, exemple, validation finale

- [X] T038 [P] README racine : propriétés `gateway.ui.tester.real-call.*` dans le tableau *Configuration*, section testeur (appel réel, IP non appliquée, TLS ; l'appel arrive sans la session de l'utilisateur, qui saisit ses en-têtes d'authentification ; les en-têtes de la page testeur figurent dans l'URL, donc ne pas y saisir de vrais secrets hors appel réel), recommandation impérative de protéger l'UI si le mode est activé (ADR 0010) dans `README.md`
- [X] T039 [P] README du module : paquet `realcall` dans l'architecture, sens des dépendances, nouveaux tests dans `gateway-ui-autoconfigure/README.md`
- [X] T040 [P] Structure des paquets et sens des dépendances (`web` → `realcall` → `tester`) dans `AGENTS.md`
- [X] T041 [P] Entrée *Ajouté* dans la section `[Unreleased]` de `CHANGELOG.md`
- [X] T042 [P] Activer le mode dans un profil `real-call` de la Gateway d'exemple dans `gateway-ui-sample/src/main/resources/application.yml`
- [X] T043 `./mvnw license:format`, puis `./mvnw clean verify` vert
- [X] T044 Dérouler `quickstart.md` sur la Gateway d'exemple, puis régénérer les captures du testeur avec `docs/screenshots.sh` (non nécessaire : page inchangée dans le profil par défaut de l'exemple ; le mode Appel réel demande un clic, hors de portée du script headless)
- [X] T045 Mettre à jour la roadmap (fonctionnalité livrée en 1.2.0) dans `docs/roadmap.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Préalable** : ADR 0015 accepté
- **Setup (Phase 1)** : aucune dépendance
- **Foundational (Phase 2)** : dépend de la phase 1 ; bloque toutes les user stories
- **US1 (Phase 3)** et **US2 (Phase 4)** : dépendent de la phase 2 ; US2 vérifie aussi T020 et T023 de US1
- **US3 (Phase 5)** : dépend de US1 (`RealCaller`, fragment de résultat)
- **Polish (Phase 6)** : dépend des user stories retenues

### User Story Dependencies

- **US1 (P1)** : après la phase 2
- **US2 (P1)** : dépend de US1 (T020, T023) : US1 et US2 forment ensemble le MVP ; ses tests
  T026–T027 peuvent être écrits en parallèle de US1
- **US3 (P2)** : après US1 ; retirable sans toucher à US1 et US2 si l'ADR 0015 refuse le `WebFilter`

### Within Each User Story

- Tests écrits d'abord, en échec
- Records et utilitaires avant `RealCaller`, `RealCaller` avant le handler, le handler avant les templates
- `./mvnw verify` vert à chaque checkpoint

### Parallel Opportunities

- Phase 2 : T004, T005, T006 en parallèle de T002–T003
- US1 : T009–T012 ensemble, puis T013–T017 ensemble, puis T022, T024 et T025 en parallèle de T021
- US2 : T026 et T027 en parallèle de toute la phase 3
- US3 : T029–T031 ensemble, T032 et T036 en parallèle
- Polish : T038–T042 en parallèle

---

## Parallel Example: User Story 1

```bash
# Tests de US1 :
Task: "Tests de SensitiveHeaders dans test/internal/realcall/SensitiveHeadersTest.java"
Task: "Tests de BodyExcerptReader dans test/internal/realcall/BodyExcerptReaderTest.java"
Task: "Tests de RealCallRequest dans test/internal/realcall/RealCallRequestTest.java"

# Briques de US1 :
Task: "SensitiveHeaders dans main/internal/realcall/SensitiveHeaders.java"
Task: "BodyExcerpt et BodyExcerptReader dans main/internal/realcall/"
Task: "RealCallResult dans main/internal/realcall/RealCallResult.java"
Task: "LocalServerAddress dans main/internal/realcall/LocalServerAddress.java"
```

---

## Implementation Strategy

### MVP First (US1 + US2)

1. Phase 1 et phase 2
2. Phase 3 (US1) et phase 4 (US2) : les deux sont P1, et US2 est la garantie de sécurité
3. **STOP et VALIDATION** : quickstart, scénarios 1 à 3 et 5
4. Démonstration au mainteneur

### Incremental Delivery

1. Setup + Foundational → configuration et URL protégée
2. US1 + US2 → appel réel utilisable, désactivé par défaut (MVP)
3. US3 → comparaison avec la simulation
4. Polish → documentation, captures, `./mvnw clean verify`

---

## Notes

- [P] = fichiers différents, sans dépendance sur une tâche non terminée
- Règles de code d'`AGENTS.md` à chaque tâche : `final` partout, méthodes ≤ 30 lignes, Javadoc publique, pas de nombre magique, en-tête de licence
- Aucun commit sans demande du mainteneur
- 45 tâches : Setup 1, Foundational 7, US1 17, US2 3, US3 9, Polish 8
