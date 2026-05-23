PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS meta
(
    id             TEXT PRIMARY KEY,
    nome           TEXT    NOT NULL,
    alvo_centavos  INTEGER,
    atual_centavos INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS carteira
(
    id                     TEXT PRIMARY KEY,
    nome                   TEXT    NOT NULL UNIQUE,
    saldo_inicial_centavos INTEGER NOT NULL DEFAULT 0
);

INSERT OR IGNORE INTO carteira (id, nome, saldo_inicial_centavos)
VALUES ('00000000-0000-0000-0000-000000000001', 'Banco', 0),
       ('00000000-0000-0000-0000-000000000002', 'Dinheiro físico', 0);

CREATE TABLE IF NOT EXISTS transacao
(
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
);

CREATE INDEX IF NOT EXISTS idx_transacao_data_desc ON transacao (data DESC);
CREATE INDEX IF NOT EXISTS idx_transacao_tipo_data ON transacao (tipo, data);
CREATE INDEX IF NOT EXISTS idx_transacao_categoria_data ON transacao (categoria, data);
CREATE INDEX IF NOT EXISTS idx_transacao_meta_id ON transacao (meta_id);
CREATE INDEX IF NOT EXISTS idx_transacao_carteira_id ON transacao (carteira_id);
CREATE INDEX IF NOT EXISTS idx_meta_nome ON meta (nome COLLATE NOCASE);
CREATE INDEX IF NOT EXISTS idx_carteira_nome ON carteira (nome COLLATE NOCASE);

CREATE TABLE IF NOT EXISTS compra_cartao_credito
(
    id                   TEXT PRIMARY KEY,
    nome                 TEXT    NOT NULL,
    valor_total_centavos INTEGER NOT NULL,
    parcelas             INTEGER NOT NULL,
    vencimento           TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS emprestimo
(
    id             TEXT PRIMARY KEY,
    tipo           TEXT    NOT NULL,
    nome           TEXT    NOT NULL,
    valor_centavos INTEGER NOT NULL,
    data_pagamento TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_compra_cartao_vencimento ON compra_cartao_credito (vencimento);
CREATE INDEX IF NOT EXISTS idx_emprestimo_tipo_data ON emprestimo (tipo, data_pagamento);

CREATE TABLE IF NOT EXISTS transferencia_carteira
(
    id                  TEXT PRIMARY KEY,
    origem_carteira_id  TEXT    NOT NULL,
    destino_carteira_id TEXT    NOT NULL,
    valor_centavos      INTEGER NOT NULL,
    data                TEXT    NOT NULL,
    comentario          TEXT    NOT NULL DEFAULT '',
    FOREIGN KEY (origem_carteira_id) REFERENCES carteira (id) ON DELETE CASCADE,
    FOREIGN KEY (destino_carteira_id) REFERENCES carteira (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_transferencia_origem ON transferencia_carteira (origem_carteira_id, data);
CREATE INDEX IF NOT EXISTS idx_transferencia_destino ON transferencia_carteira (destino_carteira_id, data);

CREATE VIRTUAL TABLE IF NOT EXISTS transacao_fts USING fts5(
    descricao,
    comentario,
    categoria,
    tipo,
    data,
    content='transacao',
    content_rowid='rowid',
    tokenize='unicode61 remove_diacritics 2'
);

CREATE TRIGGER IF NOT EXISTS transacao_ai AFTER INSERT ON transacao BEGIN
    INSERT INTO transacao_fts(rowid, descricao, comentario, categoria, tipo, data)
    VALUES (new.rowid, new.descricao, new.comentario, COALESCE(new.categoria, ''), new.tipo, new.data);
END;

CREATE TRIGGER IF NOT EXISTS transacao_ad AFTER DELETE ON transacao BEGIN
    INSERT INTO transacao_fts(transacao_fts, rowid, descricao, comentario, categoria, tipo, data)
    VALUES ('delete', old.rowid, old.descricao, old.comentario, COALESCE(old.categoria, ''), old.tipo, old.data);
END;

CREATE TRIGGER IF NOT EXISTS transacao_au AFTER UPDATE ON transacao BEGIN
    INSERT INTO transacao_fts(transacao_fts, rowid, descricao, comentario, categoria, tipo, data)
    VALUES ('delete', old.rowid, old.descricao, old.comentario, COALESCE(old.categoria, ''), old.tipo, old.data);
    INSERT INTO transacao_fts(rowid, descricao, comentario, categoria, tipo, data)
    VALUES (new.rowid, new.descricao, new.comentario, COALESCE(new.categoria, ''), new.tipo, new.data);
END;

INSERT INTO transacao_fts(transacao_fts) VALUES ('rebuild');
