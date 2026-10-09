# 0007. Règles de code imposées par l'outillage

- Statut : Accepté
- Date : 2026-10-09 (rétroactif)
- Tâche : #12

## Contexte

Le projet est destiné à recevoir des contributions, y compris produites avec des assistants IA. Des règles seulement écrites ne sont pas tenues dans la durée.

## Options envisagées

- Règles documentées uniquement.
- Règles vérifiées par l'outillage, qui fait échouer le build.

## Décision

Checkstyle et la vérification des en-têtes de licence font échouer le build sur les règles vérifiables : paramètres `final`, méthodes de 30 lignes maximum, complexité limitée, Javadoc de l'API publique, pas de nombres magiques, en-tête Apache 2.0. Les autres règles sont documentées dans CONTRIBUTING.md et AGENTS.md : champs `final`, aucune classe `final`, découpage logique des méthodes.

## Conséquences

- Une qualité homogène, contrôlée en CI.
- Les seuils ne changent que par un nouvel ADR.
