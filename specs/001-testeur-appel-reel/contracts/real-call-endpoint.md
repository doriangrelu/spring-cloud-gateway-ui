# Contrat : point d'entrée de l'appel réel

## `POST {base-path}/tester/call`

Toujours déclaré dans la `RouterFunction` de l'UI, que le mode soit activé ou non (voir R6).

**Requête** : `application/x-www-form-urlencoded`, avec le jeton CSRF quand Spring Security est
présent (même mécanisme que l'API de l'éditeur).

| Paramètre | Obligatoire | Description |
|---|---|---|
| `method` | non | Méthode HTTP, `GET` par défaut. Toutes les méthodes sont acceptées. |
| `path` | oui | Chemin et query string. |
| `host` | non | Valeur de l'en-tête `Host`, `localhost` par défaut. |
| `headers` | non | Un en-tête par ligne, `Nom: valeur`. |
| `remoteAddress` | non | Utilisée par la simulation seulement ; signalée si différente de la boucle locale. |
| `body` | non | Corps envoyé pour POST, PUT, PATCH, DELETE. |

**Réponses**

| Cas | Statut | Contenu |
|---|---|---|
| Mode désactivé | 404 | Page « introuvable » de l'UI ; aucun appel. |
| Requête invalide ou chemin sous `base-path` | 200 | Fragment de résultat avec le message d'erreur ; aucun appel. |
| Appel effectué (quelle que soit la réponse du service) | 200 | Fragment `testerCallResult` : statut, durée, en-têtes masqués, extrait du corps, route réelle et badge de comparaison avec la simulation (calculée, non affichée). |
| Délai dépassé, service injoignable | 200 | Fragment avec l'erreur et la durée. |

Avec `HX-Request`, seul le fragment est renvoyé ; sans, la page du testeur complète, en mode
Appel réel, avec le résultat de l'appel (navigation sans JavaScript).

## Page `GET {base-path}/tester` (existante)

- Paramètres d'URL inchangés (API publique, ADR 0008) : ni le mode ni le corps n'y figurent.
- Quand le mode est activé : sélecteur « Simulation | Appel réel » dans le formulaire, sur
  Simulation à l'ouverture. En mode Appel réel : cadre et bandeau d'avertissement, champ « Corps »
  dans les options avancées, bouton « Envoyer réellement » (de type `button` : la touche Entrée ne
  déclenche jamais d'appel réel), zone de résultat de l'appel à la place de la simulation.
- Quand il est désactivé : rien de tout cela n'apparaît, la page est inchangée.
- Sans JavaScript ajouté (ADR 0011) : boutons radio stylés, visibilité par CSS (`:has()`).

## En-tête interne `X-Gateway-UI-Call`

- Posé par l'UI sur la requête en boucle locale ; retiré par le `WebFilter` de corrélation avant
  la sélection de route : ni la Gateway ni le service ne le voient.
- N'existe que si le mode est activé. Hors API publique.
