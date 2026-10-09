# gateway-ui-spring-boot-starter

La dépendance à ajouter dans une application **Spring Cloud Gateway Server WebFlux** pour embarquer la Gateway UI.

Ce module ne contient aucun code : il apporte [`gateway-ui-autoconfigure`](../gateway-ui-autoconfigure) et ses dépendances d'exécution (moteur JTE, sans compilateur, et webjar htmx). Spring Cloud Gateway n'est pas tiré par le starter : c'est votre application qui le fournit, dans la version de votre choix parmi celles supportées.

## Installation

Maven :

```xml
<dependency>
    <groupId>io.github.doriangrelu</groupId>
    <artifactId>gateway-ui-spring-boot-starter</artifactId>
    <version><!-- dernière version --></version>
</dependency>
```

Gradle :

```kotlin
implementation("io.github.doriangrelu:gateway-ui-spring-boot-starter:<dernière version>")
```

## Activation

L'UI est désactivée par défaut :

```yaml
gateway:
  ui:
    enabled: true
```

Elle est alors servie sur `/gateway-ui`. Toutes les propriétés sont décrites dans le [README du projet](../README.md#configuration), et l'IDE les complète grâce aux métadonnées de configuration incluses.

## Dépendances apportées

| Dépendance | Rôle |
|---|---|
| `gateway-ui-autoconfigure` | Auto-configuration et pages de l'UI |
| `gg.jte:jte-runtime` | Rendu des templates précompilés |
| `org.webjars.npm:htmx.org` | Interactions sans rechargement de page |
