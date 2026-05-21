package fr.univ_amu.iut.bonus10;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

/**
 * Bonus 10 - étape 3 : plateau de jeu complet (modèle + logique).
 *
 * <p>L'othellier est un composant Java auto-suffisant : il étend {@link GridPane} et instancie les
 * 64 {@link Case} dans son constructeur. Toute la logique du jeu (validité d'un coup, capture dans
 * les huit directions, fin de partie) est encapsulée ici, ce qui en fait un beau cas d'usage MVC :
 * la vue FXML n'a qu'à inclure cet othellier, le contrôleur n'aura qu'à câbler quelques bindings
 * sur les propriétés exposées (étape 4).
 *
 * <p><b>Méthodes fournies :</b> le moteur de capture ({@link #casesCapturable(Case)} et {@link
 * #casesCapturable(Case, Point2D)}) ainsi que {@link #estIndicesValides(int, int)} sont livrés tels
 * quels. Ils parcourent les huit directions pour identifier les pions adverses encadrés. Tout le
 * reste (initialisation, démarrage de partie, gestion du tour) s'appuie dessus et est à votre
 * charge.
 */
public class Othellier extends GridPane {

  /** Les huit directions de propagation (horizontales, verticales, diagonales). */
  private static final Point2D[] DIRECTIONS = {
    new Point2D(1, 0),
    new Point2D(1, 1),
    new Point2D(0, 1),
    new Point2D(-1, 1),
    new Point2D(-1, 0),
    new Point2D(-1, -1),
    new Point2D(0, -1),
    new Point2D(1, -1)
  };

  /** Taille du plateau (8x8 dans la version standard du jeu). */
  public static final int TAILLE = 8;

  // TODO bonus 10 étape 3.1 : déclarer les données membres privées suivantes :
  // - cases : une matrice Case[TAILLE][TAILLE] qui représente le plateau de jeu
  // - joueurCourant : un ObjectProperty<Joueur> initialisé à Joueur.NOIR (NOIR
  // commence toujours)
  // - partieTerminee : un BooleanProperty initialisé à false
  private final Case[][] cases = new Case[TAILLE][TAILLE];

  private final ObjectProperty<Joueur> joueurCourant =
      new SimpleObjectProperty<>(this, "joueurCourant", Joueur.NOIR);

  private final BooleanProperty partieTerminee =
      new SimpleBooleanProperty(this, "partieTerminee", false);

  /**
   * Gestionnaire d'événement partagé par toutes les cases du plateau.
   *
   * <p>Une seule instance est réutilisée pour les 64 boutons : c'est le motif courant en JavaFX
   * pour ne pas multiplier inutilement les écouteurs.
   */
  private final EventHandler<ActionEvent> caseListener =
      event -> {
        Case caseSelectionnee = (Case) event.getSource();
        if (estPositionJouable(caseSelectionnee)) {
          jouer(caseSelectionnee);
        }
      };

  /**
   * Construit un othellier neuf : applique les contraintes de la grille, instancie les 64 cases,
   * branche l'écouteur et démarre une nouvelle partie.
   */
  public Othellier() {
    setHgap(1);
    setVgap(1);
    setStyle("-fx-background-color: #145830;");
    adapterLesLignesEtColonnes();
    remplirOthellier();
    nouvellePartie();
  }

  // -----------------------------------------------------------------
  // Propriétés observables exposées au contrôleur (étape 4)
  // -----------------------------------------------------------------

  /** Propriété observable du joueur dont c'est le tour. */
  public ObjectProperty<Joueur> joueurCourantProperty() {
    return joueurCourant;
  }

  /** Valeur courante du joueur dont c'est le tour. */
  public Joueur getJoueurCourant() {
    return joueurCourant.get();
  }

  /** Propriété observable du drapeau de fin de partie. */
  public BooleanProperty partieTermineeProperty() {
    return partieTerminee;
  }

