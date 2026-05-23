package app.controller;

import app.model.Carteira;
import app.service.CarteiraService;
import app.util.FormatadorData;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;

public class TransferenciaCarteiraDialogController {

    @FXML
    private ComboBox<Carteira> cmbOrigem;
    @FXML
    private ComboBox<Carteira> cmbDestino;
    @FXML
    private TextField txtValor;
    @FXML
    private DatePicker dateTransferencia;
    @FXML
    private TextArea txtComentario;
    @FXML
    private Label lblErro;
    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnSalvar;

    private CarteiraService carteiraService;
    private boolean confirmado;

    @FXML
    public void initialize() {
        carteiraService = new CarteiraService();
        FormatadorData.configurar(dateTransferencia);
        dateTransferencia.setValue(LocalDate.now());
        configurarMascaraMoeda();
        btnSalvar.setOnAction(e -> salvar());
        btnCancelar.setOnAction(e -> fechar(false));
        carregarCarteiras();
    }

    public void setCarteiraService(CarteiraService carteiraService) {
        this.carteiraService = carteiraService;
        carregarCarteiras();
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    private void carregarCarteiras() {
        if (carteiraService == null || cmbOrigem == null || cmbDestino == null) {
            return;
        }

        var carteiras = FXCollections.observableArrayList(carteiraService.listarCarteiras());
        cmbOrigem.setItems(carteiras);
        cmbDestino.setItems(FXCollections.observableArrayList(carteiras));

        if (carteiras.size() >= 2) {
            cmbOrigem.setValue(carteiras.get(0));
            cmbDestino.setValue(carteiras.get(1));
        }
    }

    private void salvar() {
        limparErro();
        try {
            Carteira origem = cmbOrigem.getValue();
            Carteira destino = cmbDestino.getValue();
            if (origem == null || destino == null) {
                mostrarErro("Selecione origem e destino");
                return;
            }

            carteiraService.transferir(
                    origem.id(),
                    destino.id(),
                    parseValorCentavos(txtValor.getText()),
                    dateTransferencia.getValue(),
                    txtComentario.getText()
            );
            fechar(true);
        } catch (RuntimeException e) {
            mostrarErro(e.getMessage());
        }
    }

    private void configurarMascaraMoeda() {
        txtValor.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("[0-9.,]*")) {
                txtValor.setText(oldValue);
            }
        });
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
            throw new IllegalArgumentException("Valor inválido");
        }
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
