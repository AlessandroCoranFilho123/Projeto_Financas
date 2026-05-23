package app.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record CompraCartaoCredito(
        UUID id,
        String nome,
        long valorTotalCentavos,
        int parcelas,
        LocalDate vencimento
) {

    public CompraCartaoCredito {
        if (id == null) id = UUID.randomUUID();
        nome = nome == null ? "" : nome.trim();
    }

    public long valorParcelaCentavos() {
        if (parcelas <= 0) return 0;
        return Math.round(valorTotalCentavos / (double) parcelas);
    }

    public long diasAteVencimento(LocalDate hoje) {
        return ChronoUnit.DAYS.between(hoje, vencimento);
    }

    public StatusVencimento statusVencimento(LocalDate hoje) {
        long dias = diasAteVencimento(hoje);
        if (dias <= 0) return StatusVencimento.Vermelho;
        if (dias <= 7) return StatusVencimento.Laranja;
        return StatusVencimento.Verde;
    }

    public String avisoVencimento(LocalDate hoje) {
        long dias = diasAteVencimento(hoje);
        if (dias < 0) return "Vencido ha " + Math.abs(dias) + " dia(s)";
        if (dias == 0) return "Vence hoje";
        if (dias <= 7) return "Vence em " + dias + " dia(s)";
        return "";
    }
}
