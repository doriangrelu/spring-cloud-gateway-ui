# 0003. UI désactivée par défaut

- Statut : Accepté
- Date : 2026-10-09 (rétroactif)
- Tâche : #12

## Contexte

L'UI expose la topologie interne de la Gateway : hôtes des services, filtres, en-têtes. L'ajout du starter ne doit pas suffire à l'exposer, en particulier en production.

## Options envisagées

- Activée par défaut, désactivable.
- Désactivée par défaut, activable.

## Décision

L'UI ne s'active qu'avec `gateway.ui.enabled=true`. Désactivée, elle ne crée aucun bean et n'expose aucune URL.

## Conséquences

- Aucune exposition accidentelle.
- L'utilisateur doit l'activer explicitement, typiquement par profil (`dev`, `recette`).
