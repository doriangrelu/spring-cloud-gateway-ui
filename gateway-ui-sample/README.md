# gateway-ui-sample

Une Gateway d'exemple pour essayer l'UI en local. Ses routes couvrent les cas que l'UI sait montrer. Les services cibles n'existent pas : rien n'est nécessaire pour explorer l'UI, qui n'appelle jamais les services.

Ce module n'est **pas publié** sur Maven Central.

## Lancer

Depuis la racine du projet :

```bash
./mvnw install -DskipTests
```

```bash
./mvnw -f gateway-ui-sample/pom.xml spring-boot:run
```

L'UI est ensuite sur http://localhost:8080/gateway-ui.

## Les routes et ce qu'elles montrent

| Route | Déclaration | Ce qu'elle illustre |
|---|---|---|
| `orders` | YAML : `Path=/api/orders/**`, `StripPrefix=1` | Réécriture de chemin simple. Le service est déclaré dans `gateway.ui.services` avec un libellé. |
| `users` | YAML : `RewritePath` avec groupe nommé, `AddResponseHeader` | Réécriture par regex, et filtre qui n'agit que sur la réponse. |
| `users-legacy` | YAML : `order: 10`, `Path=/api/users/legacy/**` | Route **masquée** par `users`, évaluée avant elle. |
| `catalog-product` | YAML : `Path=/catalog/{id}`, `Method=GET`, `SetPath`, `PreserveHostHeader` | Variables de chemin et conservation du `Host`. |
| `admin` | YAML : `Host=admin.example.com`, `Header=X-Admin-Token, .+` | Prédicats sur l'hôte et les en-têtes, et métadonnées. |
| `legacy-java` | Java DSL | Ce que l'UI peut montrer d'une route Java : prédicats évalués, filtres non simulables. |

S'y ajoutent :

- un `default-filters` (`AddRequestHeader=X-Request-Source, gateway`), présent dans la chaîne de toutes les routes déclaratives ;
- le filtre global `correlationIdFilter`, déclaré en lambda avec `@Order` sur la méthode `@Bean`. La Gateway ignore cet ordre, et l'UI le signale « non ordonné ».

## À essayer dans le testeur

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="../docs/images/tester-dark.png">
  <img alt="Testeur sur GET /api/users/legacy/42 : route users retenue, users-legacy masquée" src="../docs/images/tester-light.png">
</picture>

| Requête | Résultat attendu |
|---|---|
| `GET /api/users/legacy/42` | Route `users` retenue, `users-legacy` masquée, chemin `/users/legacy/42` |
| `GET /catalog/7` | Chemin `/products/7`, variable `id=7`, `Host` d'origine conservé |
| `GET /x` avec l'hôte `admin.example.com` et l'en-tête `X-Admin-Token: abc` | Route `admin` |
| `GET /old/foo` | Route Java DSL : filtres non simulés |
| `GET /nothing` | Aucune route : la Gateway répondrait 404 |
