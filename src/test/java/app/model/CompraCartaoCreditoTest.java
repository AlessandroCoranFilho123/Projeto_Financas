package app.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CompraCartaoCredito")
class CompraCartaoCreditoTest {

    @Test
    @DisplayName("calcula valor da parcela")
    void calculaValorParcela() {
        CompraCartaoCredito compra = new CompraCartaoCredito(
                UUID.randomUUID(),
                "Notebook",
                2400_00L,
                12,
                LocalDate.of(2026, 6, 10)
        );

        assertEquals(200_00L, compra.valorParcelaCentavos());
    }

    @Test
    @DisplayName("classifica vencimento em verde, laranja e vermelho")
    void classificaVencimento() {
        LocalDate hoje = LocalDate.of(2026, 5, 14);

        assertAll(
                () -> assertEquals(StatusVencimento.Verde,
                        compraComVencimento(hoje.plusDays(10)).statusVencimento(hoje)),
                () -> assertEquals(StatusVencimento.Laranja,
                        compraComVencimento(hoje.plusDays(7)).statusVencimento(hoje)),
                () -> assertEquals(StatusVencimento.Vermelho,
                        compraComVencimento(hoje).statusVencimento(hoje)),
                () -> assertEquals(StatusVencimento.Vermelho,
                        compraComVencimento(hoje.minusDays(1)).statusVencimento(hoje))
        );
    }

    @Test
    @DisplayName("gera avisos de vencimento")
    void geraAvisos() {
        LocalDate hoje = LocalDate.of(2026, 5, 14);

        assertAll(
                () -> assertEquals("", compraComVencimento(hoje.plusDays(8)).avisoVencimento(hoje)),
                () -> assertEquals("Vence em 7 dia(s)", compraComVencimento(hoje.plusDays(7)).avisoVencimento(hoje)),
                () -> assertEquals("Vence hoje", compraComVencimento(hoje).avisoVencimento(hoje)),
                () -> assertEquals("Vencido ha 3 dia(s)", compraComVencimento(hoje.minusDays(3)).avisoVencimento(hoje))
        );
    }

    private CompraCartaoCredito compraComVencimento(LocalDate vencimento) {
        return new CompraCartaoCredito(UUID.randomUUID(), "Compra", 100_00L, 1, vencimento);
    }
}
