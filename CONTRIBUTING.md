# Contribuer

Merci de votre intérêt pour Gateway UI ! Ce guide décrit comment construire le projet, les règles de code et le processus de contribution.

Si vous travaillez avec un assistant IA, il trouvera ces règles en version condensée dans [AGENTS.md](AGENTS.md).

## Construire

Prérequis : **JDK 25**. Maven est fourni par le wrapper.

```bash
./mvnw verify
```

`verify` exécute, dans l'ordre :

1. la vérification des en-têtes de licence ;
2. Checkstyle ;
3. la compilation des templates JTE et du code ;
4. les tests.

Si l'une de ces étapes échoue, le build échoue, en local comme en CI.

Pour essayer l'UI, lancez la [Gateway d'exemple](gateway-ui-sample).

## Règles de code

Ces règles visent un code **lisible sans effort** : on doit comprendre une méthode sans la relire. Celles marquées ✔ sont vérifiées automatiquement par Checkstyle ([config/checkstyle/checkstyle.xml](config/checkstyle/checkstyle.xml)).

### Immutabilité

- ✔ **Paramètres `final`** sur toutes les méthodes et tous les constructeurs.
- **Champs `final`** dès que possible. Un champ mutable doit être justifié par l'état qu'il porte (par ex. `SimulationState`).
- **Variables locales `final`** quand elles ne sont pas réassignées.
- **Pas de classes `final`** : elles doivent pouvoir être étendues. Une classe utilitaire a simplement un constructeur privé.
- Préférez les `record` pour les données et les collections immuables (`List.of`, `List.copyOf`, `Stream.toList()`).

### Méthodes courtes, découpe logique

- ✔ **30 lignes maximum** par méthode. Viser plutôt une dizaine.
- ✔ **6 paramètres maximum**, **complexité cyclomatique ≤ 8**, **2 niveaux de `if` imbriqués** au plus, **3 `return`** au plus.
- Une méthode fait **une seule chose**, à un seul niveau d'abstraction. Quand on a envie d'y mettre un commentaire pour séparer des étapes, chaque étape devient une méthode privée bien nommée.
- Préférez une table de correspondance ou du polymorphisme à un long `switch`. `FilterEffects` en est l'exemple : un filtre simulé = une méthode + une ligne dans le registre.
- Une classe a une responsabilité claire. Les paquets `inspect` et `tester` ne connaissent ni HTTP ni les templates.

### Nommage et lisibilité

- Des noms qui disent le **pourquoi métier** (`assignedRouteIds`, `targetsAService`) plutôt que le comment (`set2`, `check`).
- ✔ Pas de nombre magique : une constante nommée (`HTTPS_PORT`). Les tests en sont exemptés.
- ✔ Accolades toujours, une instruction par ligne, pas d'imports `*`.
- Les commentaires expliquent ce que le code ne peut pas dire : une contrainte, un piège, un comportement de la Gateway reproduit. Pas de paraphrase du code.

### Documentation

- ✔ **Javadoc obligatoire** sur les types et méthodes publics. Les records documentent leurs composants avec `@param`.
- Chaque paquet a un `package-info.java`.
- Quand le code reproduit un comportement de Spring Cloud Gateway, la Javadoc cite la classe de la Gateway concernée (par ex. `FilteringWebHandler`, `StripPrefixGatewayFilterFactory`).

### Licence

- ✔ Chaque fichier Java commence par l'en-tête Apache 2.0 ([config/license-header.txt](config/license-header.txt)). Pour l'ajouter automatiquement :

  ```bash
  ./mvnw license:format
  ```

### Tests

- Toute évolution s'accompagne de tests. Les tests unitaires couvrent `inspect` et `tester`, et `GatewayUiIntegrationTest` couvre le parcours HTTP complet.
- Un comportement de la Gateway reproduit (ordre des filtres, effet d'un filtre) doit être testé contre la vraie Gateway ou contre son algorithme exact.
- Noms de tests en anglais, sous forme de phrase : `stripPrefixKeepsTrailingSlash`.

### Interface

- Rendu serveur avec JTE et interactions htmx : **pas de SPA, pas de build front**. Seul l'éditeur utilise des modules JavaScript natifs (`assets/editor/`), sans dépendance ([ADR 0011](docs/adr/0011-technique-de-l-editeur.md)) : ils affichent et appellent l'API, toute la logique reste en Java.
- Chaque écran doit rester accessible par son URL. Une requête htmx reçoit un fragment, une navigation classique la page complète.
- Les couleurs passent par les variables CSS de `gateway-ui.css`, définies pour le thème clair et le thème sombre.
- Après un changement visible de l'UI, régénérez les captures de la documentation, dans les deux thèmes, avec la [Gateway d'exemple](gateway-ui-sample) démarrée :

  ```bash
  docs/screenshots.sh
  ```

### Documenter un prédicat ou un filtre du catalogue

Le catalogue ([ADR 0013](docs/adr/0013-catalogue-documente.md)) est une bonne première contribution. Pour une fabrique, ajoutez dans `messages_fr.properties` **et** `messages_en.properties` :

```properties
catalog.filter.SetResponseHeader.summary=Remplace un en-tête de la réponse renvoyée au client.
catalog.filter.SetResponseHeader.details=(facultatif) Précision utile, piège à éviter.
catalog.filter.SetResponseHeader.arg.name=Nom de l'en-tête.
catalog.filter.SetResponseHeader.arg.value=Nouvelle valeur.
catalog.filter.SetResponseHeader.example=- SetResponseHeader=X-Frame-Options, DENY
```

- Rédigez avec vos propres mots, sans recopier la documentation de Spring, et vérifiez le comportement dans le code de la Gateway.
- Décrivez **chaque** champ (`.arg.<champ>`) : les noms viennent de `shortcutFieldOrder()` de la fabrique, visibles sur la page « Catalogue ».
- Dans un fichier `.properties`, un antislash s'écrit `\\` (`\\d+`, `$\\{segment}`).
- `GatewayUiCatalogIntegrationTest` vérifie que chaque clé correspond à une fabrique et à un champ réels, et que tous les champs sont décrits.

## Commits et pull requests

- **Une PR = un sujet**, avec des tests, et une CI verte.
- Messages de commit au format [Conventional Commits](https://www.conventionalcommits.org/fr/) : `feat: ...`, `fix: ...`, `docs: ...`, `refactor: ...`, `test: ...`, `build: ...`, `ci: ...`.
- **Mettez à jour [CHANGELOG.md](CHANGELOG.md)** dans la section `[Unreleased]` pour tout changement visible par les utilisateurs. Utilisez les rubriques *Ajouté*, *Modifié*, *Déprécié*, *Supprimé*, *Corrigé* ou *Sécurité*.

## Signaler un problème

Ouvrez une [issue](https://github.com/doriangrelu/spring-cloud-gateway-ui/issues) avec :

- les versions de Gateway UI, Spring Boot et Spring Cloud ;
- la déclaration des routes concernées ;
- ce que l'UI affiche, et ce que vous attendiez.

Pour une faille de sécurité, n'ouvrez pas d'issue publique : utilisez les [alertes de sécurité privées](https://github.com/doriangrelu/spring-cloud-gateway-ui/security/advisories/new) du dépôt.

## Licence

En contribuant, vous acceptez que votre contribution soit distribuée sous [licence Apache 2.0](LICENSE).
