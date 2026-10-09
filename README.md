# Gateway UI

Starter Spring Boot qui embarque une UI de consultation dans une **Spring Cloud Gateway (WebFlux)** :

- **Routes** : liste dans l'ordre d'évaluation, recherche, et détail de chaque route (prédicats, chaîne complète des filtres globaux et de route dans l'ordre d'exécution, cible, métadonnées).
- **Services** : routes regroupées par service cible, qu'il soit déclaré ou déduit des URI.
- **Filtres globaux** : avec leur ordre effectif.
- **Testeur** : pour une requête donnée (méthode, hôte, chemin, en-têtes, IP), indique la route retenue, les routes masquées, et la requête réellement transmise au service (chemin réécrit, en-têtes). Aucune requête n'est envoyée.

Stack : Spring Boot 4.0 / Spring Cloud 2025.1 (Gateway 5.0), templates [JTE](https://jte.gg) précompilés, [htmx](https://htmx.org). Aucune SPA, aucun build front.

## Utilisation

```xml
<dependency>
    <groupId>dev.gatewayui</groupId>
    <artifactId>gateway-ui-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```yaml
gateway:
  ui:
    enabled: true                 # false par défaut : rien n'est exposé tant que ce n'est pas activé
    base-path: /gateway-ui        # défaut
    discover-services: true       # déduit les services des URI des routes (défaut)
    services:                     # facultatif : libellés, services sans route, rattachements explicites
      orders:
        url: http://orders.shop.svc.cluster.local:8080
        display-name: Commandes
      payments:
        url: http://payments.shop.svc.cluster.local:8080
        route-ids: [payments-v1, payments-v2]
```

Pour désactiver l'UI en production, il suffit de ne pas positionner `gateway.ui.enabled` (ou de le passer à `false`, par exemple dans un profil `prod`) : aucun bean n'est créé et aucune URL n'est exposée.

## Points de conception

- **Pas de `ViewResolver`** : le moteur JTE est privé à l'UI (paquet `dev.gatewayui.jte`). Il n'interfère pas avec le rendu de l'application hôte.
- **Pas de masquage par la Gateway** : pages et ressources (CSS, htmx) sont servies par une `RouterFunction`, dont le mapping est prioritaire sur celui de la Gateway. Une route `Path=/**` ne les intercepte pas (couvert par un test).
- **Lecture seule** : l'UI n'appelle jamais les services. Le testeur évalue les prédicats sur un échange fictif et rejoue sur le papier les filtres connus : `StripPrefix`, `PrefixPath`, `RewritePath`, `SetPath`, `Add/Set/RemoveRequestHeader`, `AddRequestParameter`, `SetRequestHostHeader`, `PreserveHostHeader`.
- **Limites** : les filtres des routes Java DSL (lambdas) et les filtres personnalisés ne sont pas simulés. Les routes sans `id` reçoivent un id aléatoire de la Gateway et sont traitées comme des routes Java.

## Modules

| Module | Rôle |
|---|---|
| `gateway-ui-autoconfigure` | Code, templates, auto-configuration |
| `gateway-ui-spring-boot-starter` | Dépendance à ajouter dans la Gateway |
| `gateway-ui-sample` | Gateway d'exemple (`mvn -f gateway-ui-sample/pom.xml spring-boot:run`, puis http://localhost:8080/gateway-ui) |
