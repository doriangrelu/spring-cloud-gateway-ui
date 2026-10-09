# 0011. Technique de l'éditeur graphique

- Statut : Accepté
- Date : 2026-10-09
- Tâche : #24 (spike #28)

## Contexte

L'éditeur graphique de routes demande des interactions riches : canevas, glisser-déposer, palette de recherche, mises à jour en direct. htmx seul ne les couvre pas, et l'ADR 0002 exclut une SPA et un build npm. Deux prototypes ont été comparés dans la Gateway d'exemple (#28) et manipulés par le mainteneur.

## Options envisagées

- Drawflow, librairie de graphes en JavaScript (prototypée) : son graphe libre ne correspond pas au pipeline linéaire d'une route ; il a fallu le contourner partout (ordre déduit de la position, liaisons manuelles annulées, zoom centré).
- Un framework de SPA (React, Vue) : impose un build npm, contraire à l'ADR 0002.
- Un canevas maison en JavaScript natif (prototypé) : léger, sans dépendance, adapté au pipeline linéaire.

## Décision

- La page de l'éditeur reste rendue par JTE (structure, navigation, textes traduits).
- Les interactions sont assurées par un canevas maison en JavaScript natif (modules ES), sans build ni dépendance externe, servi par la `RouterFunction` de l'UI. La CSP stricte reste inchangée.
- La logique métier est côté serveur, en Java, derrière une API JSON sous le chemin de l'UI : correspondance des arguments avec les champs des fabriques, génération du YAML, conseils et simulation. Le JavaScript se limite à l'affichage et à l'espace de travail.
- La simulation de la route éditée est exacte : la route est construite par le constructeur de routes de la Gateway (`RouteDefinitionRouteLocator`), puis passée au simulateur du testeur.
- htmx reste en version 2 : l'éditeur n'en a pas besoin. La migration vers htmx 4 (#10) sera réévaluée plus tard.

## Conséquences

- Tout ce qui détermine le résultat (YAML, conseils, simulation) est testé en Java ; le JavaScript reste mince.
- Quelques appels à l'API pendant l'édition, différés de quelques centaines de millisecondes.
- Pas de tests automatisés du JavaScript pour l'instant : vérification dans le navigateur.
- Les appels d'écriture de l'API (POST) doivent rester compatibles avec la protection CSRF de Spring Security quand l'application hôte l'active.
