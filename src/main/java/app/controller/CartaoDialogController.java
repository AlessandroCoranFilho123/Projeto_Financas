package app.controller;

import app.model.CompraCartaoCredito;
import app.service.CartaoCreditoService;
import app.util.FormatadorData;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;

public class CartaoDialogController {

    @FXML
    private Label lblTitulo;
    @FXML
    private TextField txtNome;
    @FXML
    private TextField txtValor;
    @FXML
    private TextField txtParcelas;
    @FXML
    private DatePicker dateVencimento;
    @FXML
    private Label lblErro;
    @FXML
    private Button btnExcluir;
    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnSalvar;

    private CartaoCreditoService service;
    private CompraCartaoCredito compraEmEdicao;
    private boolean confirmado;

    @FXML
    public void initialize() {
        service = new CartaoCreditoService();
        txtParcelas.setText("1");
        FormatadorData.configurar(dateVencimento);
        dateVencimento.setValue(LocalDate.now().plusMonths(1));
        configurarMascaraMoeda();
        configurarMascaraParcelas();

        btnSalvar.setOnAction(e -> salvar());
        btnCancelar.setOnAction(e -> fechar(false));
        btnExcluir.setOnAction(e -> excluir());
    }

    public void configurarParaCriar() {
        lblTitulo.setText("Nova compra no cartão");
        compraEmEdicao = null;
        btnExcluir.setVisible(false);
        btnExcluir.setManaged(false);
    }

    public void configurarParaEditar(CompraCartaoCredito compra) {
        lblTitulo.setText("Editar compra no cartão");
        compraEmEdicao = compra;
        txtNome.setText(compra.nome());
        txtValor.setText(formatarValor(compra.valorTotalCentavos()));
        txtParcelas.setText(String.valueOf(compra.parcelas()));
        dateVencimento.setValue(compra.vencimento());
        btnExcluir.setVisible(true);
        btnExcluir.setManaged(true);
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    private void salvar() {
        limparErro();
        try {
            service.salvar(
                    compraEmEdicao != null ? compraEmEdicao.id() : null,
                    txtNome.getText(),
                    parseValorCentavos(txtValor.getText()),
                    parseParcelas(txtParcelas.getText()),
                    dateVencimento.getValue()
            );
            fechar(true);
        } catch (RuntimeException e) {
            mostrarErro(e.getMessage());
        }
    }

    private void excluir() {
        if (compraEmEdicao == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Excluir compra");
        confirm.setHeaderText(null);
        confirm.setContentText("Tem certeza que deseja excluir \"" + compraEmEdicao.nome() + "\"?");

        confirm.showAndWait().ifPresent(resposta -> {
            if (resposta == ButtonType.OK) {
                service.deletar(compraEmEdicao.id());
                fechar(true);
            }
        });
    }

    private void configurarMascaraMoeda() {
        txtValor.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("[0-9.,]*")) {
                txtValor.setText(oldValue);
            }
        });
    }

    private void configurarMascaraParcelas() {
        txtParcelas.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtParcelas.setText(oldValue);
            }
        });
    }

    private int parseParcelas(String texto) {
        if (texto == null || texto.isBlank()) return 0;
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parcelas devem conter apenas números");
        }
    }

    private long parseValorCentavos(String texto) {
        if (texto == null || texto.trim().isEmpty()) return 0;
        try {
            String normalizado = texto.replace("R$", "").trim();
            if (normalizado.contains(",")) {
                normalizado = normalizado.replace(".", "").replace(",", ".");
            }
            return Math.round(Double.parseDouble(normalizado) * 100);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor invalido");
        }
    }

    private String formatarValor(long centavos) {
        return String.format("%.2f", centavos / 100.0);
    }

    private void mostrarErro(String mensagem) {
        lblErro.setText(mensagem);
        lblErro.setVisible(true);
        lblErro.setManaged(true);
    }

    private void limparErro() {
        lblErro.setVisible(false);
        lblErro.setManaged(false);
    }

    private void fechar(boolean confirmado) {
        this.confirmado = confirmado;
        ((Stage) btnCancelar.getScene().getWindow()).close();
    }
}
