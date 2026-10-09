# 0009. Internationalisation de l'UI en français et en anglais

- Statut : Accepté
- Date : 2026-10-09
- Tâche : #14

## Contexte

L'UI n'existait qu'en français, ce qui limite l'adoption d'un projet publié sur Maven Central. L'éditeur de la 1.1.0 ajoutera beaucoup de libellés : ils doivent être traduisibles dès leur création. L'UI est embarquée dans l'application de l'utilisateur et ne doit pas interférer avec sa propre internationalisation.

## Options envisagées

Stockage des messages :

- le `MessageSource` de Spring, déclaré comme bean ;
- le support de localisation de JTE ;
- des fichiers de messages lus par une instance propre à l'UI.

Choix de la langue : en-tête `Accept-Language`, sélecteur dans l'UI, propriété de configuration, ou une combinaison.

## Décision

- Les messages sont dans `messages_fr.properties` et `messages_en.properties`, dans un paquet interne, lus par une instance privée à l'UI, qui n'est pas un bean : aucun conflit possible avec le `messageSource` de l'application hôte.
- Les textes produits par l'implémentation (notes du testeur, erreurs de saisie) sont des clés de message avec arguments, traduites au rendu.
- La langue est choisie, par ordre de priorité :
  1. depuis l'UI, par un sélecteur FR | EN dans la barre de navigation, mémorisé dans un cookie ;
  2. par l'en-tête `Accept-Language` du navigateur ;
  3. par la propriété `gateway.ui.default-locale`, qui vaut `en` par défaut.
- Un test vérifie que les fichiers de messages contiennent exactement les mêmes clés.

## Conséquences

- L'UI s'affiche en anglais par défaut, en français pour un navigateur configuré en français, et chacun peut changer de langue depuis l'UI.
- `gateway.ui.default-locale` rejoint l'API publique (ADR 0008).
- Tout nouveau libellé doit être ajouté dans les deux langues, sinon le build échoue.
