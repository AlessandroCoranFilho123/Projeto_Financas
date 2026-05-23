package app.service;

import app.model.Carteira;
import app.model.CarteiraResumo;
import app.model.TransferenciaCarteira;
import app.repository.CarteiraDAO;
import app.repository.TransacaoDAO;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CarteiraService {

    private final CarteiraDAO carteiraDAO;
    private final TransacaoDAO transacaoDAO;

    public CarteiraService() {
        this(new CarteiraDAO(), new TransacaoDAO());
    }

    public CarteiraService(CarteiraDAO carteiraDAO, TransacaoDAO transacaoDAO) {
        this.carteiraDAO = Objects.requireNonNull(carteiraDAO, "DAO de carteira é obrigatório");
        this.transacaoDAO = Objects.requireNonNull(transacaoDAO, "DAO de transação é obrigatório");
    }

    public List<Carteira> listarCarteiras() {
        return carteiraDAO.listarTodas();
    }

    public Carteira buscarPorId(UUID id) {
        return carteiraDAO.buscarPorId(id);
    }

    public Carteira carteiraPadrao() {
        return carteiraDAO.buscarBanco();
    }

    public UUID carteiraPadraoId() {
        Carteira carteira = carteiraPadrao();
        return carteira != null ? carteira.id() : CarteiraDAO.BANCO_ID;
    }

    public List<CarteiraResumo> listarResumos() {
        return listarCarteiras().stream()
                .map(carteira -> new CarteiraResumo(carteira, calcularSaldoCarteiraCentavos(carteira.id())))
                .toList();
    }

    public long calcularSaldoTotalCentavos() {
        return listarResumos().stream()
                .mapToLong(CarteiraResumo::saldoCentavos)
                .sum();
    }

    public long calcularSaldoCarteiraCentavos(UUID carteiraId) {
        Carteira carteira = carteiraDAO.buscarPorId(carteiraId);
        if (carteira == null) {
            return 0L;
        }

        long saldoTransacoes = transacaoDAO.calcularSaldoPorCarteira(carteira.id());
        long transferenciasEntrada = carteiraDAO.calcularTransferenciasEntrada(carteira.id());
        long transferenciasSaida = carteiraDAO.calcularTransferenciasSaida(carteira.id());

        return carteira.saldoInicialCentavos()
                + saldoTransacoes
                + transferenciasEntrada
                - transferenciasSaida;
    }

    public void atualizarSaldoInicial(UUID carteiraId, long saldoInicialCentavos) {
        carteiraDAO.atualizarSaldoInicial(carteiraId, saldoInicialCentavos);
    }

    public void transferir(
            UUID origemCarteiraId,
            UUID destinoCarteiraId,
            long valorCentavos,
            LocalDate data,
            String comentario
    ) {
        Objects.requireNonNull(origemCarteiraId, "Carteira de origem é obrigatória");
        Objects.requireNonNull(destinoCarteiraId, "Carteira de destino é obrigatória");
        Objects.requireNonNull(data, "Data é obrigatória");

        if (origemCarteiraId.equals(destinoCarteiraId)) {
            throw new IllegalArgumentException("Escolha carteiras diferentes");
        }
        if (valorCentavos <= 0) {
            throw new IllegalArgumentException("Valor deve ser positivo");
        }
        if (carteiraDAO.buscarPorId(origemCarteiraId) == null) {
            throw new IllegalArgumentException("Carteira de origem não encontrada");
        }
        if (carteiraDAO.buscarPorId(destinoCarteiraId) == null) {
            throw new IllegalArgumentException("Carteira de destino não encontrada");
        }
        if (valorCentavos > calcularSaldoCarteiraCentavos(origemCarteiraId)) {
            throw new IllegalArgumentException("Saldo insuficiente na carteira de origem");
        }

        carteiraDAO.inserirTransferencia(new TransferenciaCarteira(
                UUID.randomUUID(),
                origemCarteiraId,
                destinoCarteiraId,
                valorCentavos,
                data,
                comentario
        ));
    }
}
