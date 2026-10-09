# 0001. Supporter Spring Cloud Gateway Server WebFlux, WebMVC à terme

- Statut : Accepté
- Date : 2026-10-09 (rétroactif)
- Tâche : #12

## Contexte

Spring Cloud Gateway existe en deux variantes aux modèles internes très différents : Server WebFlux (`RouteDefinition`, `RouteLocator`, prédicats et filtres introspectables) et Server WebMVC (`RouterFunction`, beaucoup plus opaques). Les premiers cas d'usage tournent sous Kubernetes, où la faible consommation mémoire de WebFlux est un atout.

## Options envisagées

- Supporter les deux variantes dès la première version.
- WebMVC seule.
- WebFlux d'abord, WebMVC ensuite.

## Décision

Jusqu'à la 1.0.0 incluse, seule la variante Server WebFlux est supportée. Le support de Server WebMVC est un objectif du projet, planifié à partir de la 1.1.0. Il fera l'objet d'un ADR dédié, qui tranchera notamment la façon d'introspecter des `RouterFunction`.

## Conséquences

- Un seul modèle à inspecter pour la 1.0.0, donc une introspection fidèle à la Gateway.
- Les utilisateurs de WebMVC ne sont pas servis avant la 1.1.0.
- L'API publique définie pour la 1.0.0 ne doit pas exposer de type propre à WebFlux, pour permettre l'ajout de WebMVC sans version majeure.
