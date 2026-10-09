# 0010. Sécuriser l'accès à l'UI

- Statut : Accepté
- Date : 2026-10-09
- Tâche : #15

## Contexte

L'UI expose la topologie de la Gateway, et exposera bientôt toute sa configuration (éditeur, 1.1.0) et des appels réels (1.2.0). Une 1.0.0 doit pouvoir être activée sereinement. L'application hôte a souvent sa propre sécurité (OAuth2, Keycloak, Basic), avec ses propres chaînes Spring Security.

## Options envisagées

- A. Déléguer entièrement à l'application hôte : documentation et exemples.
- B. Auto-configurer une chaîne Spring Security sur les URL de l'UI, avec un rôle requis paramétrable.
- C. Déléguer (A), avec un garde-fou au démarrage.

## Décision

Option C.

- La protection de l'UI relève de l'application hôte. La documentation fournit des exemples prêts à l'emploi : rôle avec Spring Security, OAuth2 avec Keycloak, activation par profil.
- Si l'UI est activée et que Spring Security est absent, un avertissement est journalisé au démarrage.

L'option B est écartée pour l'instant : l'UI devrait imposer un mécanisme d'authentification et entrerait en concurrence avec les chaînes de l'application hôte, dont l'ordre de priorité est une source fréquente d'erreurs. Elle pourra être reconsidérée par un nouvel ADR.

## Conséquences

- Aucune interférence avec la sécurité de l'application hôte, qui garde le choix de son mécanisme d'authentification.
- La protection reste de la responsabilité de l'utilisateur ; l'avertissement réduit le risque d'oubli.
