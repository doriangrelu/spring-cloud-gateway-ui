# 0015. Mode « appel réel » du testeur

- Statut : Accepté
- Date : 2026-10-13
- Tâche : [specs/001-testeur-appel-reel](../../specs/001-testeur-appel-reel/spec.md)

## Contexte

Le testeur simule (ADR 0004) : il dit quelle route est retenue et ce qui serait transmis, mais pas
ce que le service répond, ni l'effet des filtres non simulés. La roadmap 1.2.0 prévoit un mode
« appel réel », désactivé par défaut, qui complète la simulation sans la remplacer.

La spécification a tranché : l'appel passe par la Gateway elle-même, toutes les méthodes sont
autorisées et un corps peut être saisi. Restent trois choix techniques structurants.

## Options envisagées

**Chemin de l'appel**
- A. Requête HTTP en boucle locale vers le serveur qui héberge l'UI, avec l'en-tête `Host` saisi.
- B. Appel en mémoire via le `HttpHandler` de l'application.

**Route réellement empruntée**
- C. `WebFilter` de corrélation, actif seulement quand le mode est activé, qui ne traite que les
  requêtes marquées par l'UI d'un identifiant à usage unique.
- D. `GlobalFilter` de la Gateway.
- E. Ne pas afficher la route réelle.

**Configuration**
- F. Propriétés `gateway.ui.tester.real-call.*` dans `GatewayUiProperties`, ancien constructeur
  conservé et déprécié.
- G. Classe de propriétés séparée.

## Décision

**A**, **C** et **F**.

- A reproduit exactement ce qu'obtient un client (serveur, sécurité de l'hôte, route, filtres,
  rate limiters) ; B court-circuite le serveur et demande de fabriquer une requête à la main.
- C n'entre pas dans la chaîne de filtres des routes, contrairement à D qui apparaîtrait sur chaque
  route et dans la page « Filtres globaux ». Le marqueur est retiré avant la sélection de route : ni
  la Gateway ni le service ne le voient.
- F garde la configuration au même endroit et respecte l'ADR 0008 : le code qui instancie
  `GatewayUiProperties` continue de compiler.

Garde-fous :

- Le mode est désactivé par défaut ; l'URL d'appel est toujours déclarée et répond 404 quand il
  l'est, pour qu'aucune route `Path=/**` ne capte la requête.
- L'appel est une action explicite, précédée d'un avertissement ; un délai maximal et une taille
  maximale d'extrait le bornent ; les en-têtes sensibles sont masqués dans le résultat de l'appel
  réel et jamais journalisés. La simulation et le formulaire du testeur restent inchangés.
- Un chemin sous `gateway.ui.base-path` est refusé (pas d'appel de l'UI par elle-même).
- Au démarrage, un avertissement signale que le mode est activé ; la recommandation de l'ADR 0010
  (protéger l'UI) devient impérative dans la documentation.

## Conséquences

- L'UI peut désormais émettre des requêtes, y compris des écritures, sur décision explicite de
  l'exploitant ; l'ADR 0004 reste la règle par défaut.
- L'adresse IP saisie ne s'applique qu'à la simulation : l'appel réel part de la boucle locale.
- Avec TLS activé sur le serveur, l'appel loopback utilise la confiance par défaut de la JVM ; un
  certificat non reconnu donne une erreur explicite.
- Un `WebFilter` est ajouté à l'application hôte quand le mode est activé ; il est sans effet sur
  les requêtes non marquées.
- Les adaptateurs d'authentification (1.2.0) pourront s'appuyer sur ce mécanisme d'appel.
