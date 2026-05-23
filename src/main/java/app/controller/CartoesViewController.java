package app.controller;

import app.model.CompraCartaoCredito;
import app.model.StatusVencimento;
import app.service.CartaoCreditoService;
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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class CartoesViewController {

    @FXML
    private Button btnNova;
    @FXML
    private Label lblTotalAberto;
    @FXML
    private Label lblParcelasMes;
    @FXML
    private Label lblVencendo;
    @FXML
    private Label lblAviso;
    @FXML
    private FlowPane flowCartoes;

    private CartaoCreditoService service;
    private final NumberFormat currencyFormatter =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    @FXML
    public void initialize() {
        service = new CartaoCreditoService();
        btnNova.setOnAction(e -> abrirDialog(null));
        carregarCompras();
    }

    private void carregarCompras() {
        List<CompraCartaoCredito> compras = service.listarTodas();
        flowCartoes.getChildren().setAll(compras.stream()
                .map(this::criarCard)
                .toList());
        atualizarResumo(compras);
    }

    private VBox criarCard(CompraCartaoCredito compra) {
        LocalDate hoje = LocalDate.now();
        StatusVencimento status = compra.statusVencimento(hoje);

        Label nome = new Label(compra.nome());
        nome.getStyleClass().add("planning-card-title");

        Label valor = new Label(currencyFormatter.format(compra.valorTotalCentavos() / 100.0));
        valor.getStyleClass().add("planning-card-value");

        Label parcela = new Label(
                "Parcela: " + currencyFormatter.format(compra.valorParcelaCentavos() / 100.0)
                        + " / " + compra.parcelas() + " mes(es)"
        );
        parcela.getStyleClass().add("planning-card-subtitle");

        Label vencimento = new Label("Vencimento: " + FormatadorData.formatar(compra.vencimento()));
        vencimento.getStyleClass().add("planning-card-subtitle");

        Label aviso = new Label(compra.avisoVencimento(hoje));
        aviso.getStyleClass().add("planning-card-warning");
        aviso.setVisible(!aviso.getText().isBlank());
        aviso.setManaged(!aviso.getText().isBlank());

        Button editar = new Button("Editar");
        editar.getStyleClass().add("button-secondary");
        editar.setOnAction(e -> abrirDialog(compra));

        Button excluir = new Button("Excluir");
        excluir.getStyleClass().add("button-excluir");
        excluir.setOnAction(e -> confirmarExclusao(compra));

        HBox actions = new HBox(8, editar, excluir);

        VBox card = new VBox(8, nome, valor, parcela, vencimento, aviso, actions);
        card.setPadding(new Insets(18));
        card.setPrefWidth(260);
        card.getStyleClass().addAll("planning-card", "cartao-" + status.name().toLowerCase(Locale.ROOT));
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                abrirDialog(compra);
            }
        });
        return card;
    }

    private void atualizarResumo(List<CompraCartaoCredito> compras) {
        LocalDate hoje = LocalDate.now();
        long totalAberto = compras.stream()
                .mapToLong(CompraCartaoCredito::valorTotalCentavos)
                .sum();
        long totalParcelas = compras.stream()
                .mapToLong(CompraCartaoCredito::valorParcelaCentavos)
                .sum();
        long vencendo = compras.stream()
                .filter(compra -> {
                    long dias = compra.diasAteVencimento(hoje);
                    return dias >= 0 && dias <= 7;
                })
                .count();

        lblTotalAberto.setText(currencyFormatter.format(totalAberto / 100.0));
        lblParcelasMes.setText(currencyFormatter.format(totalParcelas / 100.0));
        lblVencendo.setText(String.valueOf(vencendo));

        boolean exibirAviso = compras.stream()
                .anyMatch(compra -> compra.statusVencimento(hoje) != StatusVencimento.Verde);
        lblAviso.setText("Atenção: há compras com vencimento próximo ou vencido.");
        lblAviso.setVisible(exibirAviso);
        lblAviso.setManaged(exibirAviso);
    }

    private void confirmarExclusao(CompraCartaoCredito compra) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Excluir compra");
        confirm.setHeaderText(null);
        confirm.setContentText("Tem certeza que deseja excluir \"" + compra.nome() + "\"?");

        confirm.showAndWait().ifPresent(resposta -> {
            if (resposta == ButtonType.OK) {
                service.deletar(compra.id());
                carregarCompras();
            }
        });
    }

    private void abrirDialog(CompraCartaoCredito compra) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/view/cartao_dialog.fxml"));
            VBox root = loader.load();
            CartaoDialogController controller = loader.getController();
            if (compra == null) {
                controller.configurarParaCriar();
            } else {
                controller.configurarParaEditar(compra);
            }

            Stage stage = new Stage();
            stage.setTitle(compra == null ? "Nova compra no cartão de crédito" : "Editar compra no cartão de crédito");
            stage.initModality(Modality.APPLICATION_MODAL);
            Window owner = btnNova.getScene() != null ? btnNova.getScene().getWindow() : null;
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
                carregarCompras();
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao abrir compra de cartao", e);
        }
    }
}
