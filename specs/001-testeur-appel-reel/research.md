# Recherche : mode « appel réel » du testeur

Chaque décision suit le format : Décision, Raison, Alternatives écartées. Les décisions marquées
**ADR** sont structurantes et sont tracées dans l'ADR 0015 (*Proposé*).

## R1. Comment faire passer l'appel par la Gateway elle-même (FR-004) — ADR

- **Décision** : l'UI envoie une vraie requête HTTP en boucle locale (*loopback*) au serveur qui
  l'héberge : `127.0.0.1`, port effectif du serveur web (lu une fois le serveur démarré, donc
  correct avec `server.port=0`), schéma `https` si `server.ssl.enabled`. L'en-tête `Host` est celui
  saisi dans le testeur, pour que les prédicats `Host` se comportent comme pour un vrai client.
- **Raison** : c'est la seule façon d'obtenir exactement ce qu'obtient un client : serveur Netty,
  `WebFilter` de l'application hôte (sécurité, CORS), sélection de route, filtres, rate limiters et
  circuit breakers réels.
- **Alternatives écartées** :
  - Appel en mémoire via le `HttpHandler` de l'application : il faut fabriquer une
    `ServerHttpRequest` (les implémentations prêtes sont dans `spring-test`), et on court-circuite
    le serveur ; moins fidèle, plus de code.
  - Appel direct du service avec la requête transformée par la simulation : écarté par la
    clarification Q1.

## R2. Client HTTP

- **Décision** : un `WebClient` dédié, construit sur le `ReactorClientHttpConnector` (Reactor Netty,
  déjà présent avec la Gateway), **sans** utiliser le `WebClient.Builder` de l'application hôte. Pas
  de suivi des redirections, pas de cookies de session de l'UI transmis.
- **Raison** : aucune nouvelle dépendance ; les personnalisations du builder de l'hôte (filtres
  OAuth2, en-têtes ajoutés) fausseraient l'appel, qui doit ressembler à celui d'un client nu.
- **Alternatives écartées** : `java.net.http.HttpClient` (bloquant ou à adapter, sans intérêt ici) ;
  le builder de l'hôte (voir ci-dessus).

## R3. Adresse IP saisie

- **Décision** : l'IP saisie ne s'applique qu'à la simulation. L'appel réel part de la boucle
  locale ; le résultat le signale quand l'IP saisie diffère de `127.0.0.1`.
- **Raison** : on ne peut pas usurper l'adresse source d'une connexion TCP ; ajouter
  `X-Forwarded-For` modifierait la requête et ne serait lu que si la Gateway le demande.
- **Alternatives écartées** : en-tête `X-Forwarded-For` automatique (non fidèle) ; refus de l'appel
  si une IP est saisie (trop restrictif).

## R4. Route réellement empruntée (FR-012, US3) — ADR

- **Décision** : un `WebFilter` de corrélation, déclaré **uniquement** quand le mode est activé, en
  tête de chaîne. Il ne réagit qu'aux requêtes portant l'en-tête `X-Gateway-UI-Call` avec un
  identifiant que l'UI vient de générer (UUID aléatoire, inscrit dans un registre en mémoire borné,
  expirant au délai maximal). Il retire l'en-tête avant de passer la main, puis, juste avant
  l'écriture de la réponse (`beforeCommit`), lit l'attribut `GATEWAY_ROUTE_ATTR` de l'échange et
  inscrit l'identifiant de la route dans le registre. Un identifiant inconnu est ignoré (l'en-tête
  est quand même retiré).
- **Raison** : l'attribut est posé par `RoutePredicateHandlerMapping` sur l'échange, mais n'est pas
  visible par le client. Un `WebFilter` n'entre pas dans la chaîne de filtres des routes : la page
  « Filtres globaux » et la simulation restent inchangées. `beforeCommit` garantit que la route est
  connue avant que le client reçoive la réponse : pas de course.
- **Alternatives écartées** :
  - `GlobalFilter` : il apparaîtrait dans la chaîne de chaque route et fausserait l'affichage.
  - En-tête de réponse ajouté par la Gateway : modifie ce que voient tous les clients.
  - Pas de route réelle (US3 abandonnée) : possible si l'ADR est refusé ; US3 est P2.

## R5. Requêtes vers l'UI elle-même

- **Décision** : l'appel réel est refusé si le chemin saisi est sous `gateway.ui.base-path`.
- **Raison** : sans cela, un appel vers `/gateway-ui/tester/call` relancerait un appel réel, en
  boucle ; et l'UI n'est pas une route de la Gateway, le tester n'a pas de sens.
