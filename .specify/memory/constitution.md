# Constitution de Gateway UI

## Principes fondamentaux

### I. Les règles du dépôt font foi

`AGENTS.md`, `CONTRIBUTING.md` et les ADR acceptés (`docs/adr/`) sont la référence. Cette
constitution les résume pour Spec Kit, sans les remplacer : en cas d'écart, ce sont eux qui
s'appliquent, et la constitution est corrigée. Une spécification ou un plan qui contredit un ADR
accepté DOIT proposer un nouvel ADR, jamais contourner l'existant.

### II. Lecture seule et fidélité à la Gateway

L'UI ne modifie jamais la configuration de la Gateway et n'appelle jamais les services (ADR 0004).
Le testeur n'exécute aucun filtre : il rejoue ceux enregistrés dans `FilterEffects`. L'ordre des
filtres, la sélection de route et l'effet de chaque filtre simulé DOIVENT reproduire exactement le
code de Spring Cloud Gateway, cité en Javadoc et couvert par un test. Toute exception (par exemple
un mode « appel réel ») passe par un ADR et reste désactivée par défaut.

### III. API publique limitée, UI désactivée par défaut

Sans `gateway.ui.enabled=true`, aucun bean n'est créé et aucune URL n'est exposée (ADR 0003).
L'API publique se limite au starter, aux propriétés `gateway.ui.*`, à `GatewayUiProperties`, au nom
de `GatewayUiAutoConfiguration` et aux URL des pages (ADR 0008) ; tout le reste vit dans
`internal.*`. Une propriété est dépréciée avant d'être supprimée.

### IV. Qualité vérifiée par le build

`./mvnw verify` DOIT être vert avant de rendre la main : licences, Checkstyle, JTE, compilation et
tests. Les règles de code d'`AGENTS.md` (paramètres `final`, méthodes courtes, Javadoc publique,
pas de nombres magiques) ne se contournent pas. Chaque comportement ajouté est couvert par un test
unitaire (`inspect`, `tester`, `editor`) et, pour une page ou une URL, par un test d'intégration.

### V. Langues du projet

Code et identifiants en anglais ; Javadoc, commentaires, documentation, spécifications, plans et
listes de tâches en français. Aucun texte affiché en dur : chaque clé de message existe en français
et en anglais (ADR 0009).

## Contraintes techniques

- Java 25, Spring Boot 4.0, Spring Cloud Gateway Server WebFlux ; rendu serveur JTE et htmx
  (ADR 0001, 0002, 0006).
- Pas de SPA, de build front ni de Thymeleaf. Seul l'éditeur a du JavaScript, en modules natifs
  sans dépendance (ADR 0011).
- Pages et ressources servies par la `RouterFunction` de `GatewayUiRouter` (ADR 0005), sans
  interférer avec l'application hôte.
- Les dépendances internes vont dans un seul sens : `web` → `editor` → `tester` → `inspect`.
- Toute nouvelle dépendance du starter est justifiée dans le plan.

## Processus de développement

- `/speckit-specify` produit la spécification, qui tient lieu de tâche ; elle est validée par le
  mainteneur avant `/speckit-plan`.
- Le plan liste les décisions structurantes. Chacune donne un ADR *Proposé* dans `docs/adr/` ;
  `/speckit-implement` ne démarre pas avant leur acceptation.
- Chaque étape (spécification, plan, tâches) est soumise au mainteneur avant la suivante.
- Aucun commit, push, tag ni changement de version sans demande explicite (`RELEASING.md`).
- Un changement visible par les utilisateurs met à jour `CHANGELOG.md` (`[Unreleased]`) et le README
  concerné.

## Gouvernance

Cette constitution est modifiée par `/speckit-constitution`, après validation du mainteneur. Une
modification qui change une règle de fond passe d'abord par un ADR. Version sémantique : MAJEURE
pour un principe supprimé ou redéfini, MINEURE pour un principe ou une section ajoutés, CORRECTIVE
pour une reformulation. Chaque plan vérifie sa conformité à cette constitution (« Constitution
Check »).

**Version** : 1.0.0 | **Ratifiée** : 2026-10-13 | **Dernière modification** : 2026-10-13
