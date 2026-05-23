package app.service;

import app.model.CompraCartaoCredito;
import app.repository.CartaoCreditoDAO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CartaoCreditoService")
class CartaoCreditoServiceTest {

    @Test
    @DisplayName("salva compra nova e calcula totais")
    void salvaCompraNovaECalculaTotais() {
        FakeCartaoCreditoDAO dao = new FakeCartaoCreditoDAO();
        CartaoCreditoService service = new CartaoCreditoService(dao);

        service.salvar(null, "Curso", 1200_00L, 6, LocalDate.of(2026, 5, 18));

        assertAll(
                () -> assertEquals(1, dao.compras.size()),
                () -> assertEquals(1200_00L, service.calcularTotalAbertoCentavos()),
                () -> assertEquals(200_00L, service.calcularTotalParcelasMesCentavos()),
                () -> assertEquals(1, service.contarVencendoEmAte(7, LocalDate.of(2026, 5, 14)))
        );
    }

    @Test
    @DisplayName("atualiza compra existente")
    void atualizaCompraExistente() {
        FakeCartaoCreditoDAO dao = new FakeCartaoCreditoDAO();
        CartaoCreditoService service = new CartaoCreditoService(dao);
        UUID id = UUID.randomUUID();
        dao.compras.add(new CompraCartaoCredito(id, "Antigo", 500_00L, 5, LocalDate.of(2026, 6, 1)));

        service.salvar(id, "Novo", 900_00L, 3, LocalDate.of(2026, 6, 5));

        assertAll(
                () -> assertEquals("Novo", dao.compras.getFirst().nome()),
                () -> assertEquals(900_00L, dao.compras.getFirst().valorTotalCentavos()),
                () -> assertEquals(3, dao.compras.getFirst().parcelas())
        );
    }

    @Test
    @DisplayName("valida campos obrigatorios")
    void validaCamposObrigatorios() {
        CartaoCreditoService service = new CartaoCreditoService(new FakeCartaoCreditoDAO());

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.salvar(null, "", 100_00L, 1, LocalDate.now())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.salvar(null, "Compra", 0, 1, LocalDate.now())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.salvar(null, "Compra", 100_00L, 0, LocalDate.now())),
                () -> assertThrows(NullPointerException.class,
                        () -> service.salvar(null, "Compra", 100_00L, 1, null))
        );
    }

    static class FakeCartaoCreditoDAO extends CartaoCreditoDAO {
        final List<CompraCartaoCredito> compras = new ArrayList<>();

        @Override
        public void inserir(CompraCartaoCredito compra) {
            compras.add(compra);
        }

        @Override
        public void atualizar(CompraCartaoCredito compra) {
            compras.removeIf(c -> c.id().equals(compra.id()));
            compras.add(compra);
        }

        @Override
        public List<CompraCartaoCredito> listarTodas() {
            return new ArrayList<>(compras);
        }
    }
}
