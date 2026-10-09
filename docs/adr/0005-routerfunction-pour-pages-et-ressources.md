# 0005. Pages et ressources servies par une RouterFunction

- Statut : Accepté
- Date : 2026-10-09 (rétroactif)
- Tâche : #12

## Contexte

Le mapping des routes de la Gateway (`RoutePredicateHandlerMapping`) passe avant le handler de ressources statiques de Spring Boot. Une route `Path=/**` intercepterait donc le CSS et le JavaScript de l'UI.

## Options envisagées

- Servir les ressources via `static/` ou `META-INF/resources`.
- Tout servir par une `RouterFunction`.

## Décision

Pages et ressources sont servies par une `RouterFunction`, dont le mapping est prioritaire sur celui de la Gateway, sous un préfixe dédié (`gateway.ui.base-path`, qui ne peut pas être la racine).

## Conséquences

- L'UI fonctionne quelles que soient les routes de l'utilisateur ; un test d'intégration le vérifie avec une route attrape-tout.
- Rien n'est servi via `static/` ou `META-INF/resources`.
