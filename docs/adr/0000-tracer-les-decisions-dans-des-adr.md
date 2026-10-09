# 0000. Tracer les décisions d'architecture dans des ADR

- Statut : Accepté
- Date : 2026-10-09 (rétroactif)
- Tâche : #12

## Contexte

Gateway UI est un projet open source appelé à accueillir des contributeurs. Jusqu'à la 0.1.0, les décisions n'étaient tracées que dans des échanges et dans AGENTS.md : on ne retrouvait ni les options écartées, ni les raisons des choix.

## Options envisagées

- Ne rien formaliser, s'appuyer sur l'historique Git et les issues.
- Un document d'architecture unique, mis à jour au fil de l'eau.
- Des ADR : un document court et daté par décision.

## Décision

Toute décision structurante fait l'objet d'un ADR au format MADR, en français, dans `docs/adr/`. Aucun développement qui en dépend ne démarre avant son acceptation. Un ADR accepté n'est plus modifié : il est remplacé par un nouvel ADR.

## Conséquences

- L'historique des choix, options écartées comprises, est lisible par tous.
- Un peu de formalisme avant chaque sujet structurant.
