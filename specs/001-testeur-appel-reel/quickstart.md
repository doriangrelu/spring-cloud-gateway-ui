# Guide de validation : mode « appel réel » du testeur

## Prérequis

- Build vert : `./mvnw verify`.
- Gateway d'exemple : `./mvnw install -DskipTests && ./mvnw -f gateway-ui-sample/pom.xml spring-boot:run`.

## Scénario 1 : désactivé par défaut (US2, SC-001)

1. Démarrer la Gateway d'exemple sans configuration supplémentaire.
2. Ouvrir `http://localhost:8080/gateway-ui/tester` : aucun bouton « Envoyer réellement ».
3. `POST /gateway-ui/tester/call` avec `path=/api/users/1` : réponse 404, et aucun appel dans les
   journaux du service.

## Scénario 2 : appel réel (US1, SC-002)

1. Démarrer avec `gateway.ui.tester.real-call.enabled=true` ; le journal affiche l'avertissement
   de démarrage.
2. Dans le testeur, saisir `GET /api/users/1`, lancer la simulation, puis « Envoyer réellement ».
3. Attendu : statut, durée, en-têtes et corps du service, sous le résultat de la simulation.
4. Saisir un `Authorization: Bearer ...` : sa valeur est masquée dans le résultat.

## Scénario 3 : erreurs (US1, SC-003)

1. Avec `timeout=1s`, appeler une route vers un service lent ou arrêté.
2. Attendu : « délai dépassé » ou « service injoignable » en moins de 2 s, la page reste utilisable.

## Scénario 4 : comparaison (US3)

1. Appeler un chemin pris par une route différente de celle prévue (par exemple via un filtre non
   simulé ou une IP saisie pour `RemoteAddr`).
2. Attendu : la route réelle est affichée et l'écart signalé.

## Scénario 5 : garde-fous

1. Appeler `/gateway-ui/tester` depuis le testeur : refusé, aucun envoi.
2. Résultats de la simulation identiques avec et sans le mode (SC-004).

Les tests automatisés correspondants sont listés dans `tasks.md`.
