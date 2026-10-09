# 0004. Testeur de routes en simulation, en lecture seule

- Statut : Accepté
- Date : 2026-10-09 (rétroactif)
- Tâche : #12

## Contexte

Le besoin principal est de savoir quelle route traite une requête, et ce que la Gateway transmet au service. Exécuter de vrais appels ou de vrais filtres aurait des effets de bord : consommation d'un rate limiter, ouverture d'un circuit breaker, appels aux services.

## Options envisagées

- Exécuter réellement la requête à travers la Gateway.
- Exécuter la chaîne de filtres sans appel réseau.
- Simuler à partir des déclarations des routes.

## Décision

Les prédicats sont évalués sur un échange fictif, au corps vide. Les filtres de la route retenue sont rejoués à partir de leur déclaration, uniquement ceux dont l'algorithme est reproduit à l'identique (`FilterEffects`). Aucun filtre n'est exécuté et aucune requête n'est émise.

## Conséquences

- Aucun risque, même si l'UI est activée en production.
- Les filtres non reproduits sont listés comme « non simulés ».
- Le mode « appel réel » prévu en 1.2.0 complétera cette décision par un nouvel ADR, sans la remplacer : la simulation reste le comportement par défaut.