  /** Accès à la case située à la position {@code (ligne, colonne)}. */
  public Case getCase(int ligne, int colonne) {
    return cases[ligne][colonne];
  }

  // -----------------------------------------------------------------
  // Construction du plateau
  // -----------------------------------------------------------------

  /**
   * Fixe les contraintes des lignes et colonnes pour que la grille soit régulière et extensible.
   */
  private void adapterLesLignesEtColonnes() {
    for (int i = 0; i < TAILLE; i++) {
      ColumnConstraints column = new ColumnConstraints();
      column.setHgrow(Priority.ALWAYS);
      column.setPercentWidth(100.0 / TAILLE);
      getColumnConstraints().add(column);

      RowConstraints row = new RowConstraints();
      row.setVgrow(Priority.ALWAYS);
      row.setPercentHeight(100.0 / TAILLE);
      getRowConstraints().add(row);
    }
  }

  /** Instancie les 64 cases, leur branche l'écouteur partagé et les ajoute à la grille. */
  private void remplirOthellier() {
    for (int ligne = 0; ligne < TAILLE; ligne++) {
      for (int colonne = 0; colonne < TAILLE; colonne++) {
        Case c = new Case(ligne, colonne);
        c.setOnAction(caseListener);
        cases[ligne][colonne] = c;
        add(c, colonne, ligne);
      }
    }
  }

  /**
   * Configuration de départ classique : deux pions noirs en (m-1, m) et (m, m-1) et deux pions
   * blancs en (m-1, m-1) et (m, m) où m = TAILLE / 2.
   */
  private void positionnerPionsDebutPartie() {
    int m = TAILLE / 2;
    placer(cases[m - 1][m - 1], Joueur.BLANC);
    placer(cases[m - 1][m], Joueur.NOIR);
    placer(cases[m][m - 1], Joueur.NOIR);
    placer(cases[m][m], Joueur.BLANC);
  }

  /**
   * Démarre une nouvelle partie : vide le plateau, remet les scores à zéro, place les quatre pions
   * du début et redonne la main au joueur NOIR.
   */
  public void nouvellePartie() {
    vider();
    Joueur.initialiserScores();
    positionnerPionsDebutPartie();
    joueurCourant.set(Joueur.NOIR);
    partieTerminee.set(false);
  }

  /** Vide toutes les cases du plateau (chaque case repasse au joueur {@link Joueur#PERSONNE}). */
  private void vider() {
    for (int ligne = 0; ligne < TAILLE; ligne++) {
      for (int colonne = 0; colonne < TAILLE; colonne++) {
        cases[ligne][colonne].setPossesseur(Joueur.PERSONNE);
      }
    }
  }

  // -----------------------------------------------------------------
  // Logique d'un coup (orchestration)
  // -----------------------------------------------------------------

  /** Joue le coup demandé : pose le pion, capture les pions adverses, passe la main. */
  private void jouer(Case caseSelectionnee) {
    placer(caseSelectionnee, joueurCourant.get());
    for (Case caseCapturee : casesCapturable(caseSelectionnee)) {
      capturer(caseCapturee);
    }
    tourSuivant();
  }

  /**
   * Pose un pion du joueur indiqué sur la case (et incrémente son score).
   *
   * <p>Méthode utilitaire utilisée à la fois par {@link #positionnerPionsDebutPartie()} et par
   * {@link #jouer(Case)} pour ne pas dupliquer la logique « pose + score ».
   */
  private void placer(Case c, Joueur joueur) {
    c.setPossesseur(joueur);
    joueur.incrementerScore();
  }

  /**
   * Retourne un pion : il change de couleur et les scores s'ajustent en miroir (l'ancien
   * propriétaire perd un point, le nouveau en gagne un).
   */
  private void capturer(Case caseCapturee) {
    Joueur ancien = caseCapturee.getPossesseur();
    ancien.decrementerScore();
    Joueur nouveau = ancien.suivant();
    caseCapturee.setPossesseur(nouveau);
    nouveau.incrementerScore();
  }

