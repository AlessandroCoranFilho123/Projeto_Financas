package app.controller;

import app.model.Emprestimo;
import app.model.TipoEmprestimo;
import app.service.EmprestimoService;
import app.util.CssManager;
import app.util.FormatadorData;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.text.NumberFormat;
import java.util.Locale;

public class EmprestimosViewController {

    @FXML
    private Button btnNovo;
    @FXML
    private Label lblTotalEmprestei;
    @FXML
    private Label lblTotalPedi;
    @FXML
    private VBox boxEmprestei;
    @FXML
    private VBox boxPedi;

    private EmprestimoService service;
    private final NumberFormat currencyFormatter =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    @FXML
    public void initialize() {
        service = new EmprestimoService();
        btnNovo.setOnAction(e -> abrirDialog(null));
        carregarEmprestimos();
    }

    private void carregarEmprestimos() {
        boxEmprestei.getChildren().setAll(service.listarPorTipo(TipoEmprestimo.Emprestei).stream()
                .map(this::criarBox)
                .toList());
        boxPedi.getChildren().setAll(service.listarPorTipo(TipoEmprestimo.PediEmprestado).stream()
                .map(this::criarBox)
                .toList());

        lblTotalEmprestei.setText(currencyFormatter.format(
                service.totalPorTipoCentavos(TipoEmprestimo.Emprestei) / 100.0));
        lblTotalPedi.setText(currencyFormatter.format(
                service.totalPorTipoCentavos(TipoEmprestimo.PediEmprestado) / 100.0));
    }

    private VBox criarBox(Emprestimo emprestimo) {
        Label nome = new Label(emprestimo.nome());
        nome.getStyleClass().add("planning-card-title");

        Label valor = new Label(currencyFormatter.format(emprestimo.valorCentavos() / 100.0));
        valor.getStyleClass().add("planning-card-value");

        Label data = new Label("Pagamento: " + FormatadorData.formatar(emprestimo.dataPagamento()));
        data.getStyleClass().add("planning-card-subtitle");

        Button editar = new Button("Editar");
        editar.getStyleClass().add("button-secondary");
        editar.setOnAction(e -> abrirDialog(emprestimo));

        Button excluir = new Button("Excluir");
        excluir.getStyleClass().add("button-excluir");
        excluir.setOnAction(e -> confirmarExclusao(emprestimo));

        HBox actions = new HBox(8, editar, excluir);
        VBox box = new VBox(8, nome, valor, data, actions);
        box.setPadding(new Insets(16));
        box.getStyleClass().addAll(
                "loan-box",
                emprestimo.tipo() == TipoEmprestimo.Emprestei ? "loan-box-emprestei" : "loan-box-pedi"
        );
        box.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                abrirDialog(emprestimo);
            }
        });
        return box;
    }

    private void confirmarExclusao(Emprestimo emprestimo) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Excluir empréstimo");
        confirm.setHeaderText(null);
        confirm.setContentText("Tem certeza que deseja excluir \"" + emprestimo.nome() + "\"?");

        confirm.showAndWait().ifPresent(resposta -> {
            if (resposta == ButtonType.OK) {
                service.deletar(emprestimo.id());
                carregarEmprestimos();
            }
        });
    }

    private void abrirDialog(Emprestimo emprestimo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/view/emprestimo_dialog.fxml"));
            VBox root = loader.load();
            EmprestimoDialogController controller = loader.getController();
            if (emprestimo == null) {
                controller.configurarParaCriar();
            } else {
                controller.configurarParaEditar(emprestimo);
            }

            Stage stage = new Stage();
            stage.setTitle(emprestimo == null ? "Novo empréstimo" : "Editar empréstimo");
            stage.initModality(Modality.APPLICATION_MODAL);
            Window owner = btnNovo.getScene() != null ? btnNovo.getScene().getWindow() : null;
            if (owner != null) {
                stage.initOwner(owner);
            }

            Scene scene = new Scene(root);
            CssManager.aplicarCss(scene);
            if (owner != null && owner.getScene() != null
                    && owner.getScene().getRoot().getStyleClass().contains("dark-theme")) {
                root.getStyleClass().add("dark-theme");
            }
            stage.setScene(scene);
            stage.setResizable(false);
            app.util.IconManager.setAppIcon(stage);
            stage.showAndWait();

            if (controller.isConfirmado()) {
                carregarEmprestimos();
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao abrir emprestimo", e);
        }
    }
}
