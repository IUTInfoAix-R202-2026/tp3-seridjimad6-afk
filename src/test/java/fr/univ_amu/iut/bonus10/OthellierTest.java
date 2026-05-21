package fr.univ_amu.iut.bonus10;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests du modèle / composant {@link Othellier} (bonus 10).
 *
 * <p>L'Othellier étend {@link javafx.scene.layout.GridPane} : son instanciation requiert que la
 * plateforme JavaFX soit démarrée et qu'on travaille sur le FX Application Thread. On démarre la
 * plateforme une seule fois via {@link Platform#startup(Runnable)} puis on exécute chaque scénario
 * de test à l'intérieur d'un {@link Platform#runLater(Runnable)} dont on attend la complétion.
 */
class OthellierTest {

  @BeforeAll
  static void demarrerJavaFx() {
    try {
      Platform.startup(() -> {});
    } catch (IllegalStateException dejaDemarree) {
      // OK : un autre test a déjà démarré la plateforme dans le même process.
    }
  }

  private <T> T surFxThread(java.util.function.Supplier<T> action) throws Exception {
    AtomicReference<T> resultat = new AtomicReference<>();
    AtomicReference<Throwable> erreur = new AtomicReference<>();
    java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
    Platform.runLater(
        () -> {
          try {
            resultat.set(action.get());
          } catch (Throwable t) {
            erreur.set(t);
          } finally {
            latch.countDown();
          }
        });
    latch.await();
    if (erreur.get() != null) {
      throw new RuntimeException(erreur.get());
    }
    return resultat.get();
  }

  @Test
  void un_othellier_neuf_positionne_les_quatre_pions_au_centre() throws Exception {
    Othellier o = surFxThread(Othellier::new);
    int m = Othellier.TAILLE / 2;
    assertThat(o.getCase(m - 1, m - 1).getPossesseur()).isEqualTo(Joueur.BLANC);
    assertThat(o.getCase(m - 1, m).getPossesseur()).isEqualTo(Joueur.NOIR);
    assertThat(o.getCase(m, m - 1).getPossesseur()).isEqualTo(Joueur.NOIR);
    assertThat(o.getCase(m, m).getPossesseur()).isEqualTo(Joueur.BLANC);
    assertThat(o.getJoueurCourant()).isEqualTo(Joueur.NOIR);
    assertThat(Joueur.NOIR.getScore()).isEqualTo(2);
    assertThat(Joueur.BLANC.getScore()).isEqualTo(2);
  }

  @Test
  void le_joueur_noir_dispose_de_quatre_coups_legaux_au_demarrage() throws Exception {
    Othellier o = surFxThread(Othellier::new);
    var jouables = surFxThread(o::casesJouables);
    assertThat(jouables)
        .as("au démarrage, le joueur noir doit avoir exactement 4 coups légaux")
        .hasSize(4);
    var coordonnees = jouables.stream().map(c -> c.getLigne() + "," + c.getColonne()).toList();
    assertThat(coordonnees).contains("2,3", "3,2", "4,5", "5,4");
  }

  @Test
  void jouer_en_2_3_au_demarrage_retourne_le_pion_blanc_de_3_3() throws Exception {
    Othellier o = surFxThread(Othellier::new);
    surFxThread(
        () -> {
          o.getCase(2, 3).fire();
          return null;
        });
    assertThat(o.getCase(2, 3).getPossesseur())
        .as("le pion noir est posé sur la case cliquée")
        .isEqualTo(Joueur.NOIR);
    assertThat(o.getCase(3, 3).getPossesseur())
        .as("le pion blanc encadré en (3,3) doit être retourné")
        .isEqualTo(Joueur.NOIR);
    assertThat(Joueur.NOIR.getScore()).isEqualTo(4); // 2 + 1 posé + 1 capturé
    assertThat(Joueur.BLANC.getScore()).isEqualTo(1); // 2 - 1 capturé
    assertThat(o.getJoueurCourant())
        .as("après un coup valide, la main passe au blanc")
        .isEqualTo(Joueur.BLANC);
  }

  @Test
  void cliquer_sur_une_case_non_jouable_est_ignore_et_la_main_reste_au_meme_joueur()
      throws Exception {
    Othellier o = surFxThread(Othellier::new);
    surFxThread(
        () -> {
          o.getCase(0, 0).fire();
          return null;
        });
    assertThat(o.getCase(0, 0).getPossesseur())
        .as("la case (0,0) ne capture rien : le clic doit être ignoré")
        .isEqualTo(Joueur.PERSONNE);
    assertThat(o.getJoueurCourant())
        .as("la main reste au joueur noir si son coup était illégal")
        .isEqualTo(Joueur.NOIR);
  }

  @Test
  void nouvelle_partie_reinitialise_le_plateau_les_scores_et_le_joueur_courant() throws Exception {
    Othellier o = surFxThread(Othellier::new);
    surFxThread(
        () -> {
          o.getCase(2, 3).fire();
          return null;
        });
    surFxThread(
        () -> {
          o.nouvellePartie();
          return null;
        });
    int m = Othellier.TAILLE / 2;
    assertThat(o.getCase(2, 3).getPossesseur()).isEqualTo(Joueur.PERSONNE);
    assertThat(o.getCase(m - 1, m - 1).getPossesseur()).isEqualTo(Joueur.BLANC);
    assertThat(o.getJoueurCourant()).isEqualTo(Joueur.NOIR);
    assertThat(Joueur.NOIR.getScore()).isEqualTo(2);
    assertThat(Joueur.BLANC.getScore()).isEqualTo(2);
  }

  // ---------------------------------------------------------------------
  // Couverture du moteur de retournement (capture).
  //
  // L'idée : on installe à la main une configuration de plateau (via Case.setPossesseur,
  // package-private), on remet les scores à des valeurs cohérentes, puis on déclenche un coup
  // dont on connaît à l'avance les retournements attendus.
  // ---------------------------------------------------------------------

  /**
   * Remplace l'état du plateau de {@code o} par celui décrit par {@code positions} (un tableau 8x8
   * de {@link Joueur}). Recalcule les scores en conséquence, sans toucher au joueur courant.
   */
  private void installer(Othellier o, Joueur[][] positions) {
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
  }

  /**
   * Vérifie qu'un coup peut retourner <b>plusieurs pions consécutifs</b> dans une même direction.
   * On installe une rangée NOIR - BLANC - BLANC - BLANC - NOIR (rangée 4), puis on joue de nouveau
   * noir en bordure pour vérifier qu'on ne se contente pas du premier pion.
   *
   * <p>Plus précis : on installe seulement la ligne 4 du plateau (le reste reste vide / pions du
   * milieu) et on fait jouer NOIR en (4,0) avec un NOIR en (4,4) pour qu'il puisse capturer les
   * trois BLANC consécutifs en (4,1), (4,2), (4,3).
   */
  @Test
  void un_pion_pose_au_bout_d_une_file_retourne_tous_les_pions_adverses_encadres()
      throws Exception {
    Othellier o = surFxThread(Othellier::new);
    surFxThread(
        () -> {
          Joueur[][] config = new Joueur[Othellier.TAILLE][Othellier.TAILLE];
          // Rangée 4 : NOIR BLANC BLANC BLANC NOIR _ _ _   sauf que (4,0) sera placée par le clic
          // Donc on installe ici : _ BLANC BLANC BLANC NOIR _ _ _
          config[4][1] = Joueur.BLANC;
          config[4][2] = Joueur.BLANC;
          config[4][3] = Joueur.BLANC;
          config[4][4] = Joueur.NOIR;
          installer(o, config);
          o.joueurCourantProperty().set(Joueur.NOIR);
          o.getCase(4, 0).fire();
          return null;
        });
    assertThat(o.getCase(4, 0).getPossesseur())
        .as("le pion noir est posé en (4,0)")
        .isEqualTo(Joueur.NOIR);
    assertThat(o.getCase(4, 1).getPossesseur())
        .as("(4,1) doit être retourné en NOIR")
        .isEqualTo(Joueur.NOIR);
    assertThat(o.getCase(4, 2).getPossesseur())
        .as("(4,2) doit être retourné en NOIR (capture multiple)")
        .isEqualTo(Joueur.NOIR);
    assertThat(o.getCase(4, 3).getPossesseur())
        .as("(4,3) doit être retourné en NOIR (capture multiple)")
        .isEqualTo(Joueur.NOIR);
    assertThat(o.getCase(4, 4).getPossesseur())
        .as("(4,4) reste noir, c'est le pion fermant")
        .isEqualTo(Joueur.NOIR);
  }

  /**
   * Capture en <b>diagonale</b> : on installe une diagonale NOIR (0,0) - BLANC (1,1) - BLANC (2,2)
   * - NOIR fermant en (3,3), puis on joue noir en (0,0) - non, plutôt l'inverse : on laisse NOIR en
   * (3,3) et on joue NOIR en (0,0). Cela exerce l'une des 4 directions diagonales du tableau {@code
   * DIRECTIONS} de {@link Othellier}.
   */
  @Test
  void un_pion_pose_au_bout_d_une_diagonale_retourne_les_pions_adverses_encadres()
      throws Exception {
    Othellier o = surFxThread(Othellier::new);
    surFxThread(
        () -> {
          Joueur[][] config = new Joueur[Othellier.TAILLE][Othellier.TAILLE];
          // (0,0) sera placée par le clic
          config[1][1] = Joueur.BLANC;
          config[2][2] = Joueur.BLANC;
          config[3][3] = Joueur.NOIR;
          installer(o, config);
          o.joueurCourantProperty().set(Joueur.NOIR);
          o.getCase(0, 0).fire();
          return null;
        });
    assertThat(o.getCase(1, 1).getPossesseur())
        .as("la diagonale (1,1) doit avoir basculé en NOIR")
        .isEqualTo(Joueur.NOIR);
    assertThat(o.getCase(2, 2).getPossesseur())
        .as("la diagonale (2,2) doit avoir basculé en NOIR")
        .isEqualTo(Joueur.NOIR);
  }

  /**
   * Capture <b>simultanée dans plusieurs directions</b> : on installe une croix où le pion posé
   * encadre deux pions BLANC sur deux axes différents (vertical descendant et horizontal droite).
   * Les deux directions doivent être retournées par le même coup.
   */
  @Test
  void un_meme_coup_retourne_les_pions_dans_plusieurs_directions_simultanement() throws Exception {
    Othellier o = surFxThread(Othellier::new);
    surFxThread(
        () -> {
          Joueur[][] config = new Joueur[Othellier.TAILLE][Othellier.TAILLE];
          // (2,2) sera placée par le clic ; encadre BLANC à droite et BLANC en bas.
          config[2][3] = Joueur.BLANC; // pion à capturer (direction droite)
          config[2][4] = Joueur.NOIR; // pion fermant droite
          config[3][2] = Joueur.BLANC; // pion à capturer (direction bas)
          config[4][2] = Joueur.NOIR; // pion fermant bas
          installer(o, config);
          o.joueurCourantProperty().set(Joueur.NOIR);
          o.getCase(2, 2).fire();
          return null;
        });
    assertThat(o.getCase(2, 3).getPossesseur())
        .as("le pion blanc (2,3) doit être retourné (direction horizontale droite)")
        .isEqualTo(Joueur.NOIR);
    assertThat(o.getCase(3, 2).getPossesseur())
        .as("le pion blanc (3,2) doit être retourné (direction verticale bas)")
        .isEqualTo(Joueur.NOIR);
  }

  /**
   * Une file qui se termine par une case <b>vide</b> avant de rencontrer un pion fermant ne doit
   * <b>rien capturer</b>. C'est la condition qui empêche les captures « ouvertes ».
   */
  @Test
  void un_coup_dont_la_file_se_termine_par_une_case_vide_ne_capture_aucun_pion() throws Exception {
    Othellier o = surFxThread(Othellier::new);
    surFxThread(
        () -> {
          Joueur[][] config = new Joueur[Othellier.TAILLE][Othellier.TAILLE];
          // (4,0) sera tentée par le clic ; à droite : BLANC en (4,1), VIDE en (4,2).
          config[4][1] = Joueur.BLANC;
          // (4,2) reste PERSONNE -> la file n'est pas fermée
          installer(o, config);
          o.joueurCourantProperty().set(Joueur.NOIR);
          o.getCase(4, 0).fire();
          return null;
        });
    assertThat(o.getCase(4, 0).getPossesseur())
        .as("le coup en (4,0) est illégal : la case doit rester vide")
        .isEqualTo(Joueur.PERSONNE);
    assertThat(o.getCase(4, 1).getPossesseur())
        .as("le pion blanc (4,1) ne doit pas avoir été retourné")
        .isEqualTo(Joueur.BLANC);
  }

  /**
   * Une file de pions adverses qui touche le <b>bord du plateau</b> avant de rencontrer un pion
   * fermant ne capture rien non plus. Vérifie que {@code estIndicesValides} coupe bien la
   * propagation hors plateau.
   */
  @Test
  void un_coup_dont_la_file_atteint_le_bord_sans_pion_fermant_ne_capture_aucun_pion()
      throws Exception {
    Othellier o = surFxThread(Othellier::new);
    surFxThread(
        () -> {
          Joueur[][] config = new Joueur[Othellier.TAILLE][Othellier.TAILLE];
          // (5,5) sera tentée par le clic ; vers le bas : BLANC en (6,6) puis (7,7) BLANC,
          // et plus rien après (bord).
          config[6][6] = Joueur.BLANC;
          config[7][7] = Joueur.BLANC;
          installer(o, config);
          o.joueurCourantProperty().set(Joueur.NOIR);
          o.getCase(5, 5).fire();
          return null;
        });
    assertThat(o.getCase(5, 5).getPossesseur())
        .as("le coup en (5,5) est illégal : aucun pion noir n'encadre la file")
        .isEqualTo(Joueur.PERSONNE);
    assertThat(o.getCase(7, 7).getPossesseur())
        .as("(7,7) reste BLANC, aucun retournement")
        .isEqualTo(Joueur.BLANC);
  }
}
