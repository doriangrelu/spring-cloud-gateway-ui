# Modèle de données : mode « appel réel » du testeur

Tous les types sont des records immuables, dans `internal.*` sauf la configuration.

## Configuration (API publique, `GatewayUiProperties`)

| Propriété | Type | Défaut | Règle |
|---|---|---|---|
| `gateway.ui.tester.real-call.enabled` | `boolean` | `false` | Sans effet si `gateway.ui.enabled` est faux. |
| `gateway.ui.tester.real-call.timeout` | `Duration` | `5s` | Strictement positif. |
| `gateway.ui.tester.real-call.max-body-size` | `DataSize` | `64KB` | Strictement positif ; borne l'extrait affiché. |

Records : `GatewayUiProperties.Tester(RealCall realCall)` et `GatewayUiProperties.RealCall(boolean
enabled, Duration timeout, DataSize maxBodySize)` (implémentation : `RealCall` est au même niveau que
`Tester`, les noms de propriétés sont inchangés). Les valeurs absentes ou invalides sont remplacées par les
défauts dans le constructeur compact, comme pour `basePath`.

## Requête de test (`internal.tester.TestRequest`, existant, étendu)

| Champ | Nouveau | Règle |
|---|---|---|
| `method`, `pathAndQuery`, `host`, `headers`, `remoteAddress` | non | Inchangés ; partagés par la simulation et l'appel réel. |
| `body` | oui | Texte libre, vide par défaut. Ignoré par la simulation (corps vide, ADR 0004). Envoyé seulement pour POST, PUT, PATCH, DELETE. |

Le paramètre d'URL `body` n'est **pas** ajouté à la page GET du testeur : le corps n'a de sens que
pour l'appel réel, envoyé en POST (pas de corps dans une URL partagée).

## Appel réel (`internal.realcall`, nouveau paquet)

- **`RealCallRequest`** : la `TestRequest` validée. Refusée si le chemin est sous `base-path` (R5)
  ou si la requête est invalide (`TestRequest.validate`). L'identifiant de corrélation n'en fait
  pas partie : `RealCaller` le génère au moment de l'envoi (US3).
- **`RealCallResult(error, status, headers, body, duration, routing)`** : issue de l'appel, en un seul
  record (plus simple à rendre dans JTE qu'une hiérarchie) ; `failed()` vrai si `error` est posé
  (délai dépassé, connexion refusée, erreur TLS, refus). `headers` déjà masqués (R9) ; `routing`
  (`CallRegistry.Routing(routeId, targetUrl)`) nul si inconnu.
- **`BodyExcerpt(String text, long receivedBytes, boolean truncated, boolean binary)`** : `text`
  vide si `binary`.
- **`RouteComparison(String simulatedRouteId, String actualRouteId)`** : `matches()` vrai si les deux
  sont égaux ; affichée seulement si `actualRouteId` est connu (US3).

## Registre de corrélation (`internal.realcall.CallRegistry`)

- Associe un identifiant d'appel (UUID) à la route effectivement empruntée et à l'URL transmise au
  service (`GATEWAY_REQUEST_URL_ATTR`), affichée dans le résultat (demande du mainteneur).
- Cycle de vie : `register(id)` avant l'envoi → `record(id, routing)` par le `WebFilter` au
  `beforeCommit` → `take(id)` par l'UI à la réception ; toute entrée expire après `timeout`.
- Borné (taille maximale nommée en constante) ; un identifiant inconnu n'est jamais inscrit.

## Transitions d'un appel

```text
saisie ─► validation ─┬─ invalide / sous base-path / mode désactivé ─► refus (pas d'envoi)
                      └─ valide ─► envoi loopback ─┬─ réponse ─► extrait borné ─► Response
                                                   └─ délai / erreur réseau ───► Failure
```
