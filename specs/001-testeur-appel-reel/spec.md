# Feature Specification: Mode « appel réel » du testeur

**Feature Branch**: `001-testeur-appel-reel`

**Created**: 2026-10-13

**Status**: Draft

**Input**: User description: "Mode « appel réel » du testeur, désactivé par défaut (roadmap 1.2.0)"

## Clarifications

### Session 2026-10-13

- Q: Par où passe l'appel réel ? → A: Par la Gateway elle-même, comme un vrai client.
- Q: Quelles méthodes HTTP sont autorisées ? → A: Toutes, dès que le mode est activé.
- Q: L'appel réel peut-il porter un corps ? → A: Oui, saisi par l'utilisateur, pour les méthodes
  qui en portent un.
- Q: Comment passer de la simulation à l'appel réel ? → A: Un sélecteur de mode dans le formulaire
  du testeur, toujours sur Simulation à l'ouverture (le mode n'est pas dans l'URL).
- Q: En mode Appel réel, la simulation reste-t-elle affichée ? → A: Non, seule la réponse réelle ;
  la comparaison des routes (US3) reste indiquée par un badge.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Envoyer réellement la requête testée (Priority: P1)

Un développeur qui met au point ses routes utilise le testeur. La simulation lui dit quelle route
est retenue et ce qui serait transmis au service, mais pas ce que le service répond réellement, ni
l'effet des filtres non simulés. Sur un environnement où l'exploitant l'a autorisé, il déclenche un
**appel réel** de la même requête et voit la réponse obtenue (statut, en-têtes, corps, durée), à
côté du résultat de la simulation.

**Why this priority**: c'est la valeur de la fonctionnalité : passer de « ce qui devrait se
passer » à « ce qui se passe », sans quitter l'UI ni recopier la requête dans un autre outil.

**Independent Test**: sur une Gateway où le mode est activé, saisir une requête qui correspond à une
route, déclencher l'appel réel, et vérifier que la réponse affichée est celle du service.

**Acceptance Scenarios**:

1. **Given** le mode appel réel est activé et une route correspond à la requête, **When**
   l'utilisateur déclenche l'appel réel, **Then** l'UI affiche le statut, les en-têtes, le corps de
   la réponse et la durée de l'appel, à côté du résultat de la simulation.
2. **Given** le mode appel réel est activé, **When** l'utilisateur ouvre le testeur, **Then** un
   sélecteur « Simulation | Appel réel » est proposé, positionné sur Simulation ; basculer sur
   Appel réel n'envoie rien : l'envoi reste un clic explicite sur « Envoyer réellement ».
4. **Given** le mode Appel réel est sélectionné, **When** l'appel aboutit, **Then** seule la
   réponse réelle est affichée (la simulation n'est pas affichée dans ce mode), et la touche
   Entrée ne déclenche jamais d'appel réel.
3. **Given** le service ne répond pas ou renvoie une erreur, **When** l'utilisateur déclenche
   l'appel réel, **Then** l'UI affiche l'erreur (statut d'erreur, délai dépassé, service
   injoignable) de façon lisible, sans casser la page.

---

### User Story 2 - Rester en lecture seule tant que l'exploitant n'a rien activé (Priority: P1)

Un exploitant qui active l'UI en production ne veut pas qu'elle puisse appeler ses services. Sans
configuration explicite, le testeur reste en simulation pure, et rien dans l'UI ne laisse croire
qu'un appel réel est possible.

**Why this priority**: c'est la promesse actuelle du produit (ADR 0004) ; la rompre par défaut
serait une régression de sécurité.

**Independent Test**: démarrer une Gateway avec l'UI activée mais sans activer le mode appel réel,
vérifier qu'aucune action d'appel réel n'est proposée et qu'une tentative directe est refusée.

**Acceptance Scenarios**:

1. **Given** l'UI est activée sans le mode appel réel, **When** l'utilisateur ouvre le testeur,
   **Then** aucune action d'appel réel n'est affichée.
2. **Given** le mode appel réel n'est pas activé, **When** une requête d'appel réel est envoyée
   directement à l'URL correspondante, **Then** elle est refusée et aucun service n'est appelé.

---

### User Story 3 - Comparer simulation et réalité (Priority: P2)

Après un appel réel, l'utilisateur veut savoir si la simulation était juste : la route
effectivement empruntée et la requête effectivement reçue par le service correspondent-elles à ce
que le testeur avait prévu ?

**Why this priority**: utile pour gagner confiance dans la simulation et repérer les filtres non
simulés qui changent le résultat, mais la fonctionnalité a déjà de la valeur sans cette
comparaison.

**Independent Test**: déclencher un appel réel sur une route avec un filtre non simulé et vérifier
que l'UI signale l'écart entre la prévision et la réalité.

**Acceptance Scenarios**:

1. **Given** un appel réel a abouti, **When** l'utilisateur consulte le résultat, **Then** l'UI
   indique la route qui a réellement traité la requête et la signale si elle diffère de la route
   prévue par la simulation.

---

### Edge Cases

- Aucune route ne correspond à la requête : l'appel réel reste possible et montre la réponse de la
  Gateway (en général 404), pour vérifier ce qu'obtient un client.
- Le corps de la réponse est volumineux ou binaire : l'UI en affiche un extrait borné et signale la
  troncature ou le type non affichable.
- Le service met longtemps à répondre : l'appel est interrompu après un délai maximal, et l'UI le
  signale.
