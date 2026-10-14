# 0014. Développement piloté par les spécifications avec Spec Kit

- Statut : Accepté
- Date : 2026-10-14
- Tâche : [specs/001-testeur-appel-reel](../../specs/001-testeur-appel-reel/spec.md) (pilote)

## Contexte

Depuis la 1.0.0, chaque évolution suit le même processus : une tâche, un ADR pour toute décision structurante, une validation du mainteneur, puis le code. Ce processus repose sur la discipline de chacun, et en particulier des agents IA, qui écrivent une bonne part du code : rien ne garantit que le besoin est décrit avant le code, ni que le plan fait apparaître les décisions qui méritent un ADR.

[Spec Kit](https://github.com/github/spec-kit) outille ce cycle : une spécification (le quoi, le pourquoi), un plan technique (le comment), une liste de tâches, puis l'implémentation, avec une *constitution* que chaque étape vérifie. Il fournit des commandes pour Claude Code et GitHub Copilot.

## Options envisagées

- Garder le processus actuel, sans outillage.
- Adopter Spec Kit, avec une constitution qui recopie les règles du projet.
- Adopter Spec Kit, avec une constitution qui renvoie à `AGENTS.md` et aux ADR.

## Décision

Spec Kit (version figée 1.1.3) est adopté, après un pilote sur la fonctionnalité « appel réel » du testeur (1.2.0) :

- La **constitution** (`.specify/memory/constitution.md`) renvoie à `AGENTS.md` et aux ADR, qui restent la référence ; elle ne les recopie pas, pour éviter deux sources qui divergent.
- La **spécification** (`specs/NNN-.../spec.md`) tient lieu de tâche ; elle est reliée au milestone GitHub de la version.
- Le **plan** liste les décisions structurantes. Chacune donne un ADR *Proposé* ; `/speckit-implement` ne démarre pas avant leur acceptation.
- Spécifications, plans et tâches sont rédigés en **français** ; chaque étape est validée par le mainteneur avant la suivante.
- Les intégrations Claude Code (`.claude/skills/`) et Copilot (`.github/skills/`) sont versionnées. Les scripts ne créent ni branche ni commit (extension `git` non installée).

## Conséquences

- Le besoin est écrit et validé avant le plan, et le plan avant le code ; les ADR naissent du plan au lieu d'être oubliés.
- Les fichiers générés par Spec Kit (`.specify/`, skills) sont en anglais et se mettent à jour avec la CLI (`specify integration upgrade`) ; les modifier à la main complique ces mises à jour.
- Le cycle ajoute des documents : pour une correction mineure, on peut s'en passer, la règle de la tâche et de l'ADR restant valable.

## Bilan du pilote

- Les clarifications de `/speckit-specify` ont fixé le périmètre (chemin de l'appel, méthodes, corps) avant toute ligne de code.
- Le plan a fait naître l'ADR 0015 et révélé des contraintes absentes de la spécification (URL toujours déclarée, appel de l'UI par elle-même).
- `/speckit-analyze` a repéré un risque de sécurité (en-têtes sensibles dans l'URL du testeur), tranché avant l'implémentation.
- Les écarts d'implémentation et les demandes du mainteneur en cours de route (sélecteur de mode, URL transmise) sont restés tracés dans la spécification, le plan et les tâches.
- Coût constaté : modèles et commandes en anglais, documents plus nombreux ; acceptable pour une fonctionnalité, superflu pour une correction.
