package app.service;

import app.model.Emprestimo;
import app.model.TipoEmprestimo;
import app.repository.EmprestimoDAO;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class EmprestimoService {

    private final EmprestimoDAO dao;

    public EmprestimoService() {
        this(new EmprestimoDAO());
    }

    public EmprestimoService(EmprestimoDAO dao) {
        this.dao = Objects.requireNonNull(dao, "DAO de emprestimo e obrigatorio");
    }

    public void salvar(UUID id, TipoEmprestimo tipo, String nome, long valorCentavos, LocalDate dataPagamento) {
        Emprestimo emprestimo = validar(id, tipo, nome, valorCentavos, dataPagamento);
        if (id == null) {
            dao.inserir(emprestimo);
        } else {
            dao.atualizar(emprestimo);
        }
    }

    public void deletar(UUID id) {
        Objects.requireNonNull(id, "ID e obrigatorio");
        dao.deletar(id);
    }

    public List<Emprestimo> listarTodas() {
        return dao.listarTodas();
    }

    public List<Emprestimo> listarPorTipo(TipoEmprestimo tipo) {
        Objects.requireNonNull(tipo, "Tipo e obrigatorio");
        return dao.listarPorTipo(tipo);
    }

    public long totalPorTipoCentavos(TipoEmprestimo tipo) {
        return listarPorTipo(tipo).stream()
                .mapToLong(Emprestimo::valorCentavos)
                .sum();
    }

    private Emprestimo validar(
            UUID id,
            TipoEmprestimo tipo,
            String nome,
            long valorCentavos,
            LocalDate dataPagamento
    ) {
        Objects.requireNonNull(tipo, "Tipo e obrigatorio");
        Objects.requireNonNull(nome, "Nome e obrigatorio");
        Objects.requireNonNull(dataPagamento, "Data de pagamento e obrigatoria");

        if (nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome nao pode estar vazio");
        }
        if (valorCentavos <= 0) {
            throw new IllegalArgumentException("Valor deve ser positivo");
        }

        return new Emprestimo(id, tipo, nome, valorCentavos, dataPagamento);
    }
}
