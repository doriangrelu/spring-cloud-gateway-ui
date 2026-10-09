# Contribuer

Merci de votre intérêt pour Gateway UI ! Ce guide décrit comment construire le projet, les règles de code et le processus de contribution.

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

- Rendu serveur avec JTE et interactions htmx : **pas de SPA, pas de build front**.
- Chaque écran doit rester accessible par son URL. Une requête htmx reçoit un fragment, une navigation classique la page complète.
- Les couleurs passent par les variables CSS de `gateway-ui.css`, définies pour le thème clair et le thème sombre.

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
