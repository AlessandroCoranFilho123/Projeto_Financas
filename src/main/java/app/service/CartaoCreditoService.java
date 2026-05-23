package app.service;

import app.model.CompraCartaoCredito;
import app.repository.CartaoCreditoDAO;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CartaoCreditoService {

    private final CartaoCreditoDAO dao;

    public CartaoCreditoService() {
        this(new CartaoCreditoDAO());
    }

    public CartaoCreditoService(CartaoCreditoDAO dao) {
        this.dao = Objects.requireNonNull(dao, "DAO de cartao e obrigatorio");
    }

    public void salvar(UUID id, String nome, long valorTotalCentavos, int parcelas, LocalDate vencimento) {
        CompraCartaoCredito compra = validar(id, nome, valorTotalCentavos, parcelas, vencimento);
        if (id == null) {
            dao.inserir(compra);
        } else {
            dao.atualizar(compra);
        }
    }

    public void deletar(UUID id) {
        Objects.requireNonNull(id, "ID e obrigatorio");
        dao.deletar(id);
    }

    public List<CompraCartaoCredito> listarTodas() {
        return dao.listarTodas();
    }

    public long calcularTotalAbertoCentavos() {
        return listarTodas().stream()
                .mapToLong(CompraCartaoCredito::valorTotalCentavos)
                .sum();
    }

    public long calcularTotalParcelasMesCentavos() {
        return listarTodas().stream()
                .mapToLong(CompraCartaoCredito::valorParcelaCentavos)
                .sum();
    }

    public long contarVencendoEmAte(int dias, LocalDate hoje) {
        Objects.requireNonNull(hoje, "Data base e obrigatoria");
        return listarTodas().stream()
                .filter(compra -> {
                    long diasAteVencimento = compra.diasAteVencimento(hoje);
                    return diasAteVencimento >= 0 && diasAteVencimento <= dias;
                })
                .count();
    }

    private CompraCartaoCredito validar(
            UUID id,
            String nome,
            long valorTotalCentavos,
            int parcelas,
            LocalDate vencimento
    ) {
        Objects.requireNonNull(nome, "Nome e obrigatorio");
        Objects.requireNonNull(vencimento, "Vencimento e obrigatorio");

        if (nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome nao pode estar vazio");
        }
        if (valorTotalCentavos <= 0) {
            throw new IllegalArgumentException("Valor deve ser positivo");
        }
        if (parcelas <= 0) {
            throw new IllegalArgumentException("Parcelas devem ser maiores que zero");
        }

        return new CompraCartaoCredito(id, nome, valorTotalCentavos, parcelas, vencimento);
    }
}
