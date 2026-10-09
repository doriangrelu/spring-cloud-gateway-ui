# 0008. API publique et politique de compatibilité

- Statut : Accepté
- Date : 2026-10-09
- Tâche : #13

## Contexte

En 1.0.0, tout ce qui est public devient un engagement semver. Jusqu'à la 0.1.0, toutes les classes d'implémentation (`inspect`, `tester`, `web`) étaient publiques et les beans remplaçables : la moindre refonte, pour le mode « appel réel » ou le support de WebMVC, aurait imposé une version majeure.

## Options envisagées

- A. Tout laisser public, avec une politique écrite précisant ce qui est garanti.
- B. Déplacer l'implémentation dans des paquets `io.github.doriangrelu.gatewayui.internal.*`, explicitement exclus de la garantie.
- C. Masquer l'implémentation avec les modules Java (JPMS) : inopérant, les applications Spring Boot tournant sur le classpath.

## Décision

Option B.

Est garanti d'une version à l'autre, selon le versionnage sémantique :

- les coordonnées du starter `io.github.doriangrelu:gateway-ui-spring-boot-starter`, point d'entrée officiel ;
- les propriétés `gateway.ui.*` : noms, types et sens des valeurs par défaut ;
- la classe `GatewayUiProperties` et le nom de `GatewayUiAutoConfiguration` (utilisable dans `spring.autoconfigure.exclude`) ;
- les URL des pages sous `gateway.ui.base-path`, y compris les paramètres du testeur, pour que les liens partagés restent valides ;
- le comportement d'activation (ADR 0003).

N'est pas garanti : tout le contenu des paquets `internal.*`, les beans de l'UI (qui ne sont plus remplaçables), les templates, le HTML et le CSS, les fragments htmx et les URL des ressources statiques.

Politique de compatibilité :

- une propriété est d'abord dépréciée dans une version mineure, avec les métadonnées de configuration, puis supprimée à la version majeure suivante ;
- chaque ligne mineure supporte une génération de Spring Cloud (et la génération de Spring Boot associée) ; passer à une nouvelle génération donne une nouvelle version mineure, annoncée dans la rubrique *Modifié* du changelog et dans la table de compatibilité du README ;
- aucun type propre à WebFlux dans la partie garantie (ADR 0001).

## Conséquences

- L'implémentation peut être refactorée librement, notamment pour WebMVC et l'appel réel.
- Les utilisateurs qui remplaçaient des beans de l'UI en 0.1.0 ne le peuvent plus officiellement.
- Suivre une nouvelle génération de Spring ne demande pas de version majeure, au prix d'un écart assumé avec une lecture stricte de semver.
