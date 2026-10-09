# Changelog

Toutes les évolutions notables du projet sont consignées ici.

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/), et le projet respecte le [versionnage sémantique](https://semver.org/lang/fr/). Tant que la version est en `0.x`, une version mineure peut introduire des changements incompatibles : ils sont alors signalés dans la rubrique *Modifié*.

## [Unreleased]

## [1.1.0] - 2026-10-09

Éditeur graphique de routes et catalogue documenté des prédicats et filtres.

### Ajouté

- Éditeur graphique de routes, page **Éditeur** (`{base-path}/editor`) ([ADR 0011](docs/adr/0011-technique-de-l-editeur.md), [ADR 0012](docs/adr/0012-modele-d-edition-et-export.md)) : modification des routes existantes et création de nouvelles sur un canevas, palette de recherche des prédicats et filtres au clavier, même testeur que la page **Testeur** sur la route éditée (méthode, hôte, en-têtes, adresse du client), seule ou dans la configuration pour voir quelle route prend la requête ; la route est construite par la Gateway elle-même, filtres par défaut compris, export YAML (route courante, configuration complète ou modifications seulement) avec les placeholders `${...}` d'origine. Le travail reste dans le navigateur : la Gateway n'est jamais modifiée. Compatible avec la protection CSRF de Spring Security.
- Catalogue documenté des prédicats et filtres ([ADR 0013](docs/adr/0013-catalogue-documente.md)) : nouvelle page **Catalogue** (`{base-path}/catalog`) et aide dans la palette et le panneau de l'éditeur. Chaque fabrique de la Gateway est classée par catégorie, avec un lien vers la documentation officielle ; 22 des plus courantes sont expliquées en français et en anglais (résumé, arguments, exemple). Les fabriques maison sont signalées comme telles.

### Modifié

- Sur écran étroit, les onglets de la barre de navigation passent sur leur propre ligne au lieu de défiler horizontalement.

## [1.0.0] - 2026-10-09

Première version stable : l'API publique est définie et suit le versionnage sémantique ([ADR 0008](docs/adr/0008-api-publique-et-compatibilite.md)).

### Ajouté

- UI disponible en anglais et en français ([ADR 0009](docs/adr/0009-internationalisation.md)) : langue du navigateur par défaut, sélecteur **EN | FR** dans la barre de navigation (choix mémorisé), et nouvelle propriété `gateway.ui.default-locale` (`en` par défaut).
- Sélecteur de thème Auto / Clair / Sombre dans la barre de navigation, sans JavaScript : le choix est mémorisé et appliqué par le serveur, sans changement visible au chargement.
- Avertissement au démarrage si l'UI est activée sans Spring Security sur le classpath, et guide de protection dans le README : activation par profil, rôle Spring Security, connexion OAuth2 avec Keycloak ([ADR 0010](docs/adr/0010-securiser-l-acces-a-l-ui.md)).

### Modifié

- L'UI s'affiche désormais en anglais par défaut, sauf si le navigateur demande le français.
- API publique définie et politique de compatibilité documentée ([ADR 0008](docs/adr/0008-api-publique-et-compatibilite.md)) : l'implémentation passe dans les paquets `io.github.doriangrelu.gatewayui.internal.*`, sans garantie de compatibilité, et les beans de l'UI ne sont plus remplaçables (`@ConditionalOnMissingBean` retiré).

### Sécurité

- En-têtes de sécurité sur toutes les réponses de l'UI, et uniquement sur elles : `Content-Security-Policy` stricte (seules les ressources servies par l'UI sont autorisées), `X-Content-Type-Options`, `X-Frame-Options` et `Referrer-Policy`.

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

[Unreleased]: https://github.com/doriangrelu/spring-cloud-gateway-ui/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/doriangrelu/spring-cloud-gateway-ui/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/doriangrelu/spring-cloud-gateway-ui/compare/v0.1.0...v1.0.0
[0.1.0]: https://github.com/doriangrelu/spring-cloud-gateway-ui/releases/tag/v0.1.0
