#!/usr/bin/env bash
#
# Regenere les captures "Resultat attendu" des apps GUI du TP3 (branche
# solution) dans .github/assets/apercu-*.png.
#
# Outil de MAINTENANCE (enseignant) : a relancer apres modification des exercices.
#
# Prerequis : xvfb (xvfb-run) + ImageMagick (import, convert).
# Usage (depuis la racine du TP) :
#   ./.github/assets/capture-screenshots.sh            # toutes les captures
#   ./.github/assets/capture-screenshots.sh ex4        # une seule
#
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"
ASSETS=.github/assets
mkdir -p "$ASSETS"
SCREEN="-screen 0 1366x900x24"
WAIT=24

declare -A APP=(
  [lanceur]="tp3.javafx/fr.univ_amu.iut.App|apercu-lanceur"
  [ex1]="tp3.javafx/fr.univ_amu.iut.exercice1.PremiereVueFXML|apercu-ex1-premiere-vue"
  [ex2]="tp3.javafx/fr.univ_amu.iut.exercice2.CompteurFXML|apercu-ex2-compteur"
  [ex3]="tp3.javafx/fr.univ_amu.iut.exercice3.FormulaireConnexionFXML|apercu-ex3-formulaire"
  [ex4]="tp3.javafx/fr.univ_amu.iut.exercice4.CoquilleAccueil|apercu-ex4-coquille"
  [ex5]="tp3.javafx/fr.univ_amu.iut.exercice5.SiteCarteDemo|apercu-ex5-sitecarte"
  [ex6]="tp3.javafx/fr.univ_amu.iut.exercice6.VueAccueil|apercu-ex6-vueaccueil"
  [ex7]="tp3.javafx/fr.univ_amu.iut.exercice7.Qualification|apercu-ex7-qualification"
  [bonus8]="tp3.javafx/fr.univ_amu.iut.bonus8.ThemeToggle|apercu-bonus8-theme"
  [bonus8sombre]="tp3.javafx/fr.univ_amu.iut.bonus8.ThemeToggle|apercu-bonus8-theme-sombre"
  [bonus9]="tp3.javafx/fr.univ_amu.iut.bonus9.CoquilleSceneBuilder|apercu-bonus9-scenebuilder"
  [bonus10]="tp3.javafx/fr.univ_amu.iut.bonus10.OthelloMain|apercu-bonus10-othello"
)
# bonus8 = theme clair (par defaut), bonus8sombre = apres clic sur "Mode sombre".
ORDER=(lanceur ex1 ex2 ex3 ex4 ex5 ex6 ex7 bonus8 bonus8sombre bonus9 bonus10)

# $1 = mainClass, $2 = nom de sortie, $3 = cle.
# Certaines apps sont pilotees via xdotool avant la capture (ex6 : on clique
# "+ Nouveau site" pour peupler la liste). Variables passees par l'environnement
# -> bash -c en quotes simples (pas d'echappement).
capture() {
  local main="$1" out="$2" key="${3:-}"
  MAIN="$main" OUT="$out" KEY="$key" WAITS="$WAIT" \
    xvfb-run -a --server-args="$SCREEN" bash -c '
      ./mvnw -q -Djavafx.mainClass="$MAIN" javafx:run >"/tmp/cap-$OUT.log" 2>&1 &
      M=$!
      sleep "$WAITS"
      if [ "$KEY" = ex6 ] && command -v xdotool >/dev/null 2>&1; then
        # Clique "+ Nouveau site" (en haut a droite) 3 fois pour peupler la liste.
        eval "$(xdotool search --class iut.exercice6.VueAccueil getwindowgeometry --shell 2>/dev/null | grep -E "^(X|Y|WIDTH)=")"
        if [ -n "${X:-}" ]; then
          for i in 1 2 3; do xdotool mousemove $((X + WIDTH - 75)) $((Y + 38)) click 1; sleep 0.4; done
        fi
      fi
      if [ "$KEY" = bonus8sombre ] && command -v xdotool >/dev/null 2>&1; then
        # Clique le ToggleButton "Mode sombre" (en haut a droite) pour basculer le thème.
        eval "$(xdotool search --class iut.bonus8.ThemeToggle getwindowgeometry --shell 2>/dev/null | grep -E "^(X|Y|WIDTH)=")"
        if [ -n "${X:-}" ]; then
          xdotool mousemove $((X + WIDTH - 70)) $((Y + 30)) click 1; sleep 0.6
        fi
      fi
      import -window root "/tmp/$OUT-raw.png" 2>/dev/null
      ap=$(pgrep -f "enable-native-access=javafx.graphics --module-path" | head -1)
      [ -n "$ap" ] && kill -9 "$ap" 2>/dev/null
      kill -9 "$M" 2>/dev/null
      exit 0
    '
  convert "/tmp/$out-raw.png" -trim +repage "$ASSETS/$out.png"
  echo "  $out.png  ($(identify -format '%wx%h' "$ASSETS/$out.png" 2>/dev/null))"
}

echo "Pre-compilation..."
./mvnw -q compile

if [ "$#" -ge 1 ]; then
  echo "Captures demandees : $* -> $ASSETS/"
  for k in "$@"; do
    entry="${APP[$k]:-}"
    if [ -z "$entry" ]; then echo "Cle inconnue : $k (attendu : ${ORDER[*]})" >&2; continue; fi
    capture "${entry%%|*}" "${entry##*|}" "$k"
  done
else
  echo "Toutes les captures -> $ASSETS/"
  for k in "${ORDER[@]}"; do
    entry="${APP[$k]}"
    capture "${entry%%|*}" "${entry##*|}" "$k"
  done
fi

pgrep -f "enable-native-access=javafx.graphics --module-path" | xargs -r kill -9 2>/dev/null
echo "Termine."
