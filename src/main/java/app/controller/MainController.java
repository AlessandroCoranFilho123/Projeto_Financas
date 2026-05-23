package app.controller;

import app.model.Meta;
import app.model.Transacao;
import app.repository.CarteiraDAO;
import app.repository.MetaDAO;
import app.repository.TransacaoDAO;
import app.service.CarteiraService;
import app.service.MetaService;
import app.service.TransacaoService;
import app.util.CssManager;
import app.util.IconManager;
import app.view.MetaCell;
import app.view.TransacaoCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

/* Gerencia a navegação entre telas,
   carrega os dados no dashboard,
   abre diálogos e alterna temas (claro/escuro) */

@SuppressWarnings("unused")
public class MainController {
    // Usado para formatar números padrão Brasil
    private static final NumberFormat currencyFormatter =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    // Registrar avisos e erros
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML
    private BorderPane rootContainer;
    @FXML
    private VBox dashboardContent;
    @FXML
    private Button btnInicio;
    @FXML
    private Button btnTransacao;
    @FXML
    private Button btnMetas;
    @FXML
    private Button btnCartoes;
    @FXML
    private Button btnEmprestimos;
    @FXML
    private Button btnTema;
    @FXML
    private ImageView imgTema;
    @FXML
    private Label lblTema;
    @FXML
    private Label lblSaldo;
    @FXML
    private Label lblReceitasMes;
    @FXML
    private Label lblDespesasMes;
    @FXML
    private Label lblSaldoBanco;
    @FXML
    private Label lblSaldoDinheiroFisico;
    @FXML
    private Button btnCarteiras;
    @FXML
    private Button btnTransferirCarteira;
    @FXML
    private ListView<Transacao> listTransacoes;
    @FXML
    private Button btnNovaTransacao;
    @FXML
    private Button btnVerTodasTransacoes;
    @FXML
    private ListView<Meta> listMetas;
    @FXML
    private Button btnNovaMeta;
    @FXML
    private Button btnVerTodasMetas;

    private TransacaoService transacaoService;
    private MetaService metaService;
    private CarteiraService carteiraService;
    private boolean darkTheme = false;
    private static final Preferences PREFS = Preferences.userNodeForPackage(MainController.class);

    @FXML
    public void initialize() {
        try {
            inicializarServicos(); // Cria DAO, MetaService e TransacaoService
            configurarCelulasCustomizadas(); // Define o visual personalizado para os itens das listas
            configurarEventos(); // Configurar ações dos botões
            carregarDados(); // Carrega o DB
            marcarBotaoAtivo(btnInicio); // Botão início selecionado
            aplicarTemaSalvo(); // Restaura tema da sessão anterior

        } catch (Exception e) {
            logger.error("Erro ao inicializar MainController: {}", e.getMessage());
        }
    }

    // Cria TransacaoDAO, MetaDAO, MetaService e TransacaoService
    private void inicializarServicos() {
        TransacaoDAO transacaoDAO = new TransacaoDAO();
        MetaDAO metaDAO = new MetaDAO();
        CarteiraDAO carteiraDAO = new CarteiraDAO();

        carteiraService = new CarteiraService(carteiraDAO, transacaoDAO);
        transacaoService = new TransacaoService(transacaoDAO, metaDAO, carteiraService);
        metaService = new MetaService(metaDAO);
    }

    // Define o visual personalizado para os itens das listas.
    private void configurarCelulasCustomizadas() {
        listTransacoes.setCellFactory(lv -> new TransacaoCell());
        listMetas.setCellFactory(lv -> new MetaCell(true));
    }