- **Alternatives écartées** : limite de profondeur par en-tête (plus complexe, même résultat).

## R6. Point d'entrée désactivé (FR-002)

- **Décision** : l'URL d'appel réel est **toujours** déclarée dans la `RouterFunction` de l'UI ;
  quand le mode est désactivé, elle répond 404 sans rien appeler.
- **Raison** : si elle n'était pas déclarée, une route `Path=/**` de la Gateway capterait la requête
  et la transmettrait à un service : exactement ce que FR-002 interdit (même logique que l'ADR 0005).
- **Alternatives écartées** : ne pas déclarer l'URL (voir ci-dessus).

## R7. Lecture bornée du corps de réponse

- **Décision** : lecture du flux de `DataBuffer` jusqu'à `max-body-size` (64 Ko par défaut), puis
  annulation ; les tampons sont libérés. Le corps est affiché si le `Content-Type` est textuel
  (`text/*`, `*json`, `*xml`, formulaire), avec le charset déclaré (UTF-8 à défaut) ; sinon l'UI
  indique « contenu binaire, non affiché ». La troncature est signalée.
- **Raison** : borne la mémoire et le temps, quel que soit le service.
- **Alternatives écartées** : `bodyToMono(String)` (non borné), limite globale du codec (échec au
  lieu d'extrait).

## R8. Délai maximal (FR-008)

- **Décision** : `gateway.ui.tester.real-call.timeout`, 5 s par défaut, appliqué à l'ensemble de
  l'échange (`Mono.timeout`) ; au-delà, l'appel est annulé et l'UI affiche « délai dépassé ».
- **Raison** : rend la main à l'utilisateur (SC-003), même si le service envoie le corps lentement.

## R9. En-têtes sensibles (FR-011)

- **Décision** : une liste de noms sensibles (`Authorization`, `Proxy-Authorization`, `Cookie`,
  `Set-Cookie`, et tout nom contenant `token`, `secret`, `password`, `api-key`, `credential`) dans
  le paquet de l'appel réel. Leurs valeurs sont masquées dans le **résultat de l'appel réel**
  uniquement (requête envoyée et réponse) ; la simulation, son résultat et le formulaire restent
  à l'identique. Aucun en-tête n'est journalisé : le `HttpClient` dédié n'active pas le *wiretap*
  de Reactor Netty, quelle que soit la configuration de journalisation de l'hôte.
- **Raison** : `SecretDetector` vit dans `editor`, que ce paquet ne peut pas utiliser (sens des
  dépendances). La liste est courte et propre à l'affichage d'en-têtes HTTP.
- **Alternatives écartées** : déplacer `SecretDetector` (refactor hors périmètre).

## R10. Propriétés et compatibilité de `GatewayUiProperties` — ADR

- **Décision** : nouvelles propriétés `gateway.ui.tester.real-call.enabled` (`false`),
  `.timeout` (`5s`), `.max-body-size` (`64KB`), portées par des records imbriqués
  `GatewayUiProperties.Tester` et `Tester.RealCall`. L'ajout d'un composant change le constructeur
  canonique du record public : l'ancien constructeur est conservé, déprécié, et délègue avec les
  valeurs par défaut ; le canonique porte `@ConstructorBinding` pour que la liaison Spring Boot
  reste sans ambiguïté.
- **Raison** : ADR 0008 garantit la classe `GatewayUiProperties` ; un code qui l'instancie
  (tests de l'utilisateur) doit continuer à compiler en 1.2.0.
- **Alternatives écartées** : classe de propriétés séparée (`gateway.ui.tester.*` éclaté entre deux
  classes publiques) ; casser le constructeur (contraire à l'ADR 0008).

## R11. Garde-fous au démarrage

- **Décision** : quand le mode est activé, un message `WARN` au démarrage rappelle que l'UI peut
  envoyer de vraies requêtes, toutes méthodes comprises ; il s'ajoute à l'avertissement existant si
  Spring Security est absent (ADR 0010).
- **Raison** : la clarification Q2 autorise toutes les méthodes ; l'exploitant doit le savoir.

## R12. Tests sans dépendance supplémentaire

- **Décision** : le test d'intégration déclare un « service » de test servi par une
  `RouterFunction` de l'application de test, sur le même serveur (port aléatoire), et des routes de
  la Gateway qui y mènent. Une configuration avec le mode activé, une sans.
- **Raison** : aucun serveur simulé externe (MockWebServer, WireMock) à ajouter.
