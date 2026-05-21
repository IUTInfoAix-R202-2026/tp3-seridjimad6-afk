package fr.univ_amu.iut.bonus10;

import javafx.scene.control.Button;
import javafx.scene.image.ImageView;

/**
 * Bonus 10 - étape 2 : une case du plateau de jeu.
 *
 * <p>Pour réaliser le plateau de jeu, il nous faut des boutons qui se souviennent de leur position
 * dans l'othellier. Au moment de leur construction, de tels boutons reçoivent les valeurs des
 * indices ligne et colonne qui définissent leur placement dans la matrice. En plus de ces
 * coordonnées, il faut connaître le joueur qui possède la case pour y dessiner l'image de son
 * jeton.
 *
 * <p>Cette classe étend {@link Button} : chaque case est ainsi cliquable et peut recevoir un
 * gestionnaire d'événement (le contrôleur d'othellier en branchera un seul, partagé par les 64
 * cases).
 */
class Case extends Button {

  // TODO bonus 10 étape 2.1 : déclarer les données membres privées suivantes :
  // - ligne : int - indice de ligne dans la matrice
  // - colonne : int - indice de colonne dans la matrice
  // - imageView : ImageView - composant graphique qui affiche le pion (image du
  // Joueur)
  // - possesseur : Joueur - joueur à qui appartient la case (initialisée à
  // Joueur.PERSONNE)
  private int ligne;
  private int colonne;
  private ImageView imageView;
  private Joueur possesseur = Joueur.PERSONNE;

  /**
   * Construit une case à la position {@code (ligne, colonne)}. Par défaut, la case n'appartient à
   * personne et son pion affiché est celui du joueur {@link Joueur#PERSONNE} (image transparente).
   */
  Case(int ligne, int colonne) {
    this.ligne = ligne;
    this.colonne = colonne;
    this.imageView = new ImageView(Joueur.PERSONNE.getImage());
    this.imageView.setFitWidth(56);
    this.imageView.setFitHeight(56);
    this.imageView.setPreserveRatio(true);
    setGraphic(imageView);
    setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    setStyle("-fx-background-color: #1e6f3f; -fx-border-color: #103f26; -fx-border-width: 1;");
  }

  /** Renvoie le joueur qui possède actuellement la case (NOIR, BLANC ou PERSONNE). */
  Joueur getPossesseur() {
    return possesseur;
  }

  /**
   * Modifie le joueur qui possède la case et met à jour le pion affiché par {@code imageView}.
   *
   * <p>C'est cette méthode qu'utiliseront {@link Othellier#placer(Case, Joueur)} et {@link
   * Othellier#capturer(Case)} pour changer la couleur d'un pion sur le plateau.
   */
  void setPossesseur(Joueur possesseur) {
    this.possesseur = possesseur;
    imageView.setImage(possesseur.getImage());
  }

  /** Indice de ligne de la case dans l'othellier (entre 0 et TAILLE - 1). */
  int getLigne() {
    return ligne;
  }

  /** Indice de colonne de la case dans l'othellier (entre 0 et TAILLE - 1). */
  int getColonne() {
    return colonne;
  }
}
