# 0013. Catalogue documenté des prédicats et filtres

- Statut : Accepté
- Date : 2026-10-09
- Tâche : #24 (spike #28)

## Contexte

L'éditeur doit aider les développeurs à choisir et configurer prédicats et filtres, et leur permettre de progresser. Spring Cloud Gateway compte une soixantaine de fabriques ; les filtres maison d'une application ne sont documentés nulle part.

## Options envisagées

- Renvoyer uniquement vers la documentation officielle.
- Reprendre le texte de la documentation officielle.
- Rédiger un catalogue propre au projet, traduit, avec un lien vers la documentation officielle.

## Décision

- Le catalogue est rédigé avec nos propres mots, en français et en anglais, dans les fichiers de messages de l'UI (ADR 0009). Il ne recopie pas la documentation de Spring.
- Pour chaque fabrique : une catégorie, un résumé, des détails, la description de chaque argument, un exemple YAML et un lien vérifié vers la documentation officielle.
- Il est enrichi progressivement : les fabriques les plus utilisées d'abord ; une fabrique non documentée l'indique explicitement, et les filtres maison sont signalés comme tels.
- Il est affiché dans la palette de recherche, dans l'aide du panneau de l'éditeur, et sur une page « Catalogue » de l'UI.
- Chaque fabrique restant à documenter peut faire l'objet d'une issue `good first issue`.

## Conséquences

- Un travail de rédaction à maintenir dans deux langues, contrôlé par le test de complétude des messages.
- Le catalogue devient un support d'apprentissage, consultable hors de l'éditeur.
