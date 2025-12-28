package app;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import utils.RelatorioHelper;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {
    /**
     * @param tipo      "Entrada" ou "Saída"
     * @param valor     Valor digitado pelo usuário
     * @param categoria "Salário", "Aluguel", etc.
     * @param meta      Nome da meta
     */
    public record Transacao(String tipo, double valor, String categoria, String meta) {

        @Override
        public String toString() {
            if (meta != null && !meta.isEmpty()) {
                return tipo + " - R$ " + String.format("%.2f", valor)
                        + " (" + categoria + " → Meta: " + meta + ")";
            }
            return tipo + " - R$ " + String.format("%.2f", valor) + " (" + categoria + ")";
        }
    }

    static class Meta {
        private final String nome;
        private final Double valorAlvo;
        private double valorAtual;

        public Meta(String nome, Double valorAlvo) {
            this.nome = nome;
            this.valorAlvo = valorAlvo;
            this.valorAtual = 0.0;
        }

        public String getNome() {
            return nome;
        }

        public boolean possuiAlvo() {
            return valorAlvo != null;
        }

        public boolean estaAlcancada() {
            return possuiAlvo() && valorAtual >= valorAlvo;
        }

        public double restanteParaAlvo() {
            if (!possuiAlvo()) return Double.MAX_VALUE;
            return Math.max(valorAlvo - valorAtual, 0);
        }

        public void adicionarValor(double v) {
            valorAtual += v;
        }

        public double progresso() {
            if (valorAlvo == null || valorAlvo == 0) return 0;
            return Math.min(valorAtual / valorAlvo, 1.0);
        }

        @Override
        public String toString() {
            if (!possuiAlvo()) {
                return "R$ " + String.format("%.2f", valorAtual);
            }
            return "R$ " + String.format("%.2f", valorAtual)
                    + " / R$ " + String.format("%.2f", valorAlvo);
        }
    }

    private static final String ARQUIVO = "transacoes.txt";
    private static final String ARQUIVO_METAS = "metas.txt";
    private final List<Transacao> transacoes = new ArrayList<>();
    private final List<Meta> metas = new ArrayList<>();
    private final ListView<String> listaTransacoes = new ListView<>();
    private final VBox painelMetas = new VBox(10);
    private final Label lblSaldo = new Label("Saldo: R$ 0,00");

    @Override
    public void start(Stage stage) {
        carregarMetas();
        carregarTransacoes();
        atualizarRelatorio();

        Button btnNova = new Button("Nova Transação");
        Button btnMeta = new Button("Nova Meta");

        btnNova.setOnAction(_ -> NovaTransacao());
        btnMeta.setOnAction(_ -> NovaMeta());

        HBox botoes = new HBox(10, btnNova, btnMeta);

        VBox layout = new VBox(
                10,
                botoes,
                new Label("Metas"), painelMetas,
                new Label("Transações"), listaTransacoes,
                lblSaldo
        );

        layout.setStyle("-fx-padding: 20; -fx-alignment: center; -fx-background-color: #e0efda; -fx-font-size: 14px;");

        atualizarUI();

        Scene scene = new Scene(layout, 500, 650);
        stage.setTitle("Aplicativo de Finanças");
        stage.getIcons().add(new Image("file:icon.png"));
        stage.setScene(scene);
        stage.show();
    }

    private void NovaMeta() { // Referente ao botão de nova meta
        Stage janela = new Stage();

        TextField txtNome = new TextField();
        TextField txtAlvo = new TextField();

        Button salvar = BotaoSalvarMeta(txtNome, txtAlvo, janela);

        VBox layout = new VBox(
                10,
                new Label("Nome da Meta"), txtNome,
                new Label("Valor Alvo"), txtAlvo,
                salvar
        );

        layout.setStyle("-fx-padding: 20; -fx-alignment: center; -fx-background-color: #e0efda; -fx-text-fill: red;");
        janela.setScene(new Scene(layout, 300, 250));
        janela.show();
    }

    private Button BotaoSalvarMeta(TextField txtNome, TextField txtAlvo, Stage janela) {
        Button salvar = new Button("Salvar");

        salvar.setOnAction(_ -> {
            try {
                String nome = txtNome.getText();
                Double alvo = txtAlvo.getText().isEmpty()
                        ? null
                        : Double.parseDouble(txtAlvo.getText());

                metas.add(new Meta(nome, alvo));
                atualizarUI();
                janela.close();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Dados inválidos").showAndWait();
            }
        });
        return salvar;
    }

    public void NovaTransacao() { // Referente ao botão de nova transação
        Stage janela = new Stage();

        ComboBox<String> cbTipo = new ComboBox<>(); // Caixinha de tipo e valores
        cbTipo.getItems().addAll("Entrada", "Saída");

        ComboBox<String> cbCategoria = new ComboBox<>(); // Caixinha categoria

        ComboBox<Meta> cbMetas = new ComboBox<>(); // Caixinha de metas

        cbMetas.getItems().addAll(metas);
        cbMetas.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(Meta meta, boolean empty) {
                super.updateItem(meta, empty);

                if (empty || meta == null) {
                    setText(null);
                } else {
                    if (meta.possuiAlvo()) {
                        setText(
                                meta.getNome()
                                        + ": R$ "
                                        + String.format("%.2f", meta.valorAtual)
                                        + " / R$ "
                                        + String.format("%.2f", meta.valorAlvo)
                        );
                    } else {
                        setText(
                                meta.getNome()
                                        + "  R$ "
                                        + String.format("%.2f", meta.valorAtual)
                        );
                    }
                }
            }
        });

        cbMetas.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Meta meta, boolean empty) {
                super.updateItem(meta, empty);

                if (empty || meta == null) {
                    setText(null);
                } else {
                    if (meta.possuiAlvo()) {
                        setText(
                                meta.getNome()
                                        + " — R$ "
                                        + String.format("%.2f", meta.valorAtual)
                                        + " / R$ "
                                        + String.format("%.2f", meta.valorAlvo)
                        );
                    } else {
                        setText(
                                meta.getNome()
                                        + " R$ "
                                        + String.format("%.2f", meta.valorAtual)
                        );
                    }
                }
            }
        });

        TextField txtValor = new TextField();

        cbTipo.valueProperty().addListener((_, _, n) -> {
            cbCategoria.getItems().clear();
            if ("Entrada".equals(n)) {
                cbCategoria.getItems().addAll("Salário", "Empréstimo", "Adicionar à Meta", "Outros");
            } else if ("Saída".equals(n)) {
                cbCategoria.getItems().addAll("Aluguel", "Alimentação", "Internet",
                        "Conta de água", "Conta de luz", "Compras", "Outros");
            }
        });

        Button salvar = new Button("Salvar");

        salvar.setOnAction(_ -> {
            try {
                String tipo = cbTipo.getValue();
                String categoria = cbCategoria.getValue();
                double valor = Double.parseDouble(txtValor.getText());

                Meta metaSelecionada = cbMetas.getValue();
                String nomeMeta = metaSelecionada != null ? metaSelecionada.getNome() : "";

                if ("Entrada".equals(tipo) && "Adicionar à Meta".equals(categoria)) {

                    if (metaSelecionada == null) {
                        throw new IllegalArgumentException("Selecione uma meta");
                    }

                    if (metaSelecionada.estaAlcancada()) {
                        new Alert(Alert.AlertType.INFORMATION, "Meta alcançada").showAndWait();
                        return;
                    }

                    double saldoAtual = calcularSaldoAtual();

                    double limiteSaldo = Math.min(valor, saldoAtual);
                    if (limiteSaldo <= 0) {
                        new Alert(Alert.AlertType.WARNING, "Saldo insuficiente").showAndWait();
                        return;
                    }

                    double limiteMeta = Math.min(
                            limiteSaldo,
                            metaSelecionada.restanteParaAlvo()
                    );

                    if (limiteMeta <= 0) {
                        new Alert(Alert.AlertType.INFORMATION, "Meta alcançada").showAndWait();
                        return;
                    }

                    metaSelecionada.adicionarValor(limiteMeta);

                    transacoes.add(
                            new Transacao("Saída", limiteMeta, "Adicionar à Meta", nomeMeta)
                    );

                } else {
                    transacoes.add(new Transacao(tipo, valor, categoria, nomeMeta));
                }

                salvarTransacoes();
                salvarMetas();
                atualizarUI();
                atualizarRelatorio();
                janela.close();

            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Dados inválidos").showAndWait();
            }
        });

        VBox layout = new VBox(
                10,
                new Label("Tipo"), cbTipo,
                new Label("Valor"), txtValor,
                new Label("Categoria"), cbCategoria,
                new Label("Meta"), cbMetas,
                salvar
        );

        layout.setStyle("-fx-padding: 20;");
        janela.setScene(new Scene(layout, 300, 350));
        janela.show();
    }

    private void atualizarRelatorio() {
        listaTransacoes.getItems().clear();
        for (Transacao t : transacoes) {
            listaTransacoes.getItems().add(t.toString());
        }
        double saldo = RelatorioHelper.calcularSaldo(transacoes);
        lblSaldo.setText("Saldo: R$ " + String.format("%.2f", saldo));
    }

    private void atualizarUI() {
        listaTransacoes.getItems().clear();
        for (Transacao t : transacoes) {
            listaTransacoes.getItems().add(t.toString());
        }

        painelMetas.getChildren().clear();
        for (Meta m : metas) {
            Label nome = new Label(m.getNome());
            nome.setMaxWidth(Double.MAX_VALUE); // nome.set para centralizar o título
            nome.setAlignment(Pos.CENTER);
            Label valor = new Label(m.toString());
            valor.setMaxWidth(Double.MAX_VALUE); // valor.set para centralizar o valor R$
            valor.setAlignment(Pos.CENTER);

            ProgressBar pb = new ProgressBar(m.progresso());

            VBox card = new VBox(5, nome, valor, pb);
            card.setStyle("""
                    -fx-padding: 10;
                    -fx-background-color: #dddddd;
                    -fx-background-radius: 10;
                    """);

            painelMetas.getChildren().add(card);
        }

        double saldo = transacoes.stream()
                .mapToDouble(t -> "Entrada".equals(t.tipo()) ? t.valor() : -t.valor())
                .sum();

        lblSaldo.setText("Saldo: R$ " + String.format("%.2f", saldo));
    }

    private void carregarTransacoes() { // Carrega o txt "transacoes"
        File arq = new File(ARQUIVO);
        if (!arq.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(arq))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] p = linha.split(";");
                transacoes.add(new Transacao(
                        p[0],
                        Double.parseDouble(p[1]),
                        p[2],
                        p.length > 3 ? p[3] : ""
                ));
            }
        } catch (Exception ignored) {
        }
    }

    private void salvarTransacoes() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARQUIVO))) {
            for (Transacao t : transacoes) {
                pw.println(t.tipo() + ";" + t.valor() + ";" + t.categoria() + ";" + t.meta());
            }
        } catch (Exception ignored) {
        }
    }

    private void salvarMetas() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARQUIVO_METAS))) {
            for (Meta m : metas) {
                pw.println(m.getNome() + ";" + (m.valorAlvo == null ? "null" : m.valorAlvo) + ";" + m.valorAtual);
            }
        } catch (Exception ignored) {
        }
    }

    private void carregarMetas() {
        File arq = new File(ARQUIVO_METAS);
        if (!arq.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(arq))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] p = linha.split(";");
                Double alvo = "null".equals(p[1]) ? null : Double.parseDouble(p[1]);
                Meta m = new Meta(p[0], alvo);
                m.valorAtual = Double.parseDouble(p[2]);
                metas.add(m);
            }
        } catch (Exception ignored) {
        }
    }

    private double calcularSaldoAtual() {
        double saldo = 0;

        for (Transacao t : transacoes) {
            if ("Entrada".equals(t.tipo())) {
                saldo += t.valor();
            } else {
                saldo -= t.valor();
            }
        }

        return saldo;
    }

    public static void main(String[] args) {
        launch();
    }
}
