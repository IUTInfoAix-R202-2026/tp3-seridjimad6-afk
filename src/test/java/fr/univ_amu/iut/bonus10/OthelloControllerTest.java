package fr.univ_amu.iut.bonus10;

import static org.assertj.core.api.Assertions.assertThat;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

/**
 * Tests d'intégration du contrôleur Othello complet (bonus 10).
 *
 * <p>Chaque scénario installe au besoin une configuration de plateau via {@link #installer}, joue
 * un coup, puis vérifie à la fois :
 *
 * <ul>
 *   <li>l'état du modèle ({@link Othellier} : pions posés et retournés)
 *   <li>la mise à jour des labels de la barre de statut : joueur courant, scores noir / blanc et
 *       message éventuel de fin de partie
 * </ul>
 */
@ExtendWith(ApplicationExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OthelloControllerTest {

  private OthelloController controller;

  @Start
  void start(Stage stage) throws Exception {
    stage.setScene(null);
    FXMLLoader loader = new FXMLLoader(OthelloController.class.getResource("OthelloView.fxml"));
    Parent racine = loader.load();
    controller = loader.getController();
    stage.setScene(new Scene(racine, 560, 680));
    stage.show();
  }

  /**
   * Remplace l'état complet du plateau et fixe le joueur courant. {@code positions[ligne][colonne]
   * == null} signifie case vide. Les scores sont recalculés à partir du contenu posé.
   */
  private void installer(Othellier o, Joueur[][] positions, Joueur joueurCourant) {
    Joueur.NOIR.scoreProperty().set(0);
    Joueur.BLANC.scoreProperty().set(0);
    for (int l = 0; l < Othellier.TAILLE; l++) {
      for (int c = 0; c < Othellier.TAILLE; c++) {
        Joueur j = positions[l][c] == null ? Joueur.PERSONNE : positions[l][c];
        o.getCase(l, c).setPossesseur(j);
        if (j == Joueur.NOIR) {
          Joueur.NOIR.scoreProperty().set(Joueur.NOIR.getScore() + 1);
        } else if (j == Joueur.BLANC) {
          Joueur.BLANC.scoreProperty().set(Joueur.BLANC.getScore() + 1);
        }
      }
    }
    o.partieTermineeProperty().set(false);
    o.joueurCourantProperty().set(joueurCourant);
  }

  private Label labelTour(FxRobot r) {
    return r.lookup("#labelJoueurCourant").queryAs(Label.class);
  }

  private Label labelScoreNoir(FxRobot r) {
    return r.lookup("#labelScoreNoir").queryAs(Label.class);
  }

  private Label labelScoreBlanc(FxRobot r) {
    return r.lookup("#labelScoreBlanc").queryAs(Label.class);
  }

  private Label labelFin(FxRobot r) {
    return r.lookup("#labelFinDePartie").queryAs(Label.class);
  }

  // ===================================================================
  // Cas nominaux : pose légale + retournement (1 pion)
  // ===================================================================

  @Test
  @Order(1)
  void poser_un_pion_noir_legal_au_demarrage_met_a_jour_le_modele_et_la_barre_de_statut(
      FxRobot robot) {
    // Position de départ classique : NOIR commence et joue en (2,3) pour retourner
    // le pion
    // BLANC de la case (3,3) (encadré par (2,3) et (4,3)).
    Othellier o = controller.getOthellier();
    robot.interact(() -> o.getCase(2, 3).fire());

    assertThat(o.getCase(2, 3).getPossesseur())
        .as("pion noir posé en (2,3)")
        .isEqualTo(Joueur.NOIR);
    assertThat(o.getCase(3, 3).getPossesseur())
        .as("pion blanc retourné en NOIR en (3,3)")
        .isEqualTo(Joueur.NOIR);
    assertThat(labelScoreNoir(robot).getText()).as("score noir = 4").contains("4");
    assertThat(labelScoreBlanc(robot).getText()).as("score blanc = 1").contains("1");
    assertThat(labelTour(robot).getText()).as("au tour du blanc").contains("Blanc");
    assertThat(labelFin(robot).getText()).as("partie en cours").isEmpty();
  }

  @Test
  @Order(2)
  void poser_un_pion_blanc_legal_apres_un_coup_noir_met_a_jour_le_modele_et_la_barre_de_statut(
      FxRobot robot) {
    // On enchaîne deux coups depuis la position de départ : NOIR(2,3) puis
    // BLANC(2,4).
    // BLANC(2,4) capture le pion noir (3,4) (encadré par (2,4) et (4,4) qui est
    // blanc).
    Othellier o = controller.getOthellier();
    robot.interact(() -> o.getCase(2, 3).fire());
    robot.interact(() -> o.getCase(2, 4).fire());

    assertThat(o.getCase(2, 4).getPossesseur())
        .as("pion blanc posé en (2,4)")
        .isEqualTo(Joueur.BLANC);
    assertThat(o.getCase(3, 4).getPossesseur())
        .as("pion noir retourné en BLANC en (3,4)")
        .isEqualTo(Joueur.BLANC);
    assertThat(labelScoreNoir(robot).getText()).as("score noir = 3").contains("3");
    assertThat(labelScoreBlanc(robot).getText()).as("score blanc = 3").contains("3");
    assertThat(labelTour(robot).getText()).as("au tour du noir").contains("Noir");
    assertThat(labelFin(robot).getText()).as("partie en cours").isEmpty();
  }

  @Test
  @Order(3)
  void un_coup_qui_capture_un_seul_pion_met_a_jour_le_modele_et_les_scores(FxRobot robot) {
    // Configuration minimale : un seul pion à retourner, pour isoler le cas "un
    // retournement".
    Othellier o = controller.getOthellier();
    Joueur[][] config = new Joueur[Othellier.TAILLE][Othellier.TAILLE];
    config[4][0] = Joueur.NOIR;
    config[4][1] = Joueur.BLANC;
    // (4,2) sera posée par le clic et fermera la capture
    robot.interact(() -> installer(o, config, Joueur.NOIR));
    robot.interact(() -> o.getCase(4, 2).fire());

    assertThat(o.getCase(4, 1).getPossesseur())
        .as("le seul pion à retourner doit avoir basculé en NOIR")
        .isEqualTo(Joueur.NOIR);
    assertThat(labelScoreNoir(robot).getText())
        .as("score noir = 3 (1 + 1 posé + 1 capturé)")
        .contains("3");
    assertThat(labelScoreBlanc(robot).getText()).as("score blanc = 0").contains("0");
  }

  // ===================================================================
  // Retournements par direction
  // ===================================================================

  @Test
  @Order(4)
  void un_coup_qui_capture_verticalement_retourne_le_pion_et_met_a_jour_les_scores(FxRobot robot) {
    Othellier o = controller.getOthellier();
    Joueur[][] config = new Joueur[Othellier.TAILLE][Othellier.TAILLE];
    config[0][3] = Joueur.NOIR;
    config[1][3] = Joueur.BLANC;
    // (2,3) sera jouée par NOIR : ferme la capture verticale descendante.
    robot.interact(() -> installer(o, config, Joueur.NOIR));
    robot.interact(() -> o.getCase(2, 3).fire());

    assertThat(o.getCase(1, 3).getPossesseur())
        .as("retournement vertical : (1,3) doit avoir basculé en NOIR")
        .isEqualTo(Joueur.NOIR);
    assertThat(o.getCase(2, 3).getPossesseur())
        .as("pion fermant noir en (2,3)")
        .isEqualTo(Joueur.NOIR);
    assertThat(labelScoreNoir(robot).getText()).as("score noir = 3").contains("3");
    assertThat(labelScoreBlanc(robot).getText()).as("score blanc = 0").contains("0");
  }

  @Test
  @Order(5)
  void un_coup_qui_capture_horizontalement_retourne_le_pion_et_met_a_jour_les_scores(
      FxRobot robot) {
    Othellier o = controller.getOthellier();
    Joueur[][] config = new Joueur[Othellier.TAILLE][Othellier.TAILLE];
    config[3][0] = Joueur.NOIR;
    config[3][1] = Joueur.BLANC;
    // (3,2) sera jouée par NOIR : ferme la capture horizontale vers la droite.
    robot.interact(() -> installer(o, config, Joueur.NOIR));
    robot.interact(() -> o.getCase(3, 2).fire());

    assertThat(o.getCase(3, 1).getPossesseur())
        .as("retournement horizontal : (3,1) doit avoir basculé en NOIR")
        .isEqualTo(Joueur.NOIR);
    assertThat(o.getCase(3, 2).getPossesseur())
        .as("pion fermant noir en (3,2)")
        .isEqualTo(Joueur.NOIR);
    assertThat(labelScoreNoir(robot).getText()).as("score noir = 3").contains("3");
    assertThat(labelScoreBlanc(robot).getText()).as("score blanc = 0").contains("0");
  }

  @Test
  @Order(6)
  void un_coup_qui_capture_en_diagonale_retourne_les_pions_et_met_a_jour_les_scores(FxRobot robot) {
    Othellier o = controller.getOthellier();
    Joueur[][] config = new Joueur[Othellier.TAILLE][Othellier.TAILLE];
    config[0][0] = Joueur.NOIR;
    config[1][1] = Joueur.BLANC;
    config[2][2] = Joueur.BLANC;
    // (3,3) sera jouée par NOIR : ferme la diagonale descendante.
    robot.interact(() -> installer(o, config, Joueur.NOIR));
    robot.interact(() -> o.getCase(3, 3).fire());

    assertThat(o.getCase(1, 1).getPossesseur())
        .as("retournement diagonal : (1,1) doit avoir basculé en NOIR")
        .isEqualTo(Joueur.NOIR);
    assertThat(o.getCase(2, 2).getPossesseur())
        .as("retournement diagonal : (2,2) doit avoir basculé en NOIR")
        .isEqualTo(Joueur.NOIR);
    assertThat(labelScoreNoir(robot).getText()).as("score noir = 4").contains("4");
    assertThat(labelScoreBlanc(robot).getText()).as("score blanc = 0").contains("0");
  }

  @Test
  @Order(7)
  void un_coup_qui_capture_dans_deux_directions_met_a_jour_le_modele_et_les_scores(FxRobot robot) {
    // Capture sur deux axes différents en un seul coup : (2,2) ferme à la fois la
    // direction
    // horizontale droite (capture (2,3)) et la direction verticale bas (capture
    // (3,2)).
    Othellier o = controller.getOthellier();
    Joueur[][] config = new Joueur[Othellier.TAILLE][Othellier.TAILLE];
    config[2][3] = Joueur.BLANC;
    config[2][4] = Joueur.NOIR;
    config[3][2] = Joueur.BLANC;
    config[4][2] = Joueur.NOIR;
    robot.interact(() -> installer(o, config, Joueur.NOIR));
    robot.interact(() -> o.getCase(2, 2).fire());

    assertThat(o.getCase(2, 3).getPossesseur())
        .as("retournement multiple : (2,3) (axe horizontal) doit être NOIR")
        .isEqualTo(Joueur.NOIR);
    assertThat(o.getCase(3, 2).getPossesseur())
        .as("retournement multiple : (3,2) (axe vertical) doit être NOIR")
        .isEqualTo(Joueur.NOIR);
    assertThat(labelScoreNoir(robot).getText())
        .as("score noir = 5 (2 fermants + 1 posé + 2 capturés)")
        .contains("5");
    assertThat(labelScoreBlanc(robot).getText()).as("score blanc = 0 (2 retournés)").contains("0");
  }

  // ===================================================================
  // Tours sautés (un joueur ne peut pas jouer)
  // ===================================================================

  @Test
  @Order(8)
  void quand_le_joueur_noir_ne_peut_pas_jouer_la_main_revient_au_joueur_blanc(FxRobot robot) {
    // BLANC joue (0,2) : capture (0,1). Après ce coup, NOIR n'a aucun coup légal
    // (aucun BLANC
    // restant capturable) MAIS BLANC peut encore jouer (capture diagonale via
    // (2,1)).
    // Conséquence attendue : tourSuivant() saute le tour du NOIR et redonne la main
    // au BLANC.
    Othellier o = controller.getOthellier();
    Joueur[][] config = new Joueur[Othellier.TAILLE][Othellier.TAILLE];
    config[0][0] = Joueur.BLANC;
    config[0][1] = Joueur.NOIR;
    config[1][0] = Joueur.NOIR;
    config[1][1] = Joueur.NOIR;
    config[2][0] = Joueur.NOIR;
    robot.interact(() -> installer(o, config, Joueur.BLANC));
    robot.interact(() -> o.getCase(0, 2).fire());

    assertThat(o.getCase(0, 1).getPossesseur())
        .as("(0,1) doit avoir été retourné en BLANC")
        .isEqualTo(Joueur.BLANC);
    assertThat(o.peutJouer())
        .as("BLANC doit encore avoir au moins un coup (capture diagonale via (2,1))")
        .isTrue();
    assertThat(labelTour(robot).getText())
        .as("NOIR n'ayant aucun coup légal, la main est revenue au BLANC")
        .contains("Blanc");
    assertThat(labelFin(robot).getText())
        .as("la partie n'est pas terminée : BLANC peut encore jouer")
        .isEmpty();
    assertThat(labelScoreNoir(robot).getText()).contains("3");
    assertThat(labelScoreBlanc(robot).getText()).contains("3");
  }

  @Test
  @Order(9)
  void quand_le_joueur_blanc_ne_peut_pas_jouer_la_main_revient_au_joueur_noir(FxRobot robot) {
    // Scénario symétrique du précédent : NOIR joue, BLANC ne peut pas jouer, NOIR
    // rejoue.
    Othellier o = controller.getOthellier();
    Joueur[][] config = new Joueur[Othellier.TAILLE][Othellier.TAILLE];
    config[0][0] = Joueur.NOIR;
    config[0][1] = Joueur.BLANC;
    config[1][0] = Joueur.BLANC;
    config[1][1] = Joueur.BLANC;
    config[2][0] = Joueur.BLANC;
    robot.interact(() -> installer(o, config, Joueur.NOIR));
    robot.interact(() -> o.getCase(0, 2).fire());

    assertThat(o.getCase(0, 1).getPossesseur())
        .as("(0,1) doit avoir été retourné en NOIR")
        .isEqualTo(Joueur.NOIR);
    assertThat(labelTour(robot).getText())
        .as("BLANC n'ayant aucun coup légal, la main est revenue au NOIR")
        .contains("Noir");
    assertThat(labelFin(robot).getText())
        .as("la partie n'est pas terminée : NOIR peut encore jouer")
        .isEmpty();
    assertThat(labelScoreNoir(robot).getText()).contains("3");
    assertThat(labelScoreBlanc(robot).getText()).contains("3");
  }

  // ===================================================================
  // Fin de partie
  // ===================================================================

  @Test
  @Order(10)
  void quand_le_joueur_blanc_gagne_la_partie_le_message_de_fin_affiche_la_victoire_du_blanc(
      FxRobot robot) {
    // Configuration où BLANC va capturer en un coup les deux derniers pions noirs
    // encore en
    // jeu. Après ce coup, il n'y a plus aucun NOIR sur le plateau ; ni l'un ni
    // l'autre joueur
    // ne peut donc plus capturer quoi que ce soit, et la partie se termine en
    // faveur du blanc.
    Othellier o = controller.getOthellier();
    Joueur[][] config = new Joueur[Othellier.TAILLE][Othellier.TAILLE];
    config[0][0] = Joueur.BLANC;
    config[0][1] = Joueur.NOIR;
    config[0][2] = Joueur.NOIR;
    // (0,3) sera jouée par BLANC : capture (0,1) et (0,2).
    robot.interact(() -> installer(o, config, Joueur.BLANC));
    robot.interact(() -> o.getCase(0, 3).fire());

    assertThat(o.getCase(0, 1).getPossesseur()).isEqualTo(Joueur.BLANC);
    assertThat(o.getCase(0, 2).getPossesseur()).isEqualTo(Joueur.BLANC);
    assertThat(o.partieTermineeProperty().get())
        .as("la partie doit être déclarée terminée")
        .isTrue();
    assertThat(labelScoreBlanc(robot).getText())
        .as("score BLANC = 4 (1 + 1 posé + 2 capturés)")
        .contains("4");
    assertThat(labelScoreNoir(robot).getText())
        .as("score NOIR = 0 (les 2 NOIR ont été capturés)")
        .contains("0");
    assertThat(labelFin(robot).getText())
        .as("le message de fin doit annoncer la victoire du BLANC")
        .contains("Victoire")
        .contains("Blanc");
  }

  @Test
  @Order(11)
  void quand_le_joueur_noir_perd_la_partie_le_message_de_fin_n_annonce_pas_sa_victoire(
      FxRobot robot) {
    // Même scénario que ci-dessus, examiné côté NOIR : score à 0, label de fin
    // n'annonce pas
    // la victoire du NOIR mais celle du BLANC.
    Othellier o = controller.getOthellier();
    Joueur[][] config = new Joueur[Othellier.TAILLE][Othellier.TAILLE];
    config[0][0] = Joueur.BLANC;
    config[0][1] = Joueur.NOIR;
    config[0][2] = Joueur.NOIR;
    robot.interact(() -> installer(o, config, Joueur.BLANC));
    robot.interact(() -> o.getCase(0, 3).fire());

    assertThat(Joueur.NOIR.getScore())
        .as("le score noir doit être strictement inférieur au score blanc")
        .isLessThan(Joueur.BLANC.getScore());
    assertThat(o.partieTermineeProperty().get()).as("la partie est terminée").isTrue();
    assertThat(labelFin(robot).getText())
        .as("le message de fin ne doit pas annoncer une victoire du NOIR")
        .doesNotContain("Victoire de ● Noir");
    assertThat(labelFin(robot).getText())
        .as("la défaite du NOIR se traduit par la victoire annoncée du BLANC")
        .contains("Victoire")
        .contains("Blanc");
  }

  // ===================================================================
  // Réinitialisation
  // ===================================================================

  @Test
  @Order(12)
  void cliquer_sur_nouvelle_partie_reinitialise_le_plateau_les_scores_et_la_barre_de_statut(
      FxRobot robot) {
    Othellier o = controller.getOthellier();
    robot.interact(() -> o.getCase(2, 3).fire());
    Button nouvelle =
        robot
            .lookup(b -> b instanceof Button btn && "Nouvelle partie".equals(btn.getText()))
            .queryAs(Button.class);
    robot.interact(nouvelle::fire);

    assertThat(labelScoreNoir(robot).getText())
        .as("après nouvelle partie : score noir = 2")
        .contains("2");
    assertThat(labelScoreBlanc(robot).getText())
        .as("après nouvelle partie : score blanc = 2")
        .contains("2");
    assertThat(labelTour(robot).getText())
        .as("après nouvelle partie : au tour du noir")
        .contains("Noir");
    assertThat(labelFin(robot).getText())
        .as("après nouvelle partie : pas de message de fin")
        .isEmpty();
    assertThat(o.getCase(2, 3).getPossesseur())
        .as("après nouvelle partie : la case (2,3) est vide")
        .isEqualTo(Joueur.PERSONNE);
  }
}
