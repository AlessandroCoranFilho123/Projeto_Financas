package app.model;

import java.time.LocalDate;
import java.util.UUID;

public record Emprestimo(
        UUID id,
        TipoEmprestimo tipo,
        String nome,
        long valorCentavos,
        LocalDate dataPagamento
) {

    public Emprestimo {
        if (id == null) id = UUID.randomUUID();
        nome = nome == null ? "" : nome.trim();
    }
}
