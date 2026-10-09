# Gateway UI

[![CI](https://github.com/doriangrelu/spring-cloud-gateway-ui/actions/workflows/ci.yml/badge.svg)](https://github.com/doriangrelu/spring-cloud-gateway-ui/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.doriangrelu/gateway-ui-spring-boot-starter)](https://central.sonatype.com/artifact/io.github.doriangrelu/gateway-ui-spring-boot-starter)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)
![Java](https://img.shields.io/badge/java-25-orange)

Une UI embarquée dans votre **Spring Cloud Gateway (WebFlux)** pour voir, enfin, ce que fait votre Gateway : quelles routes existent, dans quel ordre elles sont évaluées, quels filtres s'exécutent, et surtout **quelle route prend une requête donnée et ce qui arrive au service**.

Un starter à ajouter, une propriété à activer, et l'UI est disponible sur `/gateway-ui`.

## Fonctionnalités

| Écran | Ce qu'on y voit |
|---|---|
| **Routes** | Toutes les routes dans l'ordre d'évaluation, avec une recherche instantanée sur l'id, les prédicats ou l'URI. |
| **Détail d'une route** | Le parcours d'une requête : prédicats → chaîne de filtres → cible. La chaîne mélange filtres globaux et filtres de route dans **l'ordre réel d'exécution**, calculé comme le fait la Gateway. |
| **Services** | Les routes regroupées par service cible, qu'il soit déclaré en configuration ou déduit des URI des routes. |
| **Filtres globaux** | L'ordre effectif de chaque filtre global. Les filtres sans ordre explicite sont signalés, par exemple un `@Order` posé sur une méthode `@Bean`, que la Gateway ignore. |
| **Testeur** | Pour une requête donnée (méthode, hôte, chemin, en-têtes, IP) : la route retenue, les routes **masquées** par une route prioritaire, le chemin réécrit étape par étape et la requête transmise au service. **Aucune requête n'est envoyée.** |

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

## Compatibilité

| Gateway UI | Java | Spring Boot | Spring Cloud | Gateway |
|---|---|---|---|---|
| 0.1.x | 25+ | 4.0.x | 2025.1.x | Server WebFlux 5.0.x |

La variante **Server WebMVC** de Spring Cloud Gateway n'est pas supportée.

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
