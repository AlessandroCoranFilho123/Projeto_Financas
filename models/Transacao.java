package models;

public class Transacao {
    private final String tipo;
    private final double valor;
    private final String categoria;

    public Transacao (String tipo, double valor, String categoria) {
        this.tipo = tipo;
        this.valor = valor;
        this.categoria = categoria;
    }

    public String getTipo () {
        return tipo;
    }
    public double getValor () {
        return valor;
    }
    public String getCategoria () {
        return categoria;
    }

    @Override
    public String toString () {
        return tipo.toUpperCase() + " - R$ " + valor + " (" + categoria + ")";
    }
}