    //  Configura todos os botões
    private void configurarEventos() {
        btnInicio.setOnAction(e -> navegarParaInicio());
        btnTema.setOnAction(e -> alternarTema());

        btnTransacao.setOnAction(e -> navegarParaTransacoes());
        btnNovaTransacao.setOnAction(e -> abrirDialogNovaTransacao());
        btnVerTodasTransacoes.setOnAction(e -> navegarParaTransacoes());

        btnMetas.setOnAction(e -> navegarParaMetas());
        btnNovaMeta.setOnAction(e -> abrirDialogNovaMeta());
        btnVerTodasMetas.setOnAction(e -> navegarParaMetas());

        btnCartoes.setOnAction(e -> navegarParaCartoes());
        btnEmprestimos.setOnAction(e -> navegarParaEmprestimos());
        btnCarteiras.setOnAction(e -> abrirDialogCarteiras());
        btnTransferirCarteira.setOnAction(e -> abrirDialogTransferenciaCarteira());

        // Duplo clique abre detalhes transações
        listTransacoes.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Transacao selecionada = listTransacoes.getSelectionModel().getSelectedItem();
                if (selecionada != null) {
                    abrirDetalhesTransacao(selecionada);
                }
            }
        });

        // Duplo clique abre detalhes metas
        listMetas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Meta selecionada = listMetas.getSelectionModel().getSelectedItem();
                if (selecionada != null) {
                    abrirDetalhesMeta(selecionada);
                }
            }
        });
    }

    public void carregarDados() {
        carregarSaldo();
        carregarTransacoesRecentes();
        carregarMetas();
    }

    private void carregarSaldo() {
        try {
            long saldoCentavos = carteiraService.calcularSaldoTotalCentavos();
            lblSaldo.setText(currencyFormatter.format(saldoCentavos / 100.0));
            carregarSaldosCarteiras();

            YearMonth mesAtual = YearMonth.now();
            long receitasCentavos = transacaoService.calcularReceitasMes(mesAtual);
            long despesasCentavos = transacaoService.calcularDespesasMes(mesAtual);

            lblReceitasMes.setText(currencyFormatter.format(receitasCentavos / 100.0));
            lblDespesasMes.setText(currencyFormatter.format(despesasCentavos / 100.0));

        } catch (Exception e) {
            logger.error("Erro ao carregar saldo: {}", e.getMessage());
            lblSaldo.setText("R$ 0,00");
            lblSaldoBanco.setText("R$ 0,00");
            lblSaldoDinheiroFisico.setText("R$ 0,00");
        }
    }

    private void carregarSaldosCarteiras() {
        long saldoBanco = carteiraService.calcularSaldoCarteiraCentavos(CarteiraDAO.BANCO_ID);
        long saldoDinheiro = carteiraService.calcularSaldoCarteiraCentavos(CarteiraDAO.DINHEIRO_FISICO_ID);
        lblSaldoBanco.setText(currencyFormatter.format(saldoBanco / 100.0));
        lblSaldoDinheiroFisico.setText(currencyFormatter.format(saldoDinheiro / 100.0));
    }

    private void carregarTransacoesRecentes() {
        try {
            TransacaoDAO dao = new TransacaoDAO();
            List<Transacao> lista = dao.listarRecentes(10);

            ObservableList<Transacao> transacoes = FXCollections.observableArrayList(lista);
            listTransacoes.setItems(transacoes);

        } catch (Exception e) {
            logger.error("Erro ao carregar transações: {}", e.getMessage());
        }
    }

    private void carregarMetas() {
        try {
            List<Meta> lista = metaService.listarTodasMetas();

            ObservableList<Meta> metas = FXCollections.observableArrayList(lista);
            listMetas.setItems(metas);

        } catch (Exception e) {
            logger.error("Erro ao carregar metas: {}", e.getMessage());
        }
    }

    // Tela inicial (main.fxml)
    private void navegarParaInicio() {
        marcarBotaoAtivo(btnInicio);
        rootContainer.setCenter(dashboardContent);
        carregarDados();
    }

    // Tela transações (transacoes_view.fxml)
    private void navegarParaTransacoes() {
        marcarBotaoAtivo(btnTransacao);
        expandirHistoricoTransacoes();
    }

    // Tela metas (metas_view.fxml)
    private void navegarParaMetas() {
        marcarBotaoAtivo(btnMetas);
        expandirViewMetas();
    }

    private void navegarParaCartoes() {
        marcarBotaoAtivo(btnCartoes);
        expandirViewCartoes();
    }

    private void navegarParaEmprestimos() {
        marcarBotaoAtivo(btnEmprestimos);
        expandirViewEmprestimos();
    }

    // Expande o histórico de transações na própria janela principal
    private void expandirHistoricoTransacoes() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/view/transacoes_view.fxml")
            );

            VBox viewRoot = loader.load();
            TransacoesViewController controller = loader.getController();

            controller.setOnNovaTransacao(this::abrirDialogNovaTransacao);
            controller.setOnEditarTransacao(this::abrirDetalhesTransacao);

            rootContainer.setCenter(viewRoot);

        } catch (Exception e) {
            logger.error("Erro ao expandir histórico de transações: {}", e.getMessage());
        }
    }

    // Expande a tela de metas na própria janela principal
    private void expandirViewMetas() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/view/metas_view.fxml")
            );

            VBox viewRoot = loader.load();
            MetasViewController controller = loader.getController();

            controller.setOnNovaMeta(this::abrirDialogNovaMeta);
            controller.setOnEditarMeta(this::abrirDetalhesMeta);
            rootContainer.setCenter(viewRoot);

        } catch (Exception e) {
            logger.error("Erro ao expandir view de metas: {}", e.getMessage());

        }
    }

    private void expandirViewCartoes() {
        try {
            VBox viewRoot = FXMLLoader.load(
                    getClass().getResource("/app/view/cartoes_view.fxml")
            );
            rootContainer.setCenter(viewRoot);
        } catch (Exception e) {
            logger.error("Erro ao expandir view de cartões: {}", e.getMessage());
        }
    }

    private void expandirViewEmprestimos() {
        try {
            VBox viewRoot = FXMLLoader.load(
                    getClass().getResource("/app/view/emprestimos_view.fxml")
            );
            rootContainer.setCenter(viewRoot);
        } catch (Exception e) {
            logger.error("Erro ao expandir view de empréstimos: {}", e.getMessage());
        }
    }

    // Quando botão for clicado, fica destacado
    private void marcarBotaoAtivo(Button botaoAtivo) {
        btnInicio.getStyleClass().remove("active");
        btnTransacao.getStyleClass().remove("active");
        btnMetas.getStyleClass().remove("active");
        btnCartoes.getStyleClass().remove("active");
        btnEmprestimos.getStyleClass().remove("active");

        if (!botaoAtivo.getStyleClass().contains("active")) {
            botaoAtivo.getStyleClass().add("active");
        }
    }

    @FXML
    private void aplicarTemaSalvo() {
        darkTheme = PREFS.getBoolean("darkTheme", false);
        if (darkTheme) {
            if (rootContainer != null) {
                rootContainer.getStyleClass().add("dark-theme");
            }
            if (imgTema != null) {
                javafx.scene.image.Image icone = IconManager.getImage("/app/icons/tema/claro.png");
                if (icone != null) imgTema.setImage(icone);
            }
            if (lblTema != null) lblTema.setText("Tema claro");
        }
    }

    private void alternarTema() {
        darkTheme = !darkTheme;
        PREFS.putBoolean("darkTheme", darkTheme); // salva preferência

        if (rootContainer != null) {
            if (darkTheme) {
                rootContainer.getStyleClass().add("dark-theme");
            } else {
                rootContainer.getStyleClass().remove("dark-theme");
                rootContainer.setStyle("");
            }
        }

        if (imgTema != null) { // Altera entre img do sol e da lua
            String path = darkTheme ? "/app/icons/tema/claro.png" : "/app/icons/tema/escuro.png";
            javafx.scene.image.Image icone = IconManager.getImage(path);
            if (icone != null) imgTema.setImage(icone);
        }

        if (lblTema != null) {
            lblTema.setText(darkTheme ? "Tema claro" : "Tema escuro");
        }
    }

    private Window obterJanelaPrincipal() {
        return rootContainer != null && rootContainer.getScene() != null
                ? rootContainer.getScene().getWindow()
                : null;
    }

    // Janela aberta a partir de botão "Nota Transação"
    private void abrirDialogNovaTransacao() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/view/transacao_dialog.fxml")
            );

            VBox dialogRoot = loader.load();
            TransacaoDialogController controller = loader.getController();
            controller.setCarteiraService(carteiraService);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Nova Transação");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            Window owner = obterJanelaPrincipal();
            if (owner != null) {
                dialogStage.initOwner(owner);
            }

            Scene scene = new Scene(dialogRoot);
            CssManager.aplicarCss(scene);

            if (darkTheme) {
                dialogRoot.getStyleClass().add("dark-theme");
            }

            dialogStage.setScene(scene);
            dialogStage.setResizable(true);
            dialogStage.setMinWidth(520);
            dialogStage.setMinHeight(700);
            dialogStage.setMaxWidth(600);
            IconManager.setTransacaoIcon(dialogStage);

            dialogStage.showAndWait();

            if (controller.isConfirmado()) {
                carregarDados();
            }

        } catch (Exception e) {
            logger.error("Erro ao abrir dialog de transação: {}", e.getMessage());
        }
    }

    // Janela duplo clique em uma transação
    private void abrirDetalhesTransacao(Transacao transacao) {
        Window owner = listTransacoes != null && listTransacoes.getScene() != null
                ? listTransacoes.getScene().getWindow()
                : obterJanelaPrincipal();
        abrirDetalhesTransacao(transacao, owner);
    }

    private void abrirDetalhesTransacao(Transacao transacao, Window owner) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/view/transacao_dialog.fxml")
            );

            VBox dialogRoot = loader.load();
            TransacaoDialogController controller = loader.getController();

            controller.setCarteiraService(carteiraService);
            controller.configurarParaEditar(transacao);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Detalhes da Transação");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            if (owner != null) {
                dialogStage.initOwner(owner);
            }

            Scene scene = new Scene(dialogRoot);
            CssManager.aplicarCss(scene);

            if (darkTheme) {
                dialogRoot.getStyleClass().add("dark-theme");
            }

            dialogStage.setScene(scene);
            dialogStage.setResizable(true);
            dialogStage.setMinWidth(520);
            dialogStage.setMinHeight(700);
            dialogStage.setMaxWidth(600);
            IconManager.setDetalheIcon(dialogStage);

            dialogStage.showAndWait();

            if (controller.isConfirmado()) {
                carregarDados();
            }

        } catch (Exception e) {
            logger.error("Erro ao abrir detalhes da transação: {}", e.getMessage());
        }
    }

    // Janela aberta a partir de botão "Nota Meta"
    private void abrirDialogNovaMeta() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/view/meta_dialog.fxml")
            );

            VBox dialogRoot = loader.load();
            MetaDialogController controller = loader.getController();

            controller.configurarParaCriar();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Nova Meta");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            Window owner = obterJanelaPrincipal();
            if (owner != null) {
                dialogStage.initOwner(owner);
            }

            Scene scene = new Scene(dialogRoot);
            CssManager.aplicarCss(scene);

            if (darkTheme) {
                dialogRoot.getStyleClass().add("dark-theme");
            }
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            dialogStage.setMinWidth(480);
            dialogStage.setMinHeight(380);
            IconManager.setMetaIcon(dialogStage);

            dialogStage.showAndWait();

            if (controller.isConfirmado()) {
                carregarDados();
            }

        } catch (Exception e) {
            logger.error("Erro ao abrir dialog de meta: {}", e.getMessage());
        }
    }

    // Janela duplo clique em uma meta
    private void abrirDetalhesMeta(Meta meta) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/view/meta_dialog.fxml")
            );

            VBox dialogRoot = loader.load();
            MetaDialogController controller = loader.getController();

            controller.configurarParaEditar(meta);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Editar Meta");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            Window owner = listMetas != null && listMetas.getScene() != null
                    ? listMetas.getScene().getWindow()
                    : obterJanelaPrincipal();
            if (owner != null) {
                dialogStage.initOwner(owner);
            }

            Scene scene = new Scene(dialogRoot);
            CssManager.aplicarCss(scene);

            if (darkTheme) {
                dialogRoot.getStyleClass().add("dark-theme");
            }

            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            dialogStage.setMinWidth(480);
            dialogStage.setMinHeight(380);
            IconManager.setMetaIcon(dialogStage);

            dialogStage.showAndWait();

            if (controller.isConfirmado()) {
                carregarDados();
            }

        } catch (Exception e) {
            logger.error("Erro ao abrir detalhes da meta: {}", e.getMessage());
        }
    }

    private void abrirDialogCarteiras() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/view/carteiras_dialog.fxml")
            );

            VBox dialogRoot = loader.load();
            CarteirasDialogController controller = loader.getController();
            controller.setCarteiraService(carteiraService);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Carteiras");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            Window owner = obterJanelaPrincipal();
            if (owner != null) {
                dialogStage.initOwner(owner);
            }

            Scene scene = new Scene(dialogRoot);
            CssManager.aplicarCss(scene);
            if (darkTheme) {
                dialogRoot.getStyleClass().add("dark-theme");
            }

            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            dialogStage.setMinWidth(480);
            IconManager.setAppIcon(dialogStage);
            dialogStage.showAndWait();

            if (controller.isConfirmado()) {
                carregarDados();
            }
        } catch (Exception e) {
            logger.error("Erro ao abrir dialog de carteiras: {}", e.getMessage());
        }
    }

    private void abrirDialogTransferenciaCarteira() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/view/transferencia_carteira_dialog.fxml")
            );

            VBox dialogRoot = loader.load();
            TransferenciaCarteiraDialogController controller = loader.getController();
            controller.setCarteiraService(carteiraService);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Transferir entre carteiras");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            Window owner = obterJanelaPrincipal();
            if (owner != null) {
                dialogStage.initOwner(owner);
            }

            Scene scene = new Scene(dialogRoot);
            CssManager.aplicarCss(scene);
            if (darkTheme) {
                dialogRoot.getStyleClass().add("dark-theme");
            }

            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            dialogStage.setMinWidth(500);
            IconManager.setAppIcon(dialogStage);
            dialogStage.showAndWait();

            if (controller.isConfirmado()) {
                carregarDados();
            }
        } catch (Exception e) {
            logger.error("Erro ao abrir transferência de carteira: {}", e.getMessage());
        }
    }
}
