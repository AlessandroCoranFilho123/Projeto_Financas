package app.service;

import app.model.Emprestimo;
import app.model.TipoEmprestimo;
import app.repository.EmprestimoDAO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EmprestimoService")
class EmprestimoServiceTest {

    @Test
    @DisplayName("salva anotacoes separadas por tipo e calcula totais")
    void salvaSeparadoPorTipo() {
        FakeEmprestimoDAO dao = new FakeEmprestimoDAO();
        EmprestimoService service = new EmprestimoService(dao);

        service.salvar(null, TipoEmprestimo.Emprestei, "Ana", 200_00L, LocalDate.of(2026, 6, 1));
        service.salvar(null, TipoEmprestimo.PediEmprestado, "Bruno", 500_00L, LocalDate.of(2026, 6, 5));

        assertAll(
                () -> assertEquals(200_00L, service.totalPorTipoCentavos(TipoEmprestimo.Emprestei)),
                () -> assertEquals(500_00L, service.totalPorTipoCentavos(TipoEmprestimo.PediEmprestado)),
                () -> assertEquals(1, service.listarPorTipo(TipoEmprestimo.Emprestei).size()),
                () -> assertEquals(1, service.listarPorTipo(TipoEmprestimo.PediEmprestado).size())
        );
    }

    @Test
    @DisplayName("atualiza anotacao existente")
    void atualizaExistente() {
        FakeEmprestimoDAO dao = new FakeEmprestimoDAO();
        EmprestimoService service = new EmprestimoService(dao);
        UUID id = UUID.randomUUID();
        dao.emprestimos.add(new Emprestimo(id, TipoEmprestimo.Emprestei, "Ana", 100_00L, LocalDate.now()));

        service.salvar(id, TipoEmprestimo.PediEmprestado, "Bruno", 300_00L, LocalDate.of(2026, 7, 1));

        Emprestimo atualizado = dao.emprestimos.getFirst();
        assertAll(
                () -> assertEquals(TipoEmprestimo.PediEmprestado, atualizado.tipo()),
                () -> assertEquals("Bruno", atualizado.nome()),
                () -> assertEquals(300_00L, atualizado.valorCentavos())
        );
    }

    @Test
    @DisplayName("valida campos obrigatorios")
    void validaCamposObrigatorios() {
        EmprestimoService service = new EmprestimoService(new FakeEmprestimoDAO());

        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> service.salvar(null, null, "Ana", 100_00L, LocalDate.now())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.salvar(null, TipoEmprestimo.Emprestei, "", 100_00L, LocalDate.now())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.salvar(null, TipoEmprestimo.Emprestei, "Ana", 0, LocalDate.now())),
                () -> assertThrows(NullPointerException.class,
                        () -> service.salvar(null, TipoEmprestimo.Emprestei, "Ana", 100_00L, null))
        );
    }

    static class FakeEmprestimoDAO extends EmprestimoDAO {
        final List<Emprestimo> emprestimos = new ArrayList<>();

        @Override
        public void inserir(Emprestimo emprestimo) {
            emprestimos.add(emprestimo);
        }

        @Override
        public void atualizar(Emprestimo emprestimo) {
            emprestimos.removeIf(e -> e.id().equals(emprestimo.id()));
            emprestimos.add(emprestimo);
        }

        @Override
        public List<Emprestimo> listarPorTipo(TipoEmprestimo tipo) {
            return emprestimos.stream()
                    .filter(e -> e.tipo() == tipo)
                    .toList();
        }

        @Override
        public List<Emprestimo> listarTodas() {
            return new ArrayList<>(emprestimos);
        }
    }
}
