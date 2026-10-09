# 0002. Rendu serveur avec JTE et htmx

- Statut : Accepté
- Date : 2026-10-09 (rétroactif)
- Tâche : #12

## Contexte

L'UI sert essentiellement à consulter la configuration, et elle est embarquée dans l'application de l'utilisateur. Elle ne doit ni alourdir son build, ni interférer avec son propre rendu (moteur de templates, `ViewResolver`).

## Options envisagées

- Une SPA (React, Vue) avec un build npm.
- Thymeleaf.
- JTE avec htmx.

## Décision

JTE, avec des templates précompilés au build dans un paquet dédié et sans `ViewResolver`, et htmx pour les interactions. Pas de build npm : les librairies front sont embarquées via des webjars.

## Conséquences

- Un starter léger, sans compilateur de templates à l'exécution et sans conflit avec l'application hôte.
- Chaque écran reste accessible par son URL : un fragment pour htmx, la page complète sinon.
- Les interactions riches, comme l'éditeur graphique de la 1.1.0, demanderont des librairies JavaScript : un nouvel ADR complétera cette décision.
