package fr.univ_amu.iut.bonus10;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

/**
 * Bonus 10 - étape 4 : contrôleur de la vue {@code OthelloView.fxml}.
 *
 * <p>L'{@link Othellier} se construit tout seul (constructeur sans argument), donc on peut le
 * laisser instancier directement par le FXML via {@code <Othellier fx:id="othellier"/>}. Le
 * contrôleur n'a plus qu'à brancher l'entête (joueur courant + scores + message de fin) sur les
 * propriétés observables du modèle. La capture est entièrement gérée à l'intérieur de l'othellier.
 *
 * <p>Le fichier {@code OthelloView.fxml} est <b>fourni</b> : ouvrez-le pour repérer les
 * identifiants {@code fx:id} des composants à injecter (labelJoueurCourant, labelScoreNoir,
 * labelScoreBlanc, labelFinDePartie, othellier) et l'attribut {@code onAction="#onNouvellePartie"}
 * du bouton « Nouvelle partie ».
 */
public class OthelloController {

  // TODO bonus 10 étape 4.1 : déclarer les six données membres @FXML privées,
  // dont les noms
  // doivent correspondre aux fx:id du fichier OthelloView.fxml :
  // - racine : BorderPane - la racine de la scène (utile pour les tests)
  // - othellier : Othellier - le plateau de jeu (instancié par le FXML)
  // - labelJoueurCourant : Label - affiche « Au tour de : ● Noir » ou « ○ Blanc »
  // - labelScoreNoir : Label - affiche « ● Noir : N »
  // - labelScoreBlanc : Label - affiche « ○ Blanc : N »
  // - labelFinDePartie : Label - affiche le message de fin de partie quand
  // applicable
  @FXML private BorderPane racine;
  @FXML private Othellier othellier;
  @FXML private Label labelJoueurCourant;
  @FXML private Label labelScoreNoir;
  @FXML private Label labelScoreBlanc;
  @FXML private Label labelFinDePartie;

  /**
   * Méthode invoquée automatiquement par {@link javafx.fxml.FXMLLoader} après l'injection des
   * composants. C'est ici que l'on branche les labels de l'entête sur les propriétés observables du
   * modèle (l'othellier).
   */
  @FXML
  private void initialize() {
    // 1. Label joueur courant
    labelJoueurCourant
        .textProperty()
        .bind(
            Bindings.createStringBinding(
                () -> "Au tour de : " + libelle(othellier.getJoueurCourant()),
                othellier.joueurCourantProperty()));

    // 2. Label score Noir
    labelScoreNoir
        .textProperty()
        .bind(Bindings.concat("● Noir : ").concat(Joueur.NOIR.scoreProperty().asString()));

    // 3. Label score Blanc
    labelScoreBlanc
        .textProperty()
        .bind(Bindings.concat("○ Blanc : ").concat(Joueur.BLANC.scoreProperty().asString()));

    // 4. Label fin de partie
    labelFinDePartie
        .textProperty()
        .bind(
            Bindings.when(othellier.partieTermineeProperty())
                .then(
                    Bindings.createStringBinding(
                        this::messageFinDePartie,
                        Joueur.NOIR.scoreProperty(),
                        Joueur.BLANC.scoreProperty()))
                .otherwise(""));
  }

  /**
   * Construit le message de fin de partie en fonction des scores : victoire du noir, victoire du
   * blanc, ou égalité. Méthode utilitaire appelée par le binding posé dans {@link #initialize()}.
   */
  private String messageFinDePartie() {
    int noir = Joueur.NOIR.getScore();
    int blanc = Joueur.BLANC.getScore();
    if (noir > blanc) {
      return "🏁 Partie terminée. Victoire de ● Noir (" + noir + " contre " + blanc + ").";
    }
    if (blanc > noir) {
      return "🏁 Partie terminée. Victoire de ○ Blanc (" + blanc + " contre " + noir + ").";
    }
    return "🏁 Partie terminée. Égalité parfaite (" + noir + " - " + blanc + ").";
  }

  /** Libellé textuel d'un joueur (utilisé par le binding du label « Au tour de... »). */
  private String libelle(Joueur j) {
    if (j == Joueur.NOIR) {
      return "● Noir";
    }
    if (j == Joueur.BLANC) {
      return "○ Blanc";
    }
    return "—";
  }

  /**
   * Handler du bouton « Nouvelle partie » (cf. {@code onAction="#onNouvellePartie"} dans le FXML).
   */
  @FXML
  private void onNouvellePartie() {
    othellier.nouvellePartie();
  }

  /** Exposé pour les tests d'intégration : permet à TestFX d'accéder à l'othellier. */
  public Othellier getOthellier() {
    return othellier;
  }
}
