#!/usr/bin/env bash
#
# Vérifie le contenu d'un bundle de publication Maven Central avant son envoi.
#
# Usage : check-central-bundle.sh [--no-signatures] <bundle.zip> <artifactId>:<packaging>...
#   ex. : check-central-bundle.sh target/central-publishing/central-bundle.zip \
#           gateway-ui-parent:pom gateway-ui-autoconfigure:jar gateway-ui-spring-boot-starter:jar
#
# Contrôles :
#   - aucun fichier parasite (chemin dont un élément commence par un point, comme les .locks de Maven 3.10, #22) ;
#   - pour chaque artefact attendu : le .pom, et pour un jar le jar principal, les sources et la javadoc ;
#   - une signature .asc pour chaque fichier publié (sauf --no-signatures, pour un bundle construit sans GPG).
#
set -euo pipefail

signatures=true
if [ "${1:-}" = "--no-signatures" ]; then
  signatures=false
  shift
fi

bundle="${1:?Usage : $0 [--no-signatures] <bundle.zip> <artifactId>:<packaging>...}"
shift
if [ ! -f "$bundle" ]; then
  echo "::error::Bundle introuvable : $bundle (le build a échoué avant sa création, voir les logs Maven)"
  exit 1
fi

# Chemins normalisés avec des / (un bundle construit sous Windows utilise des \)
entries=$(unzip -Z1 "$bundle" | tr '\\' '/' | grep -v '/$')
errors=0
fail() { echo "::error::$1"; errors=$((errors + 1)); }

parasites=$(echo "$entries" | grep -E '(^|/)\.' || true)
if [ -n "$parasites" ]; then
  fail "Fichiers parasites dans le bundle :"$'\n'"$parasites"
fi

require() {
  if echo "$entries" | grep -qx "$1"; then
    if [ "$signatures" = true ] && ! echo "$entries" | grep -qx "$1.asc"; then
      fail "Signature manquante : $1.asc"
    fi
  else
    fail "Fichier manquant : $1"
  fi
}

for expected in "$@"; do
  artifact="${expected%%:*}"
  packaging="${expected##*:}"
  pom=$(echo "$entries" | grep -E "/$artifact/[^/]+/$artifact-[^/]+\.pom$" | head -1 || true)
  if [ -z "$pom" ]; then
    fail "Aucun .pom pour $artifact"
    continue
  fi
  base="${pom%.pom}"
  require "$pom"
  if [ "$packaging" = jar ]; then
    require "$base.jar"
    require "$base-sources.jar"
    require "$base-javadoc.jar"
  fi
  echo "✔ $artifact ($packaging) : ${base##*/}"
done

if [ "$errors" -gt 0 ]; then
  echo "Bundle invalide : $errors problème(s)."
  exit 1
fi
echo "Bundle valide : $(echo "$entries" | wc -l | tr -d ' ') fichiers."
