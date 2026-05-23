package app.model;

import java.util.UUID;

public record Carteira(
        UUID id,
        String nome,
        long saldoInicialCentavos
) {
    public Carteira {
        if (id == null) id = UUID.randomUUID();
        nome = nome == null ? "" : nome.trim();
    }

    @Override
    public String toString() {
        return nome;
    }
}