- La route passe par un rate limiter ou un circuit breaker : l'appel réel les sollicite comme
  n'importe quel client, et l'UI le rappelle avant l'envoi.
- Le chemin saisi désigne l'UI elle-même : l'appel réel est refusé, pour éviter qu'elle ne
  s'appelle en boucle.
- Une adresse IP de client est saisie : elle ne vaut que pour la simulation ; l'appel réel part du
  serveur lui-même, et le résultat le signale.
- En-têtes sensibles (par exemple `Authorization`) : voir FR-011.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Le mode appel réel DOIT être désactivé par défaut ; il ne s'active que par une
  propriété de configuration explicite, en plus de l'activation de l'UI.
- **FR-002**: Quand le mode est désactivé, le système NE DOIT proposer aucune action d'appel réel
  et DOIT refuser toute demande d'appel réel, sans contacter aucun service.
- **FR-003**: Quand le mode est activé, l'utilisateur DOIT pouvoir déclencher un appel réel à partir
  de la requête saisie dans le testeur (méthode, hôte, chemin, en-têtes).
- **FR-004**: L'appel réel DOIT passer par la Gateway elle-même, comme le ferait un vrai client :
  sélection de route, filtres, rate limiters et circuit breakers réels. Il n'est jamais envoyé
  directement au service.
- **FR-005**: Toutes les méthodes HTTP proposées par le testeur (GET, POST, PUT, PATCH, DELETE,
  HEAD, OPTIONS) DOIVENT être autorisées pour l'appel réel dès que le mode est activé ; l'avertissement de FR-010 rappelle qu'une méthode d'écriture peut modifier des
  données côté service.
- **FR-006**: L'utilisateur DOIT pouvoir saisir un corps de requête pour l'appel réel, pour les
  méthodes qui en portent un (POST, PUT, PATCH, DELETE) ; la simulation, elle, garde un corps vide.
- **FR-007**: Le système DOIT afficher le statut, les en-têtes, un extrait borné du corps et la
  durée de la réponse, à côté du résultat de la simulation.
- **FR-008**: Le système DOIT interrompre un appel réel qui dépasse un délai maximal configurable et
  le signaler à l'utilisateur.
- **FR-009**: L'appel réel DOIT être déclenché par une action explicite de l'utilisateur, jamais
  automatiquement à l'affichage ou à la modification de la saisie.
- **FR-010**: Le système DOIT avertir l'utilisateur, avant l'envoi, que l'appel est réel et peut
  avoir des effets de bord (consommation d'un rate limiter, écriture côté service).
- **FR-011**: Dans le résultat d'un appel réel, le système NE DOIT PAS réafficher en clair les
  valeurs des en-têtes sensibles, envoyés comme reçus (par exemple `Set-Cookie`), et NE DOIT
  jamais les journaliser. La simulation, son résultat et le formulaire du testeur gardent leur
  comportement actuel.
- **FR-012**: Le système DOIT indiquer la route qui a réellement traité la requête quand cette
  information est disponible, et signaler un écart avec la route prévue par la simulation.
- **FR-013**: Tous les textes de la fonctionnalité DOIVENT exister en français et en anglais.
- **FR-014**: Le système DOIT afficher l'URL à laquelle la Gateway a réellement transmis la requête,
  quand elle est connue (ajout du mainteneur, 2026-10-13).

### Key Entities

- **Requête de test** : la saisie du testeur (méthode, hôte, chemin, en-têtes, IP), commune à la
  simulation et à l'appel réel.
- **Résultat d'appel réel** : statut, en-têtes, extrait du corps, durée, route effectivement
  empruntée si connue, ou erreur (délai dépassé, service injoignable).
- **Configuration du mode appel réel** : activation, délai maximal, taille de l'extrait du corps
  affiché.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Sur une Gateway sans configuration du mode appel réel, 100 % des tentatives d'appel
  réel sont refusées et aucun service n'est contacté.
- **SC-002**: Avec le mode activé, un utilisateur obtient la réponse réelle d'une route en une seule
  action depuis le testeur, sans ressaisir sa requête.
- **SC-003**: Un appel dont le service ne répond pas rend la main à l'utilisateur au plus tard au
  délai maximal configuré, avec un message explicite.
- **SC-004**: Les résultats de la simulation restent identiques, que le mode appel réel soit
  activé ou non.

## Assumptions

- Le mode appel réel concerne la page « Testeur » ; le testeur de l'éditeur, qui porte sur une
  route qui n'existe pas encore dans la Gateway, reste en simulation.
- Les adaptateurs d'authentification (OAuth2, Basic, token statique) sont une fonctionnalité
  distincte de la 1.2.0 ; ici, l'utilisateur peut seulement saisir lui-même des en-têtes.
- La protection de l'accès à l'UI reste de la responsabilité de l'application hôte (ADR 0010) ; la
  documentation recommande de ne pas activer le mode appel réel sur une UI non protégée.
- Le délai maximal par défaut est de l'ordre de quelques secondes, et l'extrait du corps est borné
  à quelques dizaines de Ko.
- La simulation reste le comportement par défaut : ce mode complète l'ADR 0004 sans le remplacer,
  par un nouvel ADR issu du plan.
- La page testeur garde ses paramètres dans l'URL, en-têtes compris (ADR 0008) ; la documentation
  déconseille d'y saisir de vrais secrets, l'URL pouvant finir dans l'historique ou être partagée.
- Le corps saisi est borné par la taille maximale des formulaires de Spring (256 Ko par défaut).
