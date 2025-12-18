import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import models.Transacao;
import utils.RelatorioHelper;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {
    private static final String ARQUIVO = "transacoes.txt"; //Inicialmente estou usando o bloco de notas para salvar os dados
    private final List<Transacao> transacoes = new ArrayList<>();
    private final ListView<String> listaTransacoes = new ListView<>();
    private final Label lblSaldo = new Label("Saldo: R$ 0.00");

    @Override
    public void start(Stage stage) {
        carregarTransacoesArquivo(); // Carrega txt "transacoes"
        atualizarRelatorio(); // Atualiza/exibe o histórico de transações

        Button btnNova = new Button("Nova Transação");
        Button btnRelatorio = new Button("Ver Relatório");
        Button btnExcluir = new Button("Excluir");

        btnNova.setOnAction(e -> abrirTelaNovaTransacao());
        btnRelatorio.setOnAction(e -> atualizarRelatorio());
        btnExcluir.setOnAction(e -> excluirTransacaoSelecionada());

        VBox layout = new VBox(10, btnNova, btnRelatorio, btnExcluir, listaTransacoes, lblSaldo);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");

        Scene scene = new Scene(layout, 500, 500);
        stage.setTitle("Aplicativo de finanças - Controle Pessoal");
        stage.getIcons().add(new Image("file:icon.png"));
        stage.setScene(scene);
        stage.show();
    }
    private void carregarTransacoesArquivo() {
        File arquivo = new File(ARQUIVO);

        if (!arquivo.exists()) return; // Se não existir, ele ignora

        try (BufferedReader reader = new BufferedReader(new FileReader(arquivo))) {

            String linha;

            while ((linha = reader.readLine()) != null) {
                String[] partes = linha.split(";"); // Separa valores por ";"

                String tipo = partes[0];
                double valor = Double.parseDouble(partes[1]);
                String categoria = partes[2];

                transacoes.add(new Transacao(tipo, valor, categoria));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Tela de transação
    private void abrirTelaNovaTransacao() {
        Stage janela = new Stage();
        janela.setTitle("Nova Transação");

        Label lblTipo = new Label("Tipo:");
        ComboBox<String> cbTipo = new ComboBox<>();
        cbTipo.getItems().addAll("Entrada", "Saída");

        Label lblValor = new Label("Valor:");
        TextField txtValor = new TextField();

        Label lblCategoria = new Label("Categoria:");
        ComboBox<String> cbCategoria = new ComboBox<>();
        cbCategoria.getItems().addAll("");

        cbTipo.valueProperty().addListener((obs, oldValue, newValue) -> {
            cbCategoria.getItems().clear();

            if ("Entrada".equals(newValue)) {
                cbCategoria.getItems().addAll("Salário", "Empréstimo", "Vale", "Outros");
            } else if ("Saída".equals(newValue)) {
                cbCategoria.getItems().addAll("Aluguel", "Conta de água", "Conta de luz", "Alimentação", "Transporte", "Outros");
            }
        });

        Button btnSalvar = new Button("Salvar");

        btnSalvar.setOnAction(e -> {
            try {
                String tipo = cbTipo.getValue();
                double valor = Double.parseDouble(txtValor.getText());
                String categoria = cbCategoria.getValue();

                Transacao t = new Transacao(tipo, valor, categoria);
                transacoes.add(t);
                salvarTransacoesArquivo();

                janela.close();
                atualizarRelatorio();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Preencha todos os campos corretamente.");
                alert.showAndWait();
            }
        });

        VBox layout = new VBox(10, lblTipo, cbTipo, lblValor, txtValor, lblCategoria, cbCategoria, btnSalvar);
        layout.setStyle("-fx-padding: 20;");

        Scene cena = new Scene(layout, 300, 250);
        janela.setScene(cena);
        janela.show();
    }
    private void excluirTransacaoSelecionada() {
        int index = listaTransacoes.getSelectionModel().getSelectedIndex();

        if (index >= 0) { // Exclui transação do app e do txt e atualiza
            transacoes.remove(index);
            salvarTransacoesArquivo();
            listaTransacoes.getItems().remove(index);
            atualizarRelatorio();
        } else {
            Alert alerta = new Alert(Alert.AlertType.WARNING, "Selecione uma transação para excluir.");
            alerta.showAndWait();
        }
    }
    // Atualiza o histórico de transação
    private void atualizarRelatorio() {
        listaTransacoes.getItems().clear();
        for (Transacao t : transacoes) {
            listaTransacoes.getItems().add(t.toString());
        }
        double saldo = RelatorioHelper.calcularSaldo(transacoes);
        lblSaldo.setText("Saldo: R$ " + String.format("%.2f", saldo));
    }

    // Salvar dados
    private void salvarTransacoesArquivo() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(ARQUIVO))) {

            for (Transacao t : transacoes) {
                writer.println(t.getTipo() + ";" + t.getValor() + ";" + t.getCategoria());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
