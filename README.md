# Gateway UI

[![CI](https://github.com/doriangrelu/spring-cloud-gateway-ui/actions/workflows/ci.yml/badge.svg)](https://github.com/doriangrelu/spring-cloud-gateway-ui/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.doriangrelu/gateway-ui-spring-boot-starter)](https://central.sonatype.com/artifact/io.github.doriangrelu/gateway-ui-spring-boot-starter)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)
![Java](https://img.shields.io/badge/java-25-orange)

Une UI embarquée dans votre **Spring Cloud Gateway (WebFlux)** pour voir, enfin, ce que fait votre Gateway : quelles routes existent, dans quel ordre elles sont évaluées, quels filtres s'exécutent, et surtout **quelle route prend une requête donnée et ce qui arrive au service**.

Un starter à ajouter, une propriété à activer, et l'UI est disponible sur `/gateway-ui`.

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/images/tester-dark.png">
  <img alt="Testeur de routes : la requête GET /api/users/legacy/42 est prise par la route users, la route users-legacy est signalée comme masquée, et le chemin est réécrit en /users/legacy/42" src="docs/images/tester-light.png">
</picture>

## Fonctionnalités

| Écran | Ce qu'on y voit |
|---|---|
| **Routes** | Toutes les routes dans l'ordre d'évaluation, avec une recherche instantanée sur l'id, les prédicats ou l'URI. |
| **Détail d'une route** | Le parcours d'une requête : prédicats → chaîne de filtres → cible. La chaîne mélange filtres globaux et filtres de route dans **l'ordre réel d'exécution**, calculé comme le fait la Gateway. |
| **Services** | Les routes regroupées par service cible, qu'il soit déclaré en configuration ou déduit des URI des routes. |
| **Filtres globaux** | L'ordre effectif de chaque filtre global. Les filtres sans ordre explicite sont signalés, par exemple un `@Order` posé sur une méthode `@Bean`, que la Gateway ignore. |
| **Testeur** | Pour une requête donnée (méthode, hôte, chemin, en-têtes, IP) : la route retenue, les routes **masquées** par une route prioritaire, le chemin réécrit étape par étape et la requête transmise au service. **Aucune requête n'est envoyée.** |

### Aperçu

#### Le parcours d'une requête dans une route

Requête entrante → prédicats → **toute la chaîne de filtres**, globaux et de route mélangés dans l'ordre où la Gateway les exécute réellement → cible. C'est la vue qui répond enfin à la question « mais dans quel ordre passent mes filtres ? ».

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/images/route-detail-dark.png">
  <img alt="Parcours d'une requête dans la route users : le prédicat Path, puis 13 filtres globaux et de route dans l'ordre d'exécution avec leur ordre, puis la cible" src="docs/images/route-detail-light.png">
</picture>

#### Les autres écrans

<details open>
<summary><strong>Routes</strong> : toutes les routes, dans l'ordre d'évaluation</summary>

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/images/routes-dark.png">
  <img alt="Liste des routes avec leur ordre, leurs prédicats, leur cible et leur nombre de filtres" src="docs/images/routes-light.png">
</picture>
</details>

<details>
<summary><strong>Services</strong> : services déclarés et déduits, avec leurs routes</summary>

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/images/services-dark.png">
  <img alt="Cartes des services cibles, déclarés dans la configuration ou déduits des URI des routes" src="docs/images/services-light.png">
</picture>
</details>

<details>
<summary><strong>Filtres globaux</strong> : ordre effectif et filtres non ordonnés</summary>

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/images/global-filters-dark.png">
  <img alt="Filtres globaux triés par ordre, dont un filtre lambda signalé comme non ordonné" src="docs/images/global-filters-light.png">
</picture>
</details>

Toutes ces captures ont été prises sur la [Gateway d'exemple](gateway-ui-sample). L'UI suit le thème clair ou sombre du système.

## Démarrage rapide

**Prérequis :** Java 25 et Spring Boot 4.0 avec Spring Cloud 2025.1 (Gateway Server WebFlux 5.0).

```xml
<dependency>
    <groupId>io.github.doriangrelu</groupId>
    <artifactId>gateway-ui-spring-boot-starter</artifactId>
    <version><!-- dernière version --></version>
</dependency>
```

```yaml
gateway:
  ui:
    enabled: true
```

Ouvrez ensuite `http://<votre-gateway>/gateway-ui`.

## Configuration

| Propriété | Défaut | Description |
|---|---|---|
| `gateway.ui.enabled` | `false` | Active l'UI. Désactivée, l'UI ne crée aucun bean et n'expose aucune URL. |
| `gateway.ui.base-path` | `/gateway-ui` | Préfixe des pages et ressources de l'UI. La racine `/` est refusée. |
| `gateway.ui.discover-services` | `true` | Déduit les services cibles à partir des URI des routes. |
| `gateway.ui.services.<nom>.url` | | URL du service. Les routes qui pointent vers le même schéma, hôte et port lui sont rattachées. |
| `gateway.ui.services.<nom>.display-name` | `<nom>` | Libellé affiché. |
| `gateway.ui.services.<nom>.route-ids` | | Routes rattachées explicitement au service. |

```yaml
gateway:
  ui:
    enabled: true
    services:
      orders:
        url: http://orders.shop.svc.cluster.local:8080
        display-name: Commandes
      payments:
        url: http://payments.shop.svc.cluster.local:8080
        route-ids: [ payments-v1, payments-v2 ]
```

## En production

L'UI expose la topologie interne de la Gateway (hôtes, filtres, en-têtes). Elle est donc **désactivée par défaut**. Deux options :

- **Ne pas l'activer en production**, par exemple avec `gateway.ui.enabled: true` uniquement dans les profils `dev` et `recette`.
- **La protéger** : ses URL sont de simples routes WebFlux sous `gateway.ui.base-path`, que Spring Security peut sécuriser comme n'importe quel endpoint.

```java
@Bean
SecurityWebFilterChain security(ServerHttpSecurity http) {
    return http
            .authorizeExchange(exchanges -> exchanges
                    .pathMatchers("/gateway-ui/**").hasRole("OPS")
                    .anyExchange().permitAll())
            .httpBasic(Customizer.withDefaults())
            .build();
}
```

L'UI est en **lecture seule** : elle ne modifie pas les routes et n'appelle jamais les services.

Ses réponses portent des en-têtes de sécurité stricts (`Content-Security-Policy` limitée aux ressources de l'UI, interdiction d'intégration dans une frame, etc.). Ils ne s'appliquent qu'aux pages de l'UI, jamais aux routes de votre Gateway.

## Compatibilité

| Gateway UI | Java | Spring Boot | Spring Cloud | Gateway |
|---|---|---|---|---|
| 0.1.x | 25+ | 4.0.x | 2025.1.x | Server WebFlux 5.0.x |
| 1.0.x *(à venir)* | 25+ | 4.0.x | 2025.1.x | Server WebFlux 5.0.x |

La variante **Server WebMVC** de Spring Cloud Gateway n'est pas encore supportée : elle est prévue pour la 1.1.0 (voir la [roadmap](docs/roadmap.md)).

### Ce qui est garanti

À partir de la 1.0.0, le projet suit le [versionnage sémantique](https://semver.org/lang/fr/) sur son API publique ([ADR 0008](docs/adr/0008-api-publique-et-compatibilite.md)) :

- les coordonnées du starter ;
- les propriétés `gateway.ui.*` : noms, types et sens des valeurs par défaut ;
- `GatewayUiProperties` et le nom de `GatewayUiAutoConfiguration` ;
- les URL des pages de l'UI, y compris les paramètres du testeur.

Une propriété est d'abord dépréciée dans une version mineure, puis supprimée à la version majeure suivante.

Les classes des paquets `io.github.doriangrelu.gatewayui.internal.*`, les beans de l'UI, les templates et les ressources sont des détails d'implémentation, sans garantie de compatibilité.

Chaque ligne mineure supporte une génération de Spring Cloud et la génération de Spring Boot associée. Passer à une nouvelle génération donne une nouvelle version mineure, signalée dans le changelog et dans le tableau ci-dessus.

## Modules

| Module | Rôle |
|---|---|
| [`gateway-ui-spring-boot-starter`](gateway-ui-spring-boot-starter) | La dépendance à ajouter dans votre Gateway. |
| [`gateway-ui-autoconfigure`](gateway-ui-autoconfigure) | Code, templates et auto-configuration. Son README décrit l'architecture. |
| [`gateway-ui-sample`](gateway-ui-sample) | Gateway d'exemple pour essayer l'UI en local. Non publiée. |

## Essayer en local

```bash
./mvnw install -DskipTests
```

```bash
./mvnw -f gateway-ui-sample/pom.xml spring-boot:run
```

L'UI est alors sur http://localhost:8080/gateway-ui.

## Contribuer

Les contributions sont bienvenues. Les règles de code, le build et le processus de PR sont décrits dans [CONTRIBUTING.md](CONTRIBUTING.md), les évolutions dans [CHANGELOG.md](CHANGELOG.md) et la publication dans [RELEASING.md](RELEASING.md).

## Licence

Distribué sous [licence Apache 2.0](LICENSE).

Spring, Spring Boot et Spring Cloud sont des marques de Broadcom Inc. et/ou de ses filiales. Ce projet est indépendant : il n'est ni affilié à Broadcom ni approuvé par Broadcom.
