---
name: Release
about: Checklist de publication d'une version
title: "Release X.Y.Z"
labels: "type: release"
---
## Avant
- [ ] Issues de la milestone fermées, ou reportées à une autre milestone
- [ ] CI verte sur main
- [ ] Essai à blanc du workflow de release réussi (`gh workflow run release.yml -f version=X.Y.Z`)
- [ ] CHANGELOG : section [X.Y.Z] datée, liens de bas de page à jour
- [ ] Table de compatibilité du README vérifiée
- [ ] Captures régénérées si l'UI a changé (docs/screenshots.sh)

## Publication
- [ ] Tag vX.Y.Z poussé
- [ ] Workflow Release vert
- [ ] Artefacts visibles sur Maven Central
- [ ] Release GitHub créée avec les notes

## Après
- [ ] `main` passée à la version SNAPSHOT suivante (automatique pour une version finale, à vérifier ; rien après une pré-release)
- [ ] Milestone fermée
- [ ] docs/roadmap.md mise à jour
- [ ] Annonce publiée
