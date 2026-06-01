#!/usr/bin/env bash
#
# Regenere les SVG des diagrammes a partir de leurs sources (.puml / .mmd)
# de ce dossier, via un serveur Kroki local (cf. docker-compose.yml a la
# racine du TP). Outil de maintenance (enseignant).
#
# Usage :
#   docker compose up -d          # depuis la racine du TP (ou reutiliser
#                                 # le Kroki des CM s'il tourne deja sur :8000)
#   ./.github/assets/render-diagrams.sh
#   docker compose down           # (optionnel)
#
# Variable d'environnement : KROKI_URL (defaut http://localhost:8000).

set -euo pipefail

KROKI_URL="${KROKI_URL:-http://localhost:8000}"
ASSETS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

render() { # $1 = fichier source, $2 = type kroki (plantuml|mermaid)
  local src="$1" type="$2" out="${1%.*}.svg"
  printf '  %-9s %s -> %s\n' "$type" "$(basename "$src")" "$(basename "$out")"
  curl -sf -X POST -H 'Content-Type: text/plain' \
    --data-binary @"$src" "$KROKI_URL/$type/svg" -o "$out"
  # PlantUML renvoie ses erreurs comme une IMAGE en HTTP 200 : on inspecte
  # donc le contenu du SVG, pas seulement le code HTTP.
  if grep -qE 'An error has occured|ClassNotFoundException|Syntax Error' "$out"; then
    echo "  ERREUR : Kroki a renvoye une image d'erreur pour $(basename "$src")" >&2
    exit 1
  fi
  if [ "$type" = mermaid ]; then
    # Kroki-mermaid rend un fond transparent : on insere un rectangle blanc
    # en premier enfant du <svg> pour rester lisible sur le theme sombre de
    # GitHub (les diagrammes PlantUML ont deja un fond blanc).
    sed -i -E '0,/<svg[^>]*>/ s//&<rect width="100%" height="100%" fill="#ffffff"\/>/' "$out"
    # Mermaid rend le fond (blanc) des labels d'aretes a opacity:0.5 : le trait
    # de la fleche transparait alors sous le texte. On le rend opaque pour que
    # les labels restent lisibles.
    sed -i 's/opacity:0\.5/opacity:1/g' "$out"
  fi
}

echo "Rendu des diagrammes via $KROKI_URL"
shopt -s nullglob
found=0
for f in "$ASSETS_DIR"/*.puml; do render "$f" plantuml; found=1; done
for f in "$ASSETS_DIR"/*.mmd; do render "$f" mermaid; found=1; done
[ "$found" -eq 1 ] || {
  echo "Aucune source .puml/.mmd trouvee dans $ASSETS_DIR" >&2
  exit 1
}
echo "Termine."
