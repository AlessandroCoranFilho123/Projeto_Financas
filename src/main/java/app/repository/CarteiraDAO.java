package app.repository;

import app.database.Database;
import app.model.Carteira;
import app.model.TransferenciaCarteira;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CarteiraDAO {

    public static final UUID BANCO_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID DINHEIRO_FISICO_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final String BANCO_NOME = "Banco";
    public static final String DINHEIRO_FISICO_NOME = "Dinheiro físico";

    private volatile boolean estruturaGarantida = false;

    protected Connection getConn() throws SQLException {
        return Database.getConnection();
    }

    public void garantirEstrutura() {
        if (estruturaGarantida) return;
        synchronized (this) {
            if (estruturaGarantida) return;
            try (Connection c = getConn();
                 Statement stmt = c.createStatement()) {
                criarEstrutura(stmt);
                estruturaGarantida = true;
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao preparar carteiras", e);
            }
        }
    }

    public static void criarEstrutura(Statement stmt) throws SQLException {
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS carteira (
                    id                       TEXT PRIMARY KEY,
                    nome                     TEXT    NOT NULL UNIQUE,
                    saldo_inicial_centavos   INTEGER NOT NULL DEFAULT 0
                )
                """);

        stmt.execute("""
                INSERT OR IGNORE INTO carteira (id, nome, saldo_inicial_centavos)
                VALUES
                    ('00000000-0000-0000-0000-000000000001', 'Banco', 0),
                    ('00000000-0000-0000-0000-000000000002', 'Dinheiro físico', 0)
                """);

        stmt.execute("""
                CREATE TABLE IF NOT EXISTS transferencia_carteira (
                    id                  TEXT PRIMARY KEY,
                    origem_carteira_id  TEXT    NOT NULL,
                    destino_carteira_id TEXT    NOT NULL,
                    valor_centavos      INTEGER NOT NULL,
                    data                TEXT    NOT NULL,
                    comentario          TEXT    NOT NULL DEFAULT '',
                    FOREIGN KEY (origem_carteira_id) REFERENCES carteira (id) ON DELETE CASCADE,
                    FOREIGN KEY (destino_carteira_id) REFERENCES carteira (id) ON DELETE CASCADE
                )
                """);

        stmt.execute("CREATE INDEX IF NOT EXISTS idx_carteira_nome ON carteira (nome COLLATE NOCASE)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_transferencia_origem ON transferencia_carteira (origem_carteira_id, data)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_transferencia_destino ON transferencia_carteira (destino_carteira_id, data)");
    }

    public List<Carteira> listarTodas() {
        garantirEstrutura();
        String sql = """
                SELECT id, nome, saldo_inicial_centavos
                FROM carteira
                ORDER BY
                    CASE id
                        WHEN '00000000-0000-0000-0000-000000000001' THEN 0
                        WHEN '00000000-0000-0000-0000-000000000002' THEN 1
                        ELSE 2
                    END,
                    nome COLLATE NOCASE
                """;

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Carteira> carteiras = new ArrayList<>();
            while (rs.next()) {
                carteiras.add(mapear(rs));
            }
            return carteiras;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar carteiras", e);
        }
    }

    public Carteira buscarPorId(UUID id) {
        if (id == null) return null;
        garantirEstrutura();
        String sql = """
                SELECT id, nome, saldo_inicial_centavos
                FROM carteira
                WHERE id = ?
                """;

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar carteira", e);
        }
    }

    public Carteira buscarBanco() {
        Carteira banco = buscarPorId(BANCO_ID);
        if (banco != null) {
            return banco;
        }
        garantirEstrutura();
        return buscarPorId(BANCO_ID);
    }

    public void atualizarSaldoInicial(UUID id, long saldoInicialCentavos) {
        if (id == null) {
            throw new IllegalArgumentException("Carteira é obrigatória");
        }
        garantirEstrutura();
        String sql = "UPDATE carteira SET saldo_inicial_centavos = ? WHERE id = ?";

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, saldoInicialCentavos);
            ps.setString(2, id.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar saldo inicial", e);
        }
    }

    public void inserirTransferencia(TransferenciaCarteira transferencia) {
        garantirEstrutura();
        String sql = """
                INSERT INTO transferencia_carteira (
                    id, origem_carteira_id, destino_carteira_id, valor_centavos, data, comentario
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, transferencia.id().toString());
            ps.setString(2, transferencia.origemCarteiraId().toString());
            ps.setString(3, transferencia.destinoCarteiraId().toString());
            ps.setLong(4, transferencia.valorCentavos());
            ps.setString(5, transferencia.data().toString());
            ps.setString(6, transferencia.comentario());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao registrar transferência entre carteiras", e);
        }
    }

    public long calcularTransferenciasEntrada(UUID carteiraId) {
        return somarTransferencias(carteiraId, "destino_carteira_id");
    }

    public long calcularTransferenciasSaida(UUID carteiraId) {
        return somarTransferencias(carteiraId, "origem_carteira_id");
    }

    private long somarTransferencias(UUID carteiraId, String coluna) {
        if (carteiraId == null) return 0;
        garantirEstrutura();
        String sql = "SELECT SUM(valor_centavos) FROM transferencia_carteira WHERE " + coluna + " = ?";

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, carteiraId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getObject(1) != null ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao calcular transferências", e);
        }
    }

    private Carteira mapear(ResultSet rs) throws SQLException {
        return new Carteira(
                UUID.fromString(rs.getString("id")),
                rs.getString("nome"),
                rs.getLong("saldo_inicial_centavos")
        );
    }
}
