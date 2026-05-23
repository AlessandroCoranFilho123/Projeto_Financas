package app.service;

import app.model.Categoria;
import app.model.Meta;
import app.model.TipoTransacao;
import app.model.Transacao;
import app.repository.MetaDAO;
import app.repository.TransacaoDAO;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

public class TransacaoService {

    private final TransacaoDAO transacaoDAO;
    private final MetaDAO metaDAO;
    private final CarteiraService carteiraService;

    public TransacaoService(TransacaoDAO transacaoDAO, MetaDAO metaDAO) {
        this.transacaoDAO = Objects.requireNonNull(transacaoDAO);
        this.metaDAO = Objects.requireNonNull(metaDAO);
        this.carteiraService = null;
    }

    public TransacaoService(TransacaoDAO transacaoDAO, MetaDAO metaDAO, CarteiraService carteiraService) {
        this.transacaoDAO = Objects.requireNonNull(transacaoDAO);
        this.metaDAO = Objects.requireNonNull(metaDAO);
        this.carteiraService = Objects.requireNonNull(carteiraService);
    }

    public void registrar(
            TipoTransacao tipo,
            Categoria categoria,
            long valorCentavos,
            Meta meta,
            String comentario,
            LocalDate data) {
        registrar(tipo, categoria, valorCentavos, meta, comentario, data, carteiraPadraoIdOuNull());
    }

    public void registrar(
            TipoTransacao tipo,
            Categoria categoria,
            long valorCentavos,
            Meta meta,
            String comentario,
            LocalDate data,
            UUID carteiraId) {

        Objects.requireNonNull(tipo, "tipo é obrigatório");
        Objects.requireNonNull(categoria, "categoria é obrigatória");
        Objects.requireNonNull(data, "data é obrigatória");

        if (valorCentavos <= 0)
            throw new IllegalArgumentException("Valores devem ser positivos");

        boolean exigeMeta =
                categoria == Categoria.AdicionarMeta ||
                        categoria == Categoria.RetirarMeta;

        if (exigeMeta && meta == null)
            throw new IllegalArgumentException("Selecione uma meta");

        if (categoria == Categoria.AdicionarMeta) {

            long saldo = calcularSaldoParaValidacao(carteiraId);
            if (valorCentavos > saldo)
                throw new IllegalArgumentException("Saldo insuficiente");

            long permitido = Math.min(valorCentavos, meta.restanteParaAlvo());
            if (permitido <= 0)
                throw new IllegalArgumentException("Meta já alcançada");

            meta.adicionar(permitido);
            metaDAO.atualizar(meta);

            Transacao t = new Transacao(
                    UUID.randomUUID(),
                    "Aplicação em meta: " + meta.getNome(),
                    comentario != null ? comentario : "",
                    permitido,
                    TipoTransacao.Saida,
                    data,
                    meta.getId(),
                    Categoria.AdicionarMeta,
                    carteiraId
            );

            transacaoDAO.inserir(t);
            return;
        }

        if (categoria == Categoria.RetirarMeta) {

            long retirado = meta.retirar(valorCentavos);
            if (retirado <= 0)
                throw new IllegalArgumentException("Valor deve ser maior que 0");

            metaDAO.atualizar(meta);

            Transacao t = new Transacao(
                    UUID.randomUUID(),
                    "Resgate de meta: " + meta.getNome(),
                    comentario != null ? comentario : "",
                    retirado,
                    TipoTransacao.Entrada,
                    data,
                    meta.getId(),
                    Categoria.RetirarMeta,
                    carteiraId
            );

            transacaoDAO.inserir(t);
            return;
        }

        String nome = categoria.toString();
        Transacao t = new Transacao(
                UUID.randomUUID(),
                nome,
                comentario != null ? comentario : "",
                valorCentavos,
                tipo,
                data,
                null,
                categoria,
                carteiraId
        );

        transacaoDAO.inserir(t);
    }

    /**
     * Atualiza uma transação existente.
     *
     * <p>Para transações comuns (sem meta), exclui a original e reinsere
     * com os novos dados, preservando o UUID original.
     *
     * <p>Para transações de AdicionarMeta/RetirarMeta, a exclusão
     * já reverte o saldo da meta via {@link #excluirTransacao}; em seguida
     * chama {@link #registrar} com o ID original, garantindo que todas as
     * validações de saldo/meta sejam reaplicadas.
     */
    public void atualizar(
            UUID idOriginal,
            TipoTransacao tipo,
            Categoria categoria,
            long valorCentavos,
            Meta meta,
            String comentario,
            LocalDate data) {
        atualizar(idOriginal, tipo, categoria, valorCentavos, meta, comentario, data, null);
    }

