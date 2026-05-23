package app.repository;

import app.database.Database;
import app.model.CompraCartaoCredito;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

public class CartaoCreditoDAO {

    protected Connection getConn() throws SQLException {
        return Database.getConnection();
    }

    public void inserir(CompraCartaoCredito compra) {
        String sql = """
                INSERT INTO compra_cartao_credito (id, nome, valor_total_centavos, parcelas, vencimento)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            preencherStatement(ps, compra);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir compra de cartao", e);
        }
    }

    public void atualizar(CompraCartaoCredito compra) {
        String sql = """
                UPDATE compra_cartao_credito
                SET nome = ?, valor_total_centavos = ?, parcelas = ?, vencimento = ?
                WHERE id = ?
                """;

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, compra.nome());
            ps.setLong(2, compra.valorTotalCentavos());
            ps.setInt(3, compra.parcelas());
            ps.setString(4, compra.vencimento().toString());
            ps.setString(5, compra.id().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar compra de cartao", e);
        }
    }

    public void deletar(UUID id) {
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement("DELETE FROM compra_cartao_credito WHERE id = ?")) {
            ps.setString(1, id.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao deletar compra de cartao", e);
        }
    }

    public CompraCartaoCredito buscarPorId(UUID id) {
        String sql = "SELECT * FROM compra_cartao_credito WHERE id = ?";

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar compra de cartao", e);
        }
    }

    public List<CompraCartaoCredito> listarTodas() {
        String sql = """
                SELECT *
                FROM compra_cartao_credito
                ORDER BY vencimento ASC, rowid DESC
                """;

        try (Connection c = getConn();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<CompraCartaoCredito> compras = new ArrayList<>();
            while (rs.next()) {
                compras.add(mapear(rs));
            }
            return compras;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar compras de cartao", e);
        }
    }

    private void preencherStatement(PreparedStatement ps, CompraCartaoCredito compra) throws SQLException {
        ps.setString(1, compra.id().toString());
        ps.setString(2, compra.nome());
        ps.setLong(3, compra.valorTotalCentavos());
        ps.setInt(4, compra.parcelas());
        ps.setString(5, compra.vencimento().toString());
    }

    private CompraCartaoCredito mapear(ResultSet rs) throws SQLException {
        return new CompraCartaoCredito(
                UUID.fromString(rs.getString("id")),
                rs.getString("nome"),
                rs.getLong("valor_total_centavos"),
                rs.getInt("parcelas"),
                LocalDate.parse(rs.getString("vencimento"))
        );
    }
}
