# Publier une version

Les versions sont publiées sur **Maven Central** par le workflow GitHub Actions [`release.yml`](.github/workflows/release.yml), déclenché par le push d'un tag `vX.Y.Z`. Personne ne publie depuis son poste.

Sont publiés : `gateway-ui-parent`, `gateway-ui-autoconfigure` et `gateway-ui-spring-boot-starter`. Le module `gateway-ui-sample` ne l'est jamais.

## Mise en place (une seule fois)

### 1. Namespace Maven Central

1. Créez un compte sur [central.sonatype.com](https://central.sonatype.com) en vous connectant **avec GitHub** (compte `doriangrelu`).
2. Dans *Namespaces*, vérifiez que `io.github.doriangrelu` est bien **Verified**. La connexion GitHub le vérifie automatiquement ; sinon, suivez la procédure affichée (création d'un dépôt temporaire au nom indiqué).
3. Dans *Account → Generate User Token*, générez un token. Il fournit un **username** et un **password** dédiés à la publication, distincts de vos identifiants.

### 2. Clé GPG de signature

Maven Central exige que chaque artefact soit signé.

```bash
gpg --full-generate-key
```

Choisissez RSA 4096 et une passphrase solide. Relevez ensuite l'identifiant de la clé :

```bash
gpg --list-secret-keys --keyid-format=long
```

Publiez la clé publique, pour que Central puisse vérifier les signatures :

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys <ID_DE_LA_CLE>
```

Exportez enfin la clé privée, à copier dans le secret GitHub `GPG_PRIVATE_KEY` :

```bash
gpg --armor --export-secret-keys <ID_DE_LA_CLE>
```

### 3. Secrets GitHub

Dans *Settings → Secrets and variables → Actions* du dépôt :

| Secret | Valeur |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Username du user token Central |
| `MAVEN_CENTRAL_PASSWORD` | Password du user token Central |
| `GPG_PRIVATE_KEY` | Clé privée exportée en ASCII (`-----BEGIN PGP PRIVATE KEY BLOCK-----` ...) |
| `GPG_PASSPHRASE` | Passphrase de la clé |

Le workflow tourne dans l'environnement GitHub `spring-cloud-gateway-ui-action` : les secrets peuvent être déclarés au niveau du dépôt ou de cet environnement. Vous pouvez y ajouter une règle de protection (*Required reviewers*) pour valider chaque publication à la main. Le workflow vérifie la présence des 4 secrets avant toute autre étape.

## Publier

Exemple pour la version `0.1.0`.

**1. Vérifier que `main` est vert** dans la CI.

**Puis lancer un essai à blanc**, surtout si le workflow, les secrets ou les actions GitHub ont changé depuis la dernière release. Dans l'onglet *Actions*, choisissez le workflow *Release*, puis *Run workflow*, et saisissez la version à venir. Ou, avec le CLI GitHub :

```bash
gh workflow run release.yml -f version=0.1.0
```

L'essai à blanc construit les artefacts, les signe avec les vrais secrets et prépare le passage de `main` à la version SNAPSHOT suivante, **sans rien publier ni pousser** : ni Maven Central, ni tag, ni release GitHub, ni commit.

**2. Mettre à jour le changelog.** Dans [CHANGELOG.md](CHANGELOG.md), renommez la section `[Unreleased]` en version datée, et ajoutez une section `[Unreleased]` vide au-dessus :

```markdown
## [Unreleased]

## [0.1.0] - 2026-10-15

### Ajouté
- ...
```

Mettez aussi à jour les liens en bas du fichier :

```markdown
[Unreleased]: https://github.com/doriangrelu/spring-cloud-gateway-ui/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/doriangrelu/spring-cloud-gateway-ui/releases/tag/v0.1.0
```

**3. Mettre à jour la [table de compatibilité](README.md#compatibilité)** si les versions de Spring ont changé.

**4. Commiter, tagger et pousser :**

```bash
git commit -am "docs: changelog 0.1.0"
```

```bash
git tag -a v0.1.0 -m "Release 0.1.0"
```

```bash
git push origin main v0.1.0
```

**5. Suivre le workflow *Release*** dans l'onglet *Actions*. Il enchaîne :

1. la vérification que `CHANGELOG.md` contient une section `## [0.1.0]` (sinon il échoue avant toute publication) ;
2. le passage de tous les POM en version `0.1.0`, sur le runner uniquement : rien n'est commité ;
3. le build complet, avec licences, Checkstyle et tests ;
4. la création des jars sources et javadoc, la signature GPG et l'envoi sur Central ;
5. l'attente de la publication effective sur Central (`waitUntil=published`, en général 10 à 30 minutes) ;
6. la création de la **release GitHub**, dont les notes sont extraites de la section du changelog ;
7. pour une version finale uniquement, le passage de `main` à la version mineure suivante : un commit `build: passage en 0.2.0-SNAPSHOT` est poussé directement sur `main`.

Les artefacts apparaissent sur [central.sonatype.com](https://central.sonatype.com/namespace/io.github.doriangrelu) dès la fin du workflow. Leur indexation par la recherche Maven peut prendre quelques heures de plus.

**6. Récupérer le passage en SNAPSHOT** avec un `git pull`. Le workflow ne le fait que si `main` est encore sur la SNAPSHOT de la version publiée (`0.1.0-SNAPSHOT` ici) : une version déjà ajustée à la main est conservée. Il n'y a aucun passage après une version de pré-release (`1.0.0-RC1`, `1.1.0-M1`) : `main` reste sur la SNAPSHOT en cours.

Pour préparer une version de correctif plutôt que la mineure suivante, ajustez la version à la main :

```bash
./mvnw versions:set -DnewVersion=0.1.1-SNAPSHOT -DgenerateBackupPoms=false -DprocessAllModules=true
```

## Choisir le numéro de version

On suit le [versionnage sémantique](https://semver.org/lang/fr/) :

- **correctif** (`0.1.1`) : corrections sans changement de comportement attendu ;
- **mineure** (`0.2.0`) : nouvelles fonctionnalités. En `0.x`, une mineure peut aussi casser la compatibilité, à signaler dans *Modifié* ;
- **majeure** (`1.0.0`, puis `2.0.0`) : changements incompatibles, une fois l'API stabilisée.

Une nouvelle génération de Spring Boot ou Spring Cloud qui casse la compatibilité implique au minimum une version mineure.

## En cas de problème

| Symptôme | Cause probable |
|---|---|
| `CHANGELOG.md n'a pas de section` | La section `## [X.Y.Z]` manque ou ne correspond pas exactement au tag. Corrigez, supprimez le tag (`git push --delete origin vX.Y.Z`), puis recréez-le. |
| `401 Unauthorized` au déploiement | Token Central expiré ou mal copié dans les secrets. |
| `gpg: signing failed` | `GPG_PRIVATE_KEY` incomplète (il faut tout le bloc ASCII) ou passphrase erronée. |
| Validation Central : *Invalid signature* | Clé publique non publiée sur un serveur de clés, ou pas encore propagée : patientez puis relancez le job. |
| Validation Central : *Missing javadoc/sources* | Le profil `release` n'a pas été activé (`-P release`). |
| Échec après publication | Une version publiée sur Central est **définitive** : elle ne peut être ni supprimée ni republiée. Corrigez et publiez la version suivante. |

Pour diagnostiquer un problème de secrets ou de signature sans risque, lancez un essai à blanc (voir [Publier](#publier)).

Le profil `release` se vérifie aussi en local, sans signer ni publier :

```bash
./mvnw -P release -pl '!gateway-ui-sample' -Dgpg.skip=true verify
```
