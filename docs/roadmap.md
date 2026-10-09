# Roadmap

La direction du projet, version par version. Le détail de chaque version est suivi dans les [milestones GitHub](https://github.com/doriangrelu/spring-cloud-gateway-ui/milestones), et les décisions structurantes dans les [ADR](adr).

Cette roadmap donne une intention, pas un engagement de dates.

## Publié

### 1.0.0 (2026-10-09) : stabilisation
API publique définie et politique de compatibilité, UI en français et en anglais, sélecteur de thème, en-têtes de sécurité et guide de protection de l'UI, décisions fondatrices tracées dans des ADR.

### 0.1.0 (2026-10-09)
Routes, détail d'une route avec sa chaîne de filtres, services, filtres globaux, testeur de routes par simulation.

## Prochaines versions

### 1.1.0 : éditeur graphique de routes
- Canevas visuel prédicats → filtres → cible, préchargé avec les routes de la Gateway
- Formulaires générés à partir des fabriques de la Gateway, filtres maison compris
- Génération du YAML à copier dans la configuration : l'éditeur ne modifie jamais la Gateway
- Test de la route éditée avant export

### 1.2.0 : support de WebMVC, appels réels et authentification
- Support de Spring Cloud Gateway Server WebMVC, en plus de WebFlux (voir [ADR 0001](adr/0001-gateway-server-webflux.md))
- Mode « appel réel » du testeur, désactivé par défaut
- Adaptateurs d'authentification : OAuth2 (authorization code, client credentials), Basic, token statique

### 1.3.0 : observabilité
- Métriques par route, état des circuit breakers, configuration des rate limiters

## En continu
- Compatibilité avec les nouvelles versions de Spring Boot et Spring Cloud dès leur sortie GA
- Mises à jour des dépendances proposées par Dependabot

## À l'étude
- Portail développeur : catalogue d'API, documentation OpenAPI avec les chemins exposés par la Gateway, essai des endpoints
- Découverte de services (Kubernetes, `lb://`)

## Hors périmètre
- Modification de la configuration de la Gateway depuis l'UI