    public void atualizar(
            UUID idOriginal,
            TipoTransacao tipo,
            Categoria categoria,
            long valorCentavos,
            Meta meta,
            String comentario,
            LocalDate data,
            UUID carteiraId) {

        Objects.requireNonNull(idOriginal, "ID original é obrigatório");

        Transacao original = transacaoDAO.buscarPorId(idOriginal);
        if (original == null)
            throw new IllegalArgumentException("Transação original não encontrada");

        excluirTransacao(idOriginal, original.categoria());

        try {
            UUID carteiraEfetiva = carteiraId != null ? carteiraId : original.carteiraId();
            if (categoria == Categoria.AdicionarMeta || categoria == Categoria.RetirarMeta) {
                registrar(tipo, categoria, valorCentavos, meta, comentario, data, carteiraEfetiva);
                return;
            }

            Objects.requireNonNull(tipo, "tipo é obrigatório");
            Objects.requireNonNull(categoria, "categoria é obrigatória");
            Objects.requireNonNull(data, "data é obrigatória");
            if (valorCentavos <= 0)
                throw new IllegalArgumentException("Valores devem ser positivos");

            String nome = categoria.toString();
            Transacao atualizada = new Transacao(
                    idOriginal,
                    nome,
                    comentario != null ? comentario : "",
                    valorCentavos,
                    tipo,
                    data,
                    meta != null ? meta.getId() : null,
                    categoria,
                    carteiraEfetiva
            );
            transacaoDAO.inserir(atualizada);
        } catch (RuntimeException e) {
            restaurarTransacaoOriginal(original);
            throw e;
        }
    }

    public long calcularSaldoDisponivelCentavos() {
        return carteiraService != null
                ? carteiraService.calcularSaldoTotalCentavos()
                : transacaoDAO.calcularSaldo();
    }

    public long calcularReceitasMes(YearMonth mes) {
        return transacaoDAO.calcularTotalPorTipoEMes(
                TipoTransacao.Entrada,
                mes.toString()
        );
    }

    public long calcularDespesasMes(YearMonth mes) {
        return transacaoDAO.calcularTotalPorTipoEMes(
                TipoTransacao.Saida,
                mes.toString()
        );
    }

    public void excluirTransacao(UUID transacaoId, Categoria categoria) {
        Objects.requireNonNull(transacaoId, "ID da transação é obrigatório");

        Transacao t = transacaoDAO.buscarPorId(transacaoId);
        if (t == null) return;

        Categoria categoriaEfetiva = t.categoria() != null ? t.categoria() : categoria;

        if (categoriaEfetiva == Categoria.AdicionarMeta && t.metaId() != null) {
            Meta meta = metaDAO.buscarPorId(t.metaId());
            if (meta != null) {
                meta.retirar(t.valorCentavos());
                metaDAO.atualizar(meta);
            }
        }

        if (categoriaEfetiva == Categoria.RetirarMeta && t.metaId() != null) {
            Meta meta = metaDAO.buscarPorId(t.metaId());
            if (meta != null) {
                meta.adicionar(t.valorCentavos());
                metaDAO.atualizar(meta);
            }
        }

        transacaoDAO.excluir(transacaoId);
    }

    private void restaurarTransacaoOriginal(Transacao original) {
        if (original.categoria() == Categoria.AdicionarMeta && original.metaId() != null) {
            Meta meta = metaDAO.buscarPorId(original.metaId());
            if (meta != null) {
                meta.adicionar(original.valorCentavos());
                metaDAO.atualizar(meta);
            }
        }

        if (original.categoria() == Categoria.RetirarMeta && original.metaId() != null) {
            Meta meta = metaDAO.buscarPorId(original.metaId());
            if (meta != null) {
                meta.retirar(original.valorCentavos());
                metaDAO.atualizar(meta);
            }
        }

        transacaoDAO.inserir(original);
    }

    private UUID carteiraPadraoIdOuNull() {
        return carteiraService != null ? carteiraService.carteiraPadraoId() : null;
    }

    private long calcularSaldoParaValidacao(UUID carteiraId) {
        if (carteiraService != null && carteiraId != null) {
            return carteiraService.calcularSaldoCarteiraCentavos(carteiraId);
        }
        return calcularSaldoDisponivelCentavos();
    }
}
