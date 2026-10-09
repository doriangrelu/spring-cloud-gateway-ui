# Changelog

Toutes les évolutions notables du projet sont consignées ici.

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/), et le projet respecte le [versionnage sémantique](https://semver.org/lang/fr/). Tant que la version est en `0.x`, une version mineure peut introduire des changements incompatibles : ils sont alors signalés dans la rubrique *Modifié*.

## [Unreleased]

### Modifié

- API publique définie et politique de compatibilité documentée ([ADR 0008](docs/adr/0008-api-publique-et-compatibilite.md)) : l'implémentation passe dans les paquets `io.github.doriangrelu.gatewayui.internal.*`, sans garantie de compatibilité, et les beans de l'UI ne sont plus remplaçables (`@ConditionalOnMissingBean` retiré).

## [0.1.0] - 2026-10-09

### Ajouté

- Starter `gateway-ui-spring-boot-starter` pour Spring Cloud Gateway Server WebFlux 5.0 (Spring Boot 4.0, Spring Cloud 2025.1, Java 25).
- UI activable par `gateway.ui.enabled`, désactivée par défaut, servie sous `gateway.ui.base-path` (`/gateway-ui` par défaut).
- Liste des routes dans l'ordre d'évaluation, avec recherche instantanée.
- Détail d'une route : prédicats, chaîne complète des filtres globaux et de route dans l'ordre d'exécution, cible et métadonnées.
- Vue des services : services déclarés dans `gateway.ui.services` et services déduits des URI des routes (`gateway.ui.discover-services`).
- Vue des filtres globaux avec leur ordre effectif, et signalement des filtres sans ordre explicite.
- Testeur de routes : route retenue, routes masquées, simulation de `StripPrefix`, `PrefixPath`, `RewritePath`, `SetPath`, `AddRequestHeader`, `SetRequestHeader`, `RemoveRequestHeader`, `AddRequestParameter`, `SetRequestHostHeader` et `PreserveHostHeader`, sans émettre de requête.
- Thèmes clair et sombre, affichage adapté au mobile.

[Unreleased]: https://github.com/doriangrelu/spring-cloud-gateway-ui/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/doriangrelu/spring-cloud-gateway-ui/releases/tag/v0.1.0
