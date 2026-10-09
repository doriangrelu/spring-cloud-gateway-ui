# Architecture Decision Records

Les décisions structurantes du projet, au format [MADR](https://adr.github.io/madr/), en français.

- Un fichier par décision, numéroté : `NNNN-titre.md`, à partir du [modèle](template.md).
- Statuts : *Proposé*, *Accepté*, *Remplacé par NNNN*.
- Un ADR accepté n'est plus modifié : si la décision change, un nouvel ADR le remplace.
- Aucun développement qui dépend d'une décision structurante ne démarre avant l'acceptation de son ADR.

| # | Décision | Statut |
|---|---|---|
| [0000](0000-tracer-les-decisions-dans-des-adr.md) | Tracer les décisions d'architecture dans des ADR | Accepté |
| [0001](0001-gateway-server-webflux.md) | Supporter Spring Cloud Gateway Server WebFlux, WebMVC à terme | Accepté |
| [0002](0002-rendu-serveur-jte-htmx.md) | Rendu serveur avec JTE et htmx | Accepté |
| [0003](0003-ui-desactivee-par-defaut.md) | UI désactivée par défaut | Accepté |
| [0004](0004-testeur-en-simulation.md) | Testeur de routes en simulation, en lecture seule | Accepté |
| [0005](0005-routerfunction-pour-pages-et-ressources.md) | Pages et ressources servies par une RouterFunction | Accepté |
| [0006](0006-java-25.md) | Java 25 comme version minimale | Accepté |
| [0007](0007-regles-de-code-outillees.md) | Règles de code imposées par l'outillage | Accepté |
