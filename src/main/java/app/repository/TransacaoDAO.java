package app.repository;

import app.database.Database;
import app.model.Categoria;
import app.model.TipoTransacao;
import app.model.Transacao;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TransacaoDAO {

    private volatile boolean migrationDone = false;
    private volatile boolean searchInfrastructureDone = false;

    protected Connection getConn() throws SQLException {
        return Database.getConnection();
    }

    // Garante que bancos antigos tenham as colunas incrementais usadas pelo app.
    protected void garantirSchemaTransacao() {
        if (migrationDone) return;
        synchronized (this) {
            if (migrationDone) return;
            try (Connection c = getConn();
                 Statement stmt = c.createStatement()) {
                executarMigrationSegura(stmt, "ALTER TABLE transacao ADD COLUMN comentario TEXT NOT NULL DEFAULT ''");
                executarMigrationSegura(stmt, "ALTER TABLE transacao ADD COLUMN categoria TEXT");
                executarMigrationSegura(stmt, "ALTER TABLE transacao ADD COLUMN carteira_id TEXT");
                stmt.execute("""
                        UPDATE transacao
                        SET carteira_id = '00000000-0000-0000-0000-000000000001'
                        WHERE carteira_id IS NULL
                        """);
                try {
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_transacao_carteira_id ON transacao (carteira_id)");
                } catch (SQLException ignored) {
                    // Índice opcional para schemas de teste muito reduzidos.
                }
                migrationDone = true;
            } catch (SQLException e) {
                throw new RuntimeException("Erro na migration de transacao", e);
            }
        }
    }

    protected void garantirColunaComentario() {
        garantirSchemaTransacao();
    }

    public void inserir(Transacao transacao) {
        String sql = """
                INSERT INTO transacao (
                    id, descricao, comentario, valor_centavos, tipo, data, meta_id, categoria, carteira_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        garantirSchemaTransacao();
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, transacao.id().toString());
            ps.setString(2, transacao.descricao());
            ps.setString(3, transacao.comentario() != null ? transacao.comentario() : "");
            ps.setLong(4, transacao.valorCentavos());
            ps.setString(5, transacao.tipo().name());
            ps.setString(6, transacao.data().toString());

            if (transacao.metaId() != null) {
                ps.setString(7, transacao.metaId().toString());
            } else {
                ps.setNull(7, Types.VARCHAR);
            }

            if (transacao.categoria() != null) {
                ps.setString(8, transacao.categoria().name());
            } else {
                ps.setNull(8, Types.VARCHAR);
            }

            UUID carteiraId = transacao.carteiraId() != null
                    ? transacao.carteiraId()
                    : CarteiraDAO.BANCO_ID;
            ps.setString(9, carteiraId.toString());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir transacao", e);
        }
    }

    public List<Transacao> listarRecentes(int limite) {
        garantirSchemaTransacao();
        List<Transacao> list = new ArrayList<>();

        String sql = """
                SELECT * FROM transacao
                ORDER BY data DESC, rowid DESC
                LIMIT ?
                """;

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, limite);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                UUID metaId = rs.getString("meta_id") == null
                        ? null
                        : UUID.fromString(rs.getString("meta_id"));

                String catStr = rs.getString("categoria");
                Categoria categoria = catStr != null ? Categoria.valueOf(catStr) : null;

                list.add(new Transacao(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("descricao"),
                        rs.getString("comentario") != null ? rs.getString("comentario") : "",
                        rs.getLong("valor_centavos"),
                        TipoTransacao.valueOf(rs.getString("tipo")),
                        LocalDate.parse(rs.getString("data")),
                        metaId,
                        categoria,
                        lerCarteiraId(rs)
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar transacoes", e);
        }

        return list;
    }

    public List<Transacao> listarPorFiltro(TransacaoFiltro filtro) {
        garantirInfraestruturaBusca();
        TransacaoFiltro filtroSeguro = filtro != null
                ? filtro
                : new TransacaoFiltro("", null, null, null, null);
        String consultaFts = filtroSeguro.possuiBusca() ? criarConsultaFts(filtroSeguro.busca()) : "";
        boolean filtrarTexto = !consultaFts.isBlank();

        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append(filtrarTexto
                ? "SELECT t.* FROM transacao t JOIN transacao_fts ON transacao_fts.rowid = t.rowid"
                : "SELECT t.* FROM transacao t");

        List<String> where = new ArrayList<>();
        if (filtrarTexto) {
            where.add("transacao_fts MATCH ?");
            params.add(consultaFts);
        }
        if (filtroSeguro.tipo() != null) {
            where.add("t.tipo = ?");
            params.add(filtroSeguro.tipo().name());
        }
        if (filtroSeguro.categoria() != null) {
            where.add("t.categoria = ?");
            params.add(filtroSeguro.categoria().name());
        }
        if (filtroSeguro.dataInicio() != null) {
            where.add("t.data >= ?");
            params.add(filtroSeguro.dataInicio().toString());
        }
        if (filtroSeguro.dataFim() != null) {
            where.add("t.data <= ?");
            params.add(filtroSeguro.dataFim().toString());
        }

        if (!where.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", where));
        }
        sql.append(" ORDER BY t.data DESC, t.rowid DESC");

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                return mapearLista(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar transacoes filtradas", e);
        }
    }

    public List<Transacao> listarTodas() {
        garantirSchemaTransacao();
        List<Transacao> list = new ArrayList<>();

        String sql = """
                SELECT * FROM transacao
                ORDER BY data DESC, rowid DESC
                """;

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                UUID metaId = rs.getString("meta_id") == null
                        ? null
                        : UUID.fromString(rs.getString("meta_id"));

                String catStr = rs.getString("categoria");
                Categoria categoria = catStr != null ? Categoria.valueOf(catStr) : null;

                list.add(new Transacao(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("descricao"),
                        rs.getString("comentario") != null ? rs.getString("comentario") : "",
                        rs.getLong("valor_centavos"),
                        TipoTransacao.valueOf(rs.getString("tipo")),
                        LocalDate.parse(rs.getString("data")),
                        metaId,
                        categoria,
                        lerCarteiraId(rs)
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar todas as transacoes", e);
        }

        return list;
    }

    public long calcularSaldo() {
        garantirSchemaTransacao();
        String sql = """
                SELECT SUM(
                    CASE tipo
                        WHEN 'Entrada' THEN valor_centavos
                        ELSE -valor_centavos
                    END
                ) AS saldo
                FROM transacao
                """;

        try (Connection c = getConn();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            return rs.next() ? rs.getLong("saldo") : 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao calcular saldo", e);
        }
    }

    public Transacao buscarPorId(UUID id) {
        garantirSchemaTransacao();
        String sql = "SELECT * FROM transacao WHERE id = ?";

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, id.toString());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;

            UUID metaId = rs.getString("meta_id") == null
                    ? null
                    : UUID.fromString(rs.getString("meta_id"));

            String catStr = rs.getString("categoria");
            Categoria categoria = catStr != null ? Categoria.valueOf(catStr) : null;

            return new Transacao(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("descricao"),
                    rs.getString("comentario") != null ? rs.getString("comentario") : "",
                    rs.getLong("valor_centavos"),
                    TipoTransacao.valueOf(rs.getString("tipo")),
                    LocalDate.parse(rs.getString("data")),
                    metaId,
                    categoria,
                    lerCarteiraId(rs)
            );

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar transacao", e);
        }
    }

    public long calcularTotalPorTipoEMes(TipoTransacao tipo, String anoMes) {
        garantirSchemaTransacao();
        YearMonth mes;
        try {
            mes = YearMonth.parse(anoMes);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Mes deve estar no formato yyyy-MM", e);
        }

        String sql = """
                SELECT SUM(valor_centavos)
                FROM transacao
                WHERE tipo = ? AND data >= ? AND data < ?
                """;

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, tipo.name());
            ps.setString(2, mes.atDay(1).toString());
            ps.setString(3, mes.plusMonths(1).atDay(1).toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject(1) != null ? rs.getLong(1) : 0L;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao calcular total por tipo e mes", e);
        }

        return 0L;
    }

    public void excluir(UUID id) {
        garantirSchemaTransacao();
        try (Connection c = getConn();
             PreparedStatement ps =
                     c.prepareStatement("DELETE FROM transacao WHERE id = ?")) {

            ps.setString(1, id.toString());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir transacao", e);
        }
    }

    private void garantirInfraestruturaBusca() {
        garantirSchemaTransacao();
        if (searchInfrastructureDone) return;
        synchronized (this) {
            if (searchInfrastructureDone) return;
            try (Connection c = getConn();
                 Statement stmt = c.createStatement()) {
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_transacao_data_desc ON transacao (data DESC)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_transacao_tipo_data ON transacao (tipo, data)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_transacao_categoria_data ON transacao (categoria, data)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_transacao_meta_id ON transacao (meta_id)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_transacao_carteira_id ON transacao (carteira_id)");
                criarBuscaTextual(stmt);
                searchInfrastructureDone = true;
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao preparar busca otimizada", e);
            }
        }
    }

    private void criarBuscaTextual(Statement stmt) throws SQLException {
        boolean indiceCriadoAgora = !objetoExiste(stmt, "transacao_fts");

        stmt.execute("""
                CREATE VIRTUAL TABLE IF NOT EXISTS transacao_fts USING fts5(
                    descricao,
                    comentario,
                    categoria,
                    tipo,
                    data,
                    content='transacao',
                    content_rowid='rowid',
                    tokenize='unicode61 remove_diacritics 2'
                )
                """);

        stmt.execute("""
                CREATE TRIGGER IF NOT EXISTS transacao_ai AFTER INSERT ON transacao BEGIN
                    INSERT INTO transacao_fts(rowid, descricao, comentario, categoria, tipo, data)
                    VALUES (new.rowid, new.descricao, new.comentario, COALESCE(new.categoria, ''), new.tipo, new.data);
                END
                """);

        stmt.execute("""
                CREATE TRIGGER IF NOT EXISTS transacao_ad AFTER DELETE ON transacao BEGIN
                    INSERT INTO transacao_fts(transacao_fts, rowid, descricao, comentario, categoria, tipo, data)
                    VALUES ('delete', old.rowid, old.descricao, old.comentario, COALESCE(old.categoria, ''), old.tipo, old.data);
                END
                """);

        stmt.execute("""
                CREATE TRIGGER IF NOT EXISTS transacao_au AFTER UPDATE ON transacao BEGIN
                    INSERT INTO transacao_fts(transacao_fts, rowid, descricao, comentario, categoria, tipo, data)
                    VALUES ('delete', old.rowid, old.descricao, old.comentario, COALESCE(old.categoria, ''), old.tipo, old.data);
                    INSERT INTO transacao_fts(rowid, descricao, comentario, categoria, tipo, data)
                    VALUES (new.rowid, new.descricao, new.comentario, COALESCE(new.categoria, ''), new.tipo, new.data);
                END
                """);

        if (indiceCriadoAgora) {
            stmt.execute("INSERT INTO transacao_fts(transacao_fts) VALUES ('rebuild')");
        }
    }

    private boolean objetoExiste(Statement stmt, String nome) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("""
                SELECT 1
                FROM sqlite_master
                WHERE name = '%s'
                LIMIT 1
                """.formatted(nome))) {
            return rs.next();
        }
    }

    private String criarConsultaFts(String busca) {
        String semAcentos = Normalizer.normalize(busca, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);

        String[] termos = semAcentos.split("[^\\p{L}\\p{N}]+");
        List<String> partes = new ArrayList<>();
        for (String termo : termos) {
            if (!termo.isBlank()) {
                partes.add(termo + "*");
            }
        }
        return String.join(" AND ", partes);
    }

    private List<Transacao> mapearLista(ResultSet rs) throws SQLException {
        List<Transacao> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapearTransacao(rs));
        }
        return list;
    }

    private Transacao mapearTransacao(ResultSet rs) throws SQLException {
        UUID metaId = rs.getString("meta_id") == null
                ? null
                : UUID.fromString(rs.getString("meta_id"));

        String catStr = rs.getString("categoria");
        Categoria categoria = catStr != null ? Categoria.valueOf(catStr) : null;

        return new Transacao(
                UUID.fromString(rs.getString("id")),
                rs.getString("descricao"),
                rs.getString("comentario") != null ? rs.getString("comentario") : "",
                rs.getLong("valor_centavos"),
                TipoTransacao.valueOf(rs.getString("tipo")),
                LocalDate.parse(rs.getString("data")),
                metaId,
                categoria,
                lerCarteiraId(rs)
        );
    }

    public long calcularSaldoPorCarteira(UUID carteiraId) {
        garantirSchemaTransacao();
        UUID carteiraEfetiva = carteiraId != null ? carteiraId : CarteiraDAO.BANCO_ID;
        String sql = """
                SELECT SUM(
                    CASE tipo
                        WHEN 'Entrada' THEN valor_centavos
                        ELSE -valor_centavos
                    END
                ) AS saldo
                FROM transacao
                WHERE COALESCE(carteira_id, ?) = ?
                """;

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, CarteiraDAO.BANCO_ID.toString());
            ps.setString(2, carteiraEfetiva.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("saldo") : 0L;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao calcular saldo por carteira", e);
        }
    }

    private void executarMigrationSegura(Statement stmt, String sql) throws SQLException {
        try {
            stmt.execute(sql);
        } catch (SQLException ignored) {
            // Coluna já existe.
        }
    }

    private UUID lerCarteiraId(ResultSet rs) throws SQLException {
        String carteira = rs.getString("carteira_id");
        if (carteira == null || CarteiraDAO.BANCO_ID.toString().equals(carteira)) {
            return null;
        }
        return UUID.fromString(carteira);
    }
}