  /**
   * Donne la main au joueur suivant.
   *
   * <p>Cas particulier : si le joueur suivant ne peut pas jouer, on redonne la main au précédent ;
   * si aucun des deux ne peut jouer, la partie est terminée.
   */
  private void tourSuivant() {
    Joueur prochain = joueurCourant.get().suivant();
    joueurCourant.set(prochain);
    if (!peutJouer()) {
      joueurCourant.set(prochain.suivant());
      if (!peutJouer()) {
        partieTerminee.set(true);
      }
    }
  }

  /** Une position est jouable si elle est vide et si elle capture au moins un pion adverse. */
  public boolean estPositionJouable(Case caseSelectionnee) {
    return caseSelectionnee.getPossesseur() == Joueur.PERSONNE
        && !casesCapturable(caseSelectionnee).isEmpty();
  }

  /**
   * Liste des cases sur lesquelles le joueur courant peut jouer (utilisée par {@code peutJouer}).
   */
  public List<Case> casesJouables() {
    List<Case> jouables = new ArrayList<>();
    for (int ligne = 0; ligne < TAILLE; ligne++) {
      for (int colonne = 0; colonne < TAILLE; colonne++) {
        Case c = cases[ligne][colonne];
        if (estPositionJouable(c)) {
          jouables.add(c);
        }
      }
    }
    return jouables;
  }

  /** Le joueur courant peut-il jouer au moins un coup ? */
  public boolean peutJouer() {
    return !casesJouables().isEmpty();
  }

  // -----------------------------------------------------------------
  // Moteur de capture (FOURNI - aucune ligne à écrire ici)
  //
  // Ces méthodes constituent le coeur algorithmique du jeu : dans une
  // direction donnée, on collecte les pions adverses alignés à partir
  // de la case sélectionnée jusqu'à rencontrer un pion de notre couleur
  // (qui ferme la capture). Si on tombe sur une case vide ou si on sort
  // du plateau avant de fermer, rien n'est capturable dans cette
  // direction.
  // -----------------------------------------------------------------

  /** Cases adverses capturables depuis {@code caseSelectionnee}, agrégées sur les 8 directions. */
  public List<Case> casesCapturable(Case caseSelectionnee) {
    List<Case> resultat = new ArrayList<>();
    for (Point2D direction : DIRECTIONS) {
      resultat.addAll(casesCapturable(caseSelectionnee, direction));
    }
    return resultat;
  }

  /** Cases capturables dans une direction donnée à partir de {@code caseSelectionnee}. */
  private List<Case> casesCapturable(Case caseSelectionnee, Point2D direction) {
    List<Case> casesCapturable = new ArrayList<>();

    int indiceLigne = caseSelectionnee.getLigne() + (int) direction.getY();
    int indiceColonne = caseSelectionnee.getColonne() + (int) direction.getX();

    while (estIndicesValides(indiceLigne, indiceColonne)) {
      Joueur possesseur = cases[indiceLigne][indiceColonne].getPossesseur();
      if (possesseur != joueurCourant.get().suivant()) {
        break;
      }
      casesCapturable.add(cases[indiceLigne][indiceColonne]);
      indiceLigne += direction.getY();
      indiceColonne += direction.getX();
    }

    if (estIndicesValides(indiceLigne, indiceColonne)
        && cases[indiceLigne][indiceColonne].getPossesseur() == joueurCourant.get()) {
      return casesCapturable;
    }
    return new ArrayList<>();
  }

  private boolean estIndicesValides(int indiceLigne, int indiceColonne) {
    return estIndiceValide(indiceLigne) && estIndiceValide(indiceColonne);
  }

  private boolean estIndiceValide(int indice) {
    return indice >= 0 && indice < TAILLE;
  }
}
