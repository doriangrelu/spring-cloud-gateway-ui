#!/usr/bin/env bash
#
# Régénère les captures de docs/images, en thème clair et sombre, depuis la Gateway d'exemple.
# Les captures sont prises en français, la langue de la documentation (paramètre lang=fr).
#
# Prérequis : la Gateway d'exemple tourne sur http://localhost:8080 (voir gateway-ui-sample/README.md)
# et Chrome ou Chromium est installé.
#
# Usage : docs/screenshots.sh [url-de-base]
#
set -euo pipefail

BASE_URL="${1:-http://localhost:8080/gateway-ui}"
OUTPUT_DIR="$(cd "$(dirname "$0")" && pwd)/images"
CHROME="${CHROME:-$(command -v google-chrome || command -v chromium || command -v chrome || echo "/c/Program Files/Google/Chrome/Application/chrome.exe")}"
PROFILE_DIR="$(mktemp -d)"
trap 'rm -rf "$PROFILE_DIR"' EXIT

# Sous Git Bash, Chrome attend un chemin Windows
native_path() {
  if command -v cygpath >/dev/null 2>&1; then cygpath -w "$1"; else echo "$1"; fi
}

# capture <nom> <hauteur> <chemin de la page>
capture() {
  local name="$1" height="$2" page="$3"
  for theme in light dark; do
    local scheme=1
    [ "$theme" = dark ] && scheme=0
    "$CHROME" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 \
      --user-data-dir="$(native_path "$PROFILE_DIR")" --virtual-time-budget=3000 \
      --window-size=1280,"$height" --blink-settings=preferredColorScheme="$scheme" \
      --screenshot="$(native_path "$OUTPUT_DIR/$name-$theme.png")" "$BASE_URL$page" >/dev/null 2>&1
  done
  echo "✔ $name"
}

mkdir -p "$OUTPUT_DIR"
capture routes 500 "/routes?lang=fr"
capture route-detail 1310 "/routes/users?lang=fr"
capture tester 1030 "/tester?method=GET&host=localhost&path=%2Fapi%2Fusers%2Flegacy%2F42&lang=fr"
capture services 640 "/services?lang=fr"
capture global-filters 660 "/filters?lang=fr"
capture editor 1040 "/editor?lang=fr"
capture catalog 1100 "/catalog?lang=fr"
