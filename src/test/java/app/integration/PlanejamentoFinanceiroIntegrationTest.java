package app.integration;

import app.database.Database;
import app.model.CompraCartaoCredito;
import app.model.Emprestimo;
import app.model.TipoEmprestimo;
import app.repository.CartaoCreditoDAO;
import app.repository.EmprestimoDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integração - planejamento financeiro")
class PlanejamentoFinanceiroIntegrationTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearDbOverride() {
        Database.clearOverrideUrlForTests();
    }

    @Test
    @DisplayName("bootstrap cria tabelas e indices de cartao e emprestimos")
    void bootstrapCriaTabelasEIndices() throws Exception {
        Path dbFile = tempDir.resolve("planejamento-schema.db");
        Database.overrideUrlForTests(jdbcUrl(dbFile));
        Database.inicializar();

        try (Connection conn = DriverManager.getConnection(jdbcUrl(dbFile))) {
            assertAll(
                    () -> assertTrue(objectExists(conn, "compra_cartao_credito")),
                    () -> assertTrue(objectExists(conn, "emprestimo")),
                    () -> assertTrue(indexNamesOf(conn, "compra_cartao_credito")
                            .contains("idx_compra_cartao_vencimento")),
                    () -> assertTrue(indexNamesOf(conn, "emprestimo")
                            .contains("idx_emprestimo_tipo_data"))
            );
        }
    }

    @Test
    @DisplayName("DAO de cartao persiste, atualiza, lista e deleta compras")
    void cartaoDaoPersisteFluxoCompleto() {
        Database.overrideUrlForTests(jdbcUrl(tempDir.resolve("cartao.db")));
        Database.inicializar();

        CartaoCreditoDAO dao = new CartaoCreditoDAO();
        UUID id = UUID.randomUUID();
        dao.inserir(new CompraCartaoCredito(
                id,
                "Notebook",
                2400_00L,
                12,
                LocalDate.of(2026, 6, 10)
        ));

        dao.atualizar(new CompraCartaoCredito(
                id,
                "Notebook gamer",
                3000_00L,
                10,
                LocalDate.of(2026, 6, 12)
        ));

        CompraCartaoCredito compra = dao.buscarPorId(id);
        assertAll(
                () -> assertEquals(1, dao.listarTodas().size()),
                () -> assertEquals("Notebook gamer", compra.nome()),
                () -> assertEquals(3000_00L, compra.valorTotalCentavos()),
                () -> assertEquals(300_00L, compra.valorParcelaCentavos())
        );

        dao.deletar(id);
        assertNull(dao.buscarPorId(id));
    }

    @Test
    @DisplayName("DAO de emprestimos mantem anotacoes sem criar transacoes")
    void emprestimoDaoPersisteAnotacoes() {
        Database.overrideUrlForTests(jdbcUrl(tempDir.resolve("emprestimos.db")));
        Database.inicializar();

        EmprestimoDAO dao = new EmprestimoDAO();
        UUID id = UUID.randomUUID();
        dao.inserir(new Emprestimo(
                id,
                TipoEmprestimo.Emprestei,
                "Ana",
                150_00L,
                LocalDate.of(2026, 6, 20)
        ));

        dao.atualizar(new Emprestimo(
                id,
                TipoEmprestimo.PediEmprestado,
                "Bruno",
                200_00L,
                LocalDate.of(2026, 6, 25)
        ));

        assertAll(
                () -> assertTrue(dao.listarPorTipo(TipoEmprestimo.Emprestei).isEmpty()),
                () -> assertEquals(1, dao.listarPorTipo(TipoEmprestimo.PediEmprestado).size()),
                () -> assertEquals("Bruno", dao.listarTodas().getFirst().nome())
        );

        dao.deletar(id);
        assertTrue(dao.listarTodas().isEmpty());
    }

    private static String jdbcUrl(Path path) {
        return "jdbc:sqlite:" + path.toAbsolutePath();
    }

    private static boolean objectExists(Connection conn, String name) throws Exception {
        try (var ps = conn.prepareStatement("""
                SELECT 1
                FROM sqlite_master
                WHERE name = ?
                LIMIT 1
                """)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static Set<String> indexNamesOf(Connection conn, String tableName) throws Exception {
        Set<String> indexes = new java.util.HashSet<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA index_list(%s)".formatted(tableName))) {
            while (rs.next()) {
                indexes.add(rs.getString("name"));
            }
        }
        return indexes;
    }
}
