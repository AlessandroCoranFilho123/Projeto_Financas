package app.service;

import app.database.Database;
import app.model.Categoria;
import app.model.TipoTransacao;
import app.repository.CarteiraDAO;
import app.repository.MetaDAO;
import app.repository.TransacaoDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CarteiraService - integração")
class CarteiraServiceIntegrationTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearDbOverride() {
        Database.clearOverrideUrlForTests();
    }

    @Test
    @DisplayName("saldo inicial compõe saldo disponível sem entrar como receita")
    void saldoInicialNaoEntraComoReceita() {
        prepararBanco("saldo-inicial.db");
        CarteiraService carteiraService = new CarteiraService();
        TransacaoService transacaoService = new TransacaoService(
                new TransacaoDAO(),
                new MetaDAO(),
                carteiraService
        );

        carteiraService.atualizarSaldoInicial(CarteiraDAO.BANCO_ID, 1500_00L);

        assertAll(
                () -> assertEquals(1500_00L, transacaoService.calcularSaldoDisponivelCentavos()),
                () -> assertEquals(0L, transacaoService.calcularReceitasMes(YearMonth.now()))
        );
    }

    @Test
    @DisplayName("transação afeta somente a carteira escolhida")
    void transacaoAfetaCarteiraEscolhida() {
        prepararBanco("transacao-carteira.db");
        CarteiraService carteiraService = new CarteiraService();
        TransacaoService transacaoService = new TransacaoService(
                new TransacaoDAO(),
                new MetaDAO(),
                carteiraService
        );

        carteiraService.atualizarSaldoInicial(CarteiraDAO.BANCO_ID, 1000_00L);
        carteiraService.atualizarSaldoInicial(CarteiraDAO.DINHEIRO_FISICO_ID, 300_00L);
        transacaoService.registrar(
                TipoTransacao.Saida,
                Categoria.Alimentacao,
                50_00L,
                null,
                "padaria",
                LocalDate.now(),
                CarteiraDAO.DINHEIRO_FISICO_ID
        );

        assertAll(
                () -> assertEquals(1000_00L, carteiraService.calcularSaldoCarteiraCentavos(CarteiraDAO.BANCO_ID)),
                () -> assertEquals(250_00L, carteiraService.calcularSaldoCarteiraCentavos(CarteiraDAO.DINHEIRO_FISICO_ID)),
                () -> assertEquals(1250_00L, carteiraService.calcularSaldoTotalCentavos())
        );
    }

    @Test
    @DisplayName("transferência muda carteiras sem criar transação")
    void transferenciaNaoCriaTransacao() {
        prepararBanco("transferencia.db");
        CarteiraService carteiraService = new CarteiraService();
        TransacaoDAO transacaoDAO = new TransacaoDAO();

        carteiraService.atualizarSaldoInicial(CarteiraDAO.BANCO_ID, 1000_00L);
        carteiraService.transferir(
                CarteiraDAO.BANCO_ID,
                CarteiraDAO.DINHEIRO_FISICO_ID,
                200_00L,
                LocalDate.now(),
                "saque"
        );

        assertAll(
                () -> assertEquals(800_00L, carteiraService.calcularSaldoCarteiraCentavos(CarteiraDAO.BANCO_ID)),
                () -> assertEquals(200_00L, carteiraService.calcularSaldoCarteiraCentavos(CarteiraDAO.DINHEIRO_FISICO_ID)),
                () -> assertEquals(1000_00L, carteiraService.calcularSaldoTotalCentavos()),
                () -> assertTrue(transacaoDAO.listarTodas().isEmpty())
        );
    }

    private void prepararBanco(String nomeArquivo) {
        Database.overrideUrlForTests("jdbc:sqlite:" + tempDir.resolve(nomeArquivo).toAbsolutePath());
        Database.inicializar();
    }
}
