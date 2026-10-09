# AGENTS.md

Instructions pour les agents IA (Claude Code, Copilot, Codex, Cursor...) qui travaillent sur ce dépôt. Les humains trouveront les mêmes règles, plus détaillées, dans [CONTRIBUTING.md](CONTRIBUTING.md).

## Le projet en bref

**Gateway UI** est un starter Spring Boot open source (Apache 2.0) qui embarque une UI de consultation dans une **Spring Cloud Gateway Server WebFlux**. L'UI montre les routes, les filtres et les services, et propose un testeur de routes qui n'envoie aucune requête.

- **Stack** : Java 25, Spring Boot 4.0, Spring Cloud 2025.1 (Gateway 5.0), templates **JTE** précompilés, **htmx**.
- **Publication** : `io.github.doriangrelu`, sur Maven Central.
- **Langue** : code et identifiants en anglais ; Javadoc, commentaires et documentation en **français** ; UI en français et en anglais (ADR 0009).
- **Échanges avec le mainteneur : toujours en français**, y compris les comptes rendus, les résumés et les questions.

## Commandes

```bash
./mvnw verify                          # build complet : licences, Checkstyle, JTE, compilation, tests
./mvnw -pl gateway-ui-autoconfigure verify
./mvnw license:format                  # ajoute l'en-tête Apache 2.0 aux nouveaux fichiers Java
./mvnw install -DskipTests && ./mvnw -f gateway-ui-sample/pom.xml spring-boot:run   # UI sur http://localhost:8080/gateway-ui
./mvnw -P release -pl '!gateway-ui-sample' -Dgpg.skip=true verify                   # vérifie les artefacts de release
```

**Avant de rendre la main, lancez toujours `./mvnw verify`.** Il doit être vert : la CI exécute la même chose.

Après un renommage de package ou de template, faites un `./mvnw clean verify` : les sources JTE générées dans `target/` deviennent obsolètes.

## Structure

```
gateway-ui-autoconfigure/            tout le code
  src/main/java/io/github/doriangrelu/gatewayui/
    autoconfigure/       GatewayUiAutoConfiguration, GatewayUiProperties (gateway.ui.*) : API publique
    internal/            implémentation, sans garantie de compatibilité (ADR 0008)
      i18n/             messages FR/EN (messages_xx.properties) et clés de message (ADR 0009)
      inspect/          lecture seule de la Gateway : routes, définitions, filtres, services
      tester/           testeur : SimulatedExchange, FilterSimulator, registre FilterEffects
      editor/           éditeur graphique : routes éditables, YAML, conseils, simulation de la route éditée, catalogue
      web/              RouterFunction, handlers, rendu JTE
  src/main/jte/      templates (layout + pages + fragments htmx)
  src/main/resources/io/github/doriangrelu/gatewayui/assets/gateway-ui.css
  src/main/resources/io/github/doriangrelu/gatewayui/assets/editor/   modules JavaScript natifs de l'éditeur (ADR 0011)
gateway-ui-spring-boot-starter/      pom seul, aucun code
gateway-ui-sample/                   Gateway d'exemple, jamais publiée
config/                              Checkstyle et en-tête de licence
```

Les dépendances vont dans un seul sens : `web` → `editor` → `tester` → `inspect` → Spring Cloud Gateway. `inspect`, `tester` et `editor` ne doivent dépendre ni de HTTP ni des templates.

## Règles de code (non négociables)

Checkstyle fait échouer le build si l'une des règles marquées ✔ n'est pas respectée.

- ✔ `final` sur **tous les paramètres** de méthodes et de constructeurs.
- `final` sur les **champs**. Un champ mutable n'est acceptable que s'il porte un état, comme dans `SimulationState`.
- **Jamais de classe `final`.** Une classe utilitaire a simplement un constructeur privé.
- ✔ Méthodes de **30 lignes maximum** (viser une dizaine), 6 paramètres maximum, complexité cyclomatique ≤ 8, 2 niveaux de `if` imbriqués au plus, 3 `return` au plus.
- Une méthode fait une seule chose : découpez en méthodes privées bien nommées. Préférez une table de correspondance à un long `switch` (voir `FilterEffects`).
- ✔ Javadoc sur tout type et toute méthode **publics**. Les records documentent leurs composants avec `@param`. Chaque package a un `package-info.java`.
- ✔ Pas de nombres magiques dans le code de production (constantes nommées), pas d'imports `*`, accolades obligatoires.
- ✔ En-tête de licence Apache 2.0 en tête de chaque fichier Java (`./mvnw license:format`).
- Préférez les `record` et les collections immuables (`List.of`, `List.copyOf`, `Stream.toList()`).
- Les commentaires expliquent un **pourquoi** (contrainte, piège, comportement de la Gateway reproduit), jamais ce que fait le code.

## Comportements à préserver

