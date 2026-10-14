# Implementation Plan: Mode « appel réel » du testeur

**Branch**: `001-testeur-appel-reel` | **Date**: 2026-10-13 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-testeur-appel-reel/spec.md`

## Summary

Ajouter au testeur une action explicite « Envoyer réellement », désactivée par défaut
(`gateway.ui.tester.real-call.enabled`). L'UI envoie la requête saisie (toutes méthodes, corps
facultatif) en boucle locale au serveur qui l'héberge, pour passer par la vraie Gateway, et affiche
statut, en-têtes masqués, extrait borné du corps, durée et route réellement empruntée, à côté de la
simulation, qui reste inchangée. Détail des choix : [research.md](research.md).

## Technical Context

**Language/Version**: Java 25

**Primary Dependencies**: Spring Boot 4.0, Spring Cloud Gateway Server WebFlux 5.0, Reactor Netty
(déjà présent), JTE, htmx. Aucune nouvelle dépendance.

**Storage**: N/A (registre de corrélation en mémoire, borné, à expiration)

**Testing**: JUnit 5, `WebTestClient` ; `GatewayUiIntegrationTest` avec un service de test servi par
l'application de test (R12)

**Target Platform**: Gateway Spring Cloud WebFlux embarquant le starter (JVM serveur)

**Project Type**: bibliothèque (starter Spring Boot) avec UI serveur

**Performance Goals**: aucun coût quand le mode est désactivé ; un appel réel coûte un aller-retour
loopback plus le service

**Constraints**: délai maximal configurable (5 s), extrait de corps borné (64 Ko), aucun en-tête
sensible journalisé ni affiché en clair

**Scale/Scope**: 1 nouveau paquet (`internal.realcall`, ~8 classes), 1 URL, 1 fragment JTE,
3 propriétés, ~15 clés de message

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principe | Vérification | Statut |
|---|---|---|
| I. Règles du dépôt | L'appel réel déroge à l'ADR 0004 (aucune requête émise) : couvert par l'ADR 0015, accepté le 2026-10-13, qui le complète sans le remplacer. | ✅ |
| II. Lecture seule, fidélité | Mode désactivé par défaut ; simulation inchangée (SC-004) ; le `WebFilter` de corrélation n'agit que sur les requêtes marquées par l'UI et n'entre pas dans la chaîne des routes. Le testeur n'exécute toujours aucun filtre lui-même : c'est la Gateway qui traite l'appel. | ✅ |
| III. API publique, désactivée par défaut | 3 propriétés ajoutées, constructeur historique de `GatewayUiProperties` conservé et déprécié (R10) ; nouvelle URL `POST {base-path}/tester/call` ; paramètres de la page testeur inchangés. | ✅ |
| IV. Qualité | Tests unitaires de `internal.realcall` et de `TestRequest`, intégration activé/désactivé ; règles Checkstyle respectées (méthodes courtes, `final`). | ✅ |
| V. Langues | Textes de l'UI en clés FR/EN ; message de démarrage en anglais comme `UnprotectedUiWarning`. | ✅ |
| Contraintes techniques | Pas de JavaScript ajouté (htmx) ; URL servie par la `RouterFunction` (R6) ; sens des dépendances : `web` → `realcall` → `tester` → `inspect`. | ✅ |
| Processus | Décisions structurantes R1, R4, R10 → ADR 0015, accepté. | ✅ |

Réévaluation après la phase 1 : aucune nouvelle violation ; le paquet `realcall` dépend de HTTP,
ce qui est permis : la règle « sans HTTP » vise `inspect`, `tester` et `editor`.

## Project Structure

### Documentation (this feature)

```text
specs/001-testeur-appel-reel/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── real-call-endpoint.md
├── checklists/
│   └── requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
gateway-ui-autoconfigure/src/main/java/io/github/doriangrelu/gatewayui/
├── autoconfigure/
│   ├── GatewayUiProperties.java          # + Tester, Tester.RealCall, constructeur déprécié
│   └── GatewayUiAutoConfiguration.java   # + beans realcall (conditionnels au mode)
└── internal/
    ├── tester/TestRequest.java           # + champ body
    ├── realcall/                         # NOUVEAU
    │   ├── package-info.java
    │   ├── RealCaller.java               # envoi loopback, délai, lecture bornée
    │   ├── LocalServerAddress.java       # schéma, port effectif
    │   ├── RealCallResult.java           # Response | Failure
    │   ├── RealCallRequest.java          # requête validée (refus sous base-path)
    │   ├── BodyExcerpt.java
    │   ├── BodyExcerptReader.java        # lecture bornée du corps
    │   ├── RouteComparison.java          # route simulée / route réelle
    │   ├── SensitiveHeaders.java         # masquage (R9)
    │   ├── CallRegistry.java             # corrélation id → route
    │   ├── CallCorrelationFilter.java    # WebFilter (R4)
    │   └── RealCallWarning.java          # avertissement de démarrage (R11)
    └── web/
        ├── GatewayUiRouter.java          # + POST {base}/tester/call, toujours déclaré
        └── GatewayUiHandler.java         # + testerCall ; drapeau du mode dans la page
gateway-ui-autoconfigure/src/main/jte/
├── tester.jte                            # + bouton, avertissement, champ corps
└── testerCallResult.jte                  # NOUVEAU fragment
gateway-ui-autoconfigure/src/main/resources/.../i18n/messages_{en,fr}.properties
gateway-ui-autoconfigure/src/test/java/.../
├── internal/realcall/*Test.java
└── GatewayUiRealCallIntegrationTest.java # activé / désactivé, service de test
```

**Écarts d'implémentation** (validés au fil de `/speckit-implement`) :

- `TesterHandler` sort le testeur de `GatewayUiHandler`, et les handlers deviennent des beans
  internes : sinon la méthode du bean `RouterFunction` dépassait 6 paramètres (Checkstyle).
- Utilitaire `HtmlResponses` partagé entre handlers ; `RealCallResult` en un seul record.
- `TestRequest` garde un constructeur à 5 arguments : l'éditeur n'est pas modifié.
- Sélecteur « Simulation | Appel réel » dans le formulaire (demande du mainteneur) au lieu d'un bloc
  séparé ; en mode Appel réel, seule la réponse réelle est affichée, avec la route et l'URL
  transmise relevées par `CallCorrelationFilter`.

**Structure Decision**: un paquet `internal.realcall` entre `web` et `tester`, parce que l'appel
réel est par nature HTTP et ne peut pas vivre dans `tester`. Documentation à mettre à jour :
`AGENTS.md` (structure), README racine (tableau *Configuration*, section testeur),
README d'`autoconfigure` (architecture, tests), `CHANGELOG.md` (*Ajouté*), captures du testeur.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| `WebFilter` ajouté à l'application hôte (mode activé seulement) | Connaître la route réellement empruntée (US3, FR-012) | `GlobalFilter` fausserait la chaîne affichée ; en-tête de réponse visible par tous les clients. Si l'ADR 0015 refuse ce point, US3 est retirée et le reste du plan tient. |
| Constructeur déprécié dans un record public | Compatibilité de `GatewayUiProperties` (ADR 0008) | Casser le constructeur imposerait une version majeure. |
