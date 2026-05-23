package app.database;

import app.repository.CarteiraDAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    // Em produção (instalado): %APPDATA%\SmartFinance\financas.db
    // Em desenvolvimento (IDE): ./financas.db (fallback)
    private static final String DEFAULT_URL;
    private static volatile String testOverrideUrl;

    static {
        DEFAULT_URL = resolveDefaultUrl();
    }

    private Database() {
    }

    public static void inicializar() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Tabela meta
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS meta (
                        id             TEXT PRIMARY KEY,
                        nome           TEXT    NOT NULL,
                        alvo_centavos  INTEGER,
                        atual_centavos INTEGER NOT NULL DEFAULT 0
                    )
                    """);

            CarteiraDAO.criarEstrutura(stmt);

            // Tabela transacao - schema completo com comentario, categoria e carteira
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS transacao (
                        id             TEXT PRIMARY KEY,
                        descricao      TEXT    NOT NULL,
                        comentario     TEXT    NOT NULL DEFAULT '',
                        valor_centavos INTEGER NOT NULL,
                        tipo           TEXT    NOT NULL,
                        data           TEXT    NOT NULL,
                        meta_id        TEXT,
                        categoria      TEXT,
                        carteira_id    TEXT    DEFAULT '00000000-0000-0000-0000-000000000001',
                        FOREIGN KEY (meta_id) REFERENCES meta (id) ON DELETE SET NULL,
                        FOREIGN KEY (carteira_id) REFERENCES carteira (id) ON DELETE SET NULL
                    )
                    """);

            // Migrations para bancos existentes que não possuem as colunas novas
            executarMigrationSegura(stmt, "ALTER TABLE transacao ADD COLUMN comentario TEXT NOT NULL DEFAULT ''");
            executarMigrationSegura(stmt, "ALTER TABLE transacao ADD COLUMN categoria TEXT");
            executarMigrationSegura(stmt, "ALTER TABLE transacao ADD COLUMN carteira_id TEXT");
            stmt.execute("""
                    UPDATE transacao
                    SET carteira_id = '00000000-0000-0000-0000-000000000001'
                    WHERE carteira_id IS NULL
                    """);

            criarTabelasPlanejamento(stmt);
            criarIndices(stmt);
            criarBuscaTextual(stmt);

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inicializar banco de dados", e);
        }
    }

    private static String resolveDefaultUrl() {
        String explicitUrl = System.getProperty("smartfinance.db.url");
        if (explicitUrl != null && !explicitUrl.isBlank()) {
            return explicitUrl;
        }

        String explicitPath = System.getProperty("smartfinance.db.path");
        if (explicitPath != null && !explicitPath.isBlank()) {
            java.io.File file = new java.io.File(explicitPath).getAbsoluteFile();
            java.io.File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            return "jdbc:sqlite:" + file.getAbsolutePath();
        }

        String appDataOverride = System.getProperty("smartfinance.appdata.dir");
        if (appDataOverride != null && !appDataOverride.isBlank()) {
            java.io.File dir = new java.io.File(appDataOverride, "SmartFinance");
            dir.mkdirs();
            return "jdbc:sqlite:" + new java.io.File(dir, "financas.db").getAbsolutePath();
        }

        String appData = System.getenv("APPDATA");
        if (appData != null) {
            java.io.File dir = new java.io.File(appData, "SmartFinance");
            dir.mkdirs();
            return "jdbc:sqlite:" + new java.io.File(dir, "financas.db").getAbsolutePath();
        }

        return "jdbc:sqlite:financas.db";
    }

    private static String getEffectiveUrl() {
        return testOverrideUrl != null ? testOverrideUrl : DEFAULT_URL;
    }

    /**
     * Hook de testes para isolar o banco sem alterar o comportamento de produção.
     */
    public static void overrideUrlForTests(String jdbcUrl) {
        testOverrideUrl = jdbcUrl;
    }

    public static void clearOverrideUrlForTests() {
        testOverrideUrl = null;
    }

    /**
     * Executa um ALTER TABLE ignorando o erro caso a coluna já exista.
     * Usado exclusivamente para migrations de schema incremental.
     */
    private static void executarMigrationSegura(Statement stmt, String sql) {
        try {
            stmt.execute(sql);
        } catch (SQLException ignored) {
            // Coluna já existe — comportamento esperado em bancos existentes
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(getEffectiveUrl());
        configurarConexao(conn);
        return conn;
    }

    private static void configurarConexao(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA busy_timeout = 5000");
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA synchronous = NORMAL");
            stmt.execute("PRAGMA secure_delete = ON");
        }
    }

    private static void criarIndices(Statement stmt) throws SQLException {
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_transacao_data_desc ON transacao (data DESC)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_transacao_tipo_data ON transacao (tipo, data)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_transacao_categoria_data ON transacao (categoria, data)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_transacao_meta_id ON transacao (meta_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_transacao_carteira_id ON transacao (carteira_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_meta_nome ON meta (nome COLLATE NOCASE)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_compra_cartao_vencimento ON compra_cartao_credito (vencimento)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_emprestimo_tipo_data ON emprestimo (tipo, data_pagamento)");
    }

    private static void criarTabelasPlanejamento(Statement stmt) throws SQLException {
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS compra_cartao_credito (
                    id                    TEXT PRIMARY KEY,
                    nome                  TEXT    NOT NULL,
                    valor_total_centavos  INTEGER NOT NULL,
                    parcelas              INTEGER NOT NULL,
                    vencimento            TEXT    NOT NULL
                )
                """);

        stmt.execute("""
                CREATE TABLE IF NOT EXISTS emprestimo (
                    id              TEXT PRIMARY KEY,
                    tipo            TEXT    NOT NULL,
                    nome            TEXT    NOT NULL,
                    valor_centavos  INTEGER NOT NULL,
                    data_pagamento  TEXT    NOT NULL
                )
                """);
    }

    private static void criarBuscaTextual(Statement stmt) throws SQLException {
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

    private static boolean objetoExiste(Statement stmt, String nome) throws SQLException {
        try (var rs = stmt.executeQuery("""
                SELECT 1
                FROM sqlite_master
                WHERE name = '%s'
                LIMIT 1
                """.formatted(nome))) {
            return rs.next();
        }
    }
}
