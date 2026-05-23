package app.controller;

import app.model.Emprestimo;
import app.model.TipoEmprestimo;
import app.service.EmprestimoService;
import app.util.FormatadorData;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;

public class EmprestimoDialogController {

    @FXML
    private Label lblTitulo;
    @FXML
    private ToggleButton btnEmprestei;
    @FXML
    private ToggleButton btnPedi;
    @FXML
    private TextField txtNome;
    @FXML
    private TextField txtValor;
    @FXML
    private DatePicker datePagamento;
    @FXML
    private Label lblErro;
    @FXML
    private Button btnExcluir;
    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnSalvar;

    private EmprestimoService service;
    private ToggleGroup tipoGroup;
    private Emprestimo emprestimoEmEdicao;
    private boolean confirmado;

    @FXML
    public void initialize() {
        service = new EmprestimoService();
        tipoGroup = new ToggleGroup();
        btnEmprestei.setToggleGroup(tipoGroup);
        btnPedi.setToggleGroup(tipoGroup);
        btnEmprestei.setSelected(true);
        FormatadorData.configurar(datePagamento);
        datePagamento.setValue(LocalDate.now().plusMonths(1));
        configurarMascaraMoeda();

        btnSalvar.setOnAction(e -> salvar());
        btnCancelar.setOnAction(e -> fechar(false));
        btnExcluir.setOnAction(e -> excluir());
    }

    public void configurarParaCriar() {
        lblTitulo.setText("Novo empréstimo");
        emprestimoEmEdicao = null;
        btnExcluir.setVisible(false);
        btnExcluir.setManaged(false);
    }

    public void configurarParaEditar(Emprestimo emprestimo) {
        lblTitulo.setText("Editar empréstimo");
        emprestimoEmEdicao = emprestimo;
        if (emprestimo.tipo() == TipoEmprestimo.Emprestei) {
            btnEmprestei.setSelected(true);
        } else {
            btnPedi.setSelected(true);
        }
        txtNome.setText(emprestimo.nome());
        txtValor.setText(formatarValor(emprestimo.valorCentavos()));
        datePagamento.setValue(emprestimo.dataPagamento());
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
                    emprestimoEmEdicao != null ? emprestimoEmEdicao.id() : null,
                    btnEmprestei.isSelected() ? TipoEmprestimo.Emprestei : TipoEmprestimo.PediEmprestado,
                    txtNome.getText(),
                    parseValorCentavos(txtValor.getText()),
                    datePagamento.getValue()
            );
            fechar(true);
        } catch (RuntimeException e) {
            mostrarErro(e.getMessage());
        }
    }

    private void excluir() {
        if (emprestimoEmEdicao == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Excluir empréstimo");
        confirm.setHeaderText(null);
        confirm.setContentText("Tem certeza que deseja excluir \"" + emprestimoEmEdicao.nome() + "\"?");

        confirm.showAndWait().ifPresent(resposta -> {
            if (resposta == ButtonType.OK) {
                service.deletar(emprestimoEmEdicao.id());
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
