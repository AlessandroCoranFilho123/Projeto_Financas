package app.repository;

import app.database.Database;
import app.model.Emprestimo;
import app.model.TipoEmprestimo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EmprestimoDAO {

    protected Connection getConn() throws SQLException {
        return Database.getConnection();
    }

    public void inserir(Emprestimo emprestimo) {
        String sql = """
                INSERT INTO emprestimo (id, tipo, nome, valor_centavos, data_pagamento)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            preencherStatement(ps, emprestimo);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir emprestimo", e);
        }
    }

    public void atualizar(Emprestimo emprestimo) {
        String sql = """
                UPDATE emprestimo
                SET tipo = ?, nome = ?, valor_centavos = ?, data_pagamento = ?
                WHERE id = ?
                """;

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, emprestimo.tipo().name());
            ps.setString(2, emprestimo.nome());
            ps.setLong(3, emprestimo.valorCentavos());
            ps.setString(4, emprestimo.dataPagamento().toString());
            ps.setString(5, emprestimo.id().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar emprestimo", e);
        }
    }

    public void deletar(UUID id) {
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement("DELETE FROM emprestimo WHERE id = ?")) {
            ps.setString(1, id.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao deletar emprestimo", e);
        }
    }

    public List<Emprestimo> listarTodas() {
        String sql = """
                SELECT *
                FROM emprestimo
                ORDER BY data_pagamento ASC, rowid DESC
                """;

        try (Connection c = getConn();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<Emprestimo> emprestimos = new ArrayList<>();
            while (rs.next()) {
                emprestimos.add(mapear(rs));
            }
            return emprestimos;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar emprestimos", e);
        }
    }

    public List<Emprestimo> listarPorTipo(TipoEmprestimo tipo) {
        String sql = """
                SELECT *
                FROM emprestimo
                WHERE tipo = ?
                ORDER BY data_pagamento ASC, rowid DESC
                """;

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tipo.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<Emprestimo> emprestimos = new ArrayList<>();
                while (rs.next()) {
                    emprestimos.add(mapear(rs));
                }
                return emprestimos;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar emprestimos por tipo", e);
        }
    }

    private void preencherStatement(PreparedStatement ps, Emprestimo emprestimo) throws SQLException {
        ps.setString(1, emprestimo.id().toString());
        ps.setString(2, emprestimo.tipo().name());
        ps.setString(3, emprestimo.nome());
        ps.setLong(4, emprestimo.valorCentavos());
        ps.setString(5, emprestimo.dataPagamento().toString());
    }

    private Emprestimo mapear(ResultSet rs) throws SQLException {
        return new Emprestimo(
                UUID.fromString(rs.getString("id")),
                TipoEmprestimo.valueOf(rs.getString("tipo")),
                rs.getString("nome"),
                rs.getLong("valor_centavos"),
                LocalDate.parse(rs.getString("data_pagamento"))
        );
    }
}
