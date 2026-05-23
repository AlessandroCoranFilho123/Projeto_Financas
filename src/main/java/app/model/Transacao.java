package app.model;

import java.time.LocalDate;
import java.util.UUID;

public record Transacao(
        UUID id,
        String descricao,
        String comentario,
        long valorCentavos,
        TipoTransacao tipo,
        LocalDate data,
        UUID metaId,
        Categoria categoria,
        UUID carteiraId
) {
    public Transacao {
        if (id == null) id = UUID.randomUUID(); // Garante que toda transação tenha id
        if (comentario == null) comentario = ""; // Permite comentário em branco
    }

    public Transacao(
            UUID id,
            String descricao,
            String comentario,
            long valorCentavos,
            TipoTransacao tipo,
            LocalDate data,
            UUID metaId,
            Categoria categoria
    ) {
        this(id, descricao, comentario, valorCentavos, tipo, data, metaId, categoria, null);
    }
}
