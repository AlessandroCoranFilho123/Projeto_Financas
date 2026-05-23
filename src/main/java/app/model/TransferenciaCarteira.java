package app.model;

import java.time.LocalDate;
import java.util.UUID;

public record TransferenciaCarteira(
        UUID id,
        UUID origemCarteiraId,
        UUID destinoCarteiraId,
        long valorCentavos,
        LocalDate data,
        String comentario
) {
    public TransferenciaCarteira {
        if (id == null) id = UUID.randomUUID();
        comentario = comentario == null ? "" : comentario.trim();
    }
}
