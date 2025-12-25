package utils;
import java.util.List;
import app.Main.Transacao;

public class RelatorioHelper {

    public static double calcularSaldo(List<Transacao> transacoes) {
        double saldo = 0;
        for (Transacao t : transacoes) {
            if (t.tipo().equalsIgnoreCase("Entrada")) {
                saldo += t.valor();
            } else {
                saldo -= t.valor();
            }
        }
        return saldo;
    }
}
