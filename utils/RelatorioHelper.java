package utils;

import models.Transacao;
import java.util.List;

public class RelatorioHelper {
    public static double calcularSaldo(List<Transacao> transacoes) {
        double saldo = 0;
        for (Transacao t : transacoes) {
            if (t.getTipo().equalsIgnoreCase("Entrada")) {
                saldo += t.getValor();
            } else {
                saldo -= t.getValor();
            }
        }
        return saldo;
    }
}