- **API publique limitée** (ADR 0008) : le starter, les propriétés `gateway.ui.*`, `GatewayUiProperties`, le nom de `GatewayUiAutoConfiguration` et les URL des pages. Tout nouveau code d'implémentation va dans `internal.*` ; ne rendez rien remplaçable (`@ConditionalOnMissingBean`) sans ADR. Une propriété se déprécie avant d'être supprimée.
- **Désactivée par défaut** : sans `gateway.ui.enabled=true`, aucun bean n'est créé et aucune URL n'est exposée.
- **Lecture seule** : l'UI ne modifie jamais la configuration et n'appelle jamais les services. Le testeur **n'exécute aucun filtre** ; il rejoue uniquement ceux enregistrés dans `FilterEffects`.
- **Fidélité à la Gateway** : l'ordre des filtres (`GatewayInspector.pipeline`), la sélection de route et l'effet de chaque filtre simulé doivent reproduire exactement le code de Spring Cloud Gateway. Citez la classe de la Gateway reproduite dans la Javadoc, et testez-la.
- **Pas de masquage par la Gateway** : pages *et* ressources statiques passent par la `RouterFunction` de `GatewayUiRouter`. Ne servez rien via `static/` ou `META-INF/resources` : une route `Path=/**` l'intercepterait.
- **Pas d'interférence avec l'application hôte** : pas de `ViewResolver`, et les templates restent dans le package dédié `io.github.doriangrelu.gatewayui.internal.jte`.
- **htmx** : une requête avec `HX-Request` reçoit un fragment, une navigation classique la page complète. Chaque écran doit rester accessible par son URL.
- **i18n** (ADR 0009) : aucun texte affiché en dur, ni dans les templates (`ui.message("clé")`) ni dans le code (`Message.of("clé", args)`). Toute clé est ajoutée dans `messages_en.properties` **et** `messages_fr.properties`, sinon le build échoue ; dans un message avec arguments, l'apostrophe s'écrit `''`.

## Ce qu'il ne faut pas faire

- Ajouter une SPA, un build front (npm...) ou Thymeleaf. Seul l'éditeur a du JavaScript : des modules natifs, sans dépendance, qui affichent et appellent l'API ; la logique reste en Java (ADR 0011).
- Abaisser la version de Java, ou cibler Gateway Server WebMVC.
- Ajouter une dépendance au starter sans raison forte : il est embarqué dans les Gateways des utilisateurs.
- Contourner Checkstyle (`@SuppressWarnings("checkstyle:...")`, modification des seuils) sans demande explicite.
- Publier, tagger ou modifier la version des POM : la release est manuelle et passe par un tag (voir [RELEASING.md](RELEASING.md)).

## Ajouter une fonctionnalité courante

| Je veux... | Étapes |
|---|---|
| **Simuler un nouveau filtre** | 1. Écrire une méthode `(Definition, SimulationState) -> String` dans `FilterEffects`, qui reproduit la fabrique de la Gateway. 2. L'enregistrer dans `EFFECTS`. 3. L'ajouter à `PATH_FILTERS` si elle modifie le chemin. 4. Ajouter un test, et mettre à jour la liste des filtres dans le README de `gateway-ui-autoconfigure`. |
| **Ajouter une page** | 1. Créer un template `src/main/jte/<page>.jte` qui utilise `@template.layout(...)`. 2. Ajouter une méthode au `GatewayUiHandler`. 3. Déclarer la route dans `GatewayUiRouter.pages`. 4. Ajouter le lien dans `layout.jte`. 5. Ajouter un test dans `GatewayUiIntegrationTest`. |
| **Ajouter une propriété** | 1. Ajouter le composant au record `GatewayUiProperties`, avec sa Javadoc `@param` et un `@DefaultValue`. 2. La documenter dans le tableau *Configuration* du README racine. |
| **Documenter une fabrique du catalogue** | Ajouter `catalog.predicate.<Nom>.summary` (ou `catalog.filter.<Nom>.summary`), `.details` (facultatif), `.arg.<champ>` pour chaque champ et `.example` dans les deux fichiers de messages, avec nos propres mots (ADR 0013). `GatewayUiCatalogIntegrationTest` vérifie noms et champs. Une nouvelle fabrique de la Gateway se classe dans `Catalog` (catégorie, lien vérifié vers la documentation). |

## Tests

- **Unitaires** pour `inspect` et `tester` (`*Test.java`, à côté du package testé). **Intégration** dans `GatewayUiIntegrationTest`, qui monte une vraie Gateway avec routes déclarées et l'interroge via `WebTestClient`.
- Noms de tests en anglais, sous forme de phrase : `stripPrefixKeepsTrailingSlash`.
- Les nombres littéraux sont autorisés dans les tests.

## Documentation et commits

- Tout changement visible par les utilisateurs doit être ajouté à [CHANGELOG.md](CHANGELOG.md), section `[Unreleased]`, sous la bonne rubrique (*Ajouté*, *Modifié*, *Corrigé*...).
- Si une propriété, un filtre simulé ou un comportement change, mettez à jour le README concerné. Si l'UI change visuellement, régénérez les captures avec `docs/screenshots.sh` (Gateway d'exemple démarrée).
- Messages de commit au format Conventional Commits : `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `build:`, `ci:`.
- Ne commitez ni ne poussez sans que la personne qui vous pilote le demande.
