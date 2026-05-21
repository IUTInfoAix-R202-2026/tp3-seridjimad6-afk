package fr.univ_amu.iut.exercice7;

import java.time.LocalTime;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

/**
 * Contrôleur de la pierre angulaire MVC (parcours P3 - vérification d'une nuit de capture par
 * échantillonnage).
 *
 * <p>L'instance possède son propre modèle ({@link NuitVerification}). Le FXML s'occupe de la
 * structure, le contrôleur du câblage modèle ↔ vue.
 */
public class QualificationController {

  @FXML private TableView<Sequence> tableView;

  @FXML private TableColumn<Sequence, LocalTime> colHorodatage;

  @FXML private TableColumn<Sequence, Number> colFrequence;

  @FXML private TableColumn<Sequence, Number> colDuree;

  @FXML private TableColumn<Sequence, String> colStatut;

  @FXML private Label labelSelection;

  @FXML private Button boutonEcouter;

  @FXML private Label labelLecture;

  @FXML private ChoiceBox<String> choiceBoxVerdict;

  @FXML private TextArea zoneCommentaire;

  @FXML private Label labelVerdictGlobal;

  private final NuitVerification nuit = NuitVerification.genererJeu(10);

  /**
   * Méthode appelée automatiquement après injection des champs {@code @FXML}. Tout le câblage MVC
   * se passe ici.
   */
  @FXML
  private void initialize() {
    // Étape 1 : alimenter la TableView
    colHorodatage.setCellValueFactory(c -> c.getValue().horodatageProperty());
    colFrequence.setCellValueFactory(c -> c.getValue().frequenceDominanteKHzProperty());
    colDuree.setCellValueFactory(c -> c.getValue().dureeSecondesProperty());
    colStatut.setCellValueFactory(c -> c.getValue().statutProperty());
    tableView.setItems(nuit.getSequences());

    // Étape 2 : afficher la séquence sélectionnée
    tableView
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldSeq, newSeq) -> {
              if (newSeq == null) {
                labelSelection.setText("(sélectionnez une séquence dans le tableau)");
              } else {
                labelSelection.setText(
                    String.format(
                        "Séquence %s - %.1f kHz",
                        newSeq.getHorodatage().toString(), newSeq.getFrequenceDominanteKHz()));
              }
            });
    labelSelection.setText("(sélectionnez une séquence dans le tableau)");
    labelLecture.setText("");

    // Étape 3 : bouton Écouter activé seulement si sélection
    boutonEcouter
        .disableProperty()
        .bind(tableView.getSelectionModel().selectedItemProperty().isNull());

    // Étape 4 : peupler la ChoiceBox
    choiceBoxVerdict.getItems().addAll("OK", "Douteux", "À jeter");

    // Étape 5 : lier labelVerdictGlobal au verdict du modèle
    labelVerdictGlobal
        .textProperty()
        .bind(
            javafx.beans.binding.Bindings.when(nuit.verdictGlobalProperty().isEmpty())
                .then("Verdict global : (à saisir)")
                .otherwise(
                    javafx.beans.binding.Bindings.concat(
                        "Verdict global : ", nuit.verdictGlobalProperty())));

    // Étape 6 : liaison bidirectionnelle TextArea ↔ modèle
    zoneCommentaire.textProperty().bindBidirectional(nuit.commentaireProperty());
  }

  @FXML
  private void ecouter() {
    Sequence seq = tableView.getSelectionModel().getSelectedItem();
    if (seq == null) return;
    seq.setStatut("Écoutée");
    labelLecture.setText("Lecture en cours...");
    javafx.animation.PauseTransition pause =
        new javafx.animation.PauseTransition(javafx.util.Duration.millis(600));
    pause.setOnFinished(e -> labelLecture.setText(""));
    pause.play();
  }

  @FXML
  private void enregistrerVerdict() {
    String verdict = choiceBoxVerdict.getValue();
    if (verdict != null) {
      nuit.setVerdictGlobal(verdict);
    }
  }

  /** Exposé pour les tests : permet de vérifier l'état du modèle après actions sur la vue. */
  public NuitVerification getNuit() {
    return nuit;
  }
}
