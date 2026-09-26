package co.edu.uniquindio.sga.domain.comun;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Valor monetario en pesos colombianos (COP). Usa precisión exacta y no redondea
 * en las operaciones intermedias: el redondeo al peso se aplica explícitamente
 * al final del cálculo de cada cargo con {@link #redondear()}.
 */
public final class Dinero implements Comparable<Dinero> {

    public static final Dinero CERO = new Dinero(BigDecimal.ZERO);

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final BigDecimal valor;

    private Dinero(BigDecimal valor) {
        ExcepcionNegocio.exigir(valor != null, CodigoError.DINERO_INVALIDO);
        this.valor = valor;
    }

    public static Dinero de(long pesos) {
        return new Dinero(BigDecimal.valueOf(pesos));
    }

    public static Dinero de(BigDecimal valor) {
        return new Dinero(valor);
    }

    public Dinero mas(Dinero otro) {
        return new Dinero(valor.add(otro.valor));
    }

    public Dinero menos(Dinero otro) {
        return new Dinero(valor.subtract(otro.valor));
    }

    public Dinero por(long factor) {
        return new Dinero(valor.multiply(BigDecimal.valueOf(factor)));
    }

    /** Porcentaje del valor, sin redondear. Ej.: {@code de(1000).porcentaje(30)} = 300. */
    public Dinero porcentaje(BigDecimal porcentaje) {
        return new Dinero(valor.multiply(porcentaje).divide(CIEN));
    }

    public Dinero negar() {
        return new Dinero(valor.negate());
    }

    /** Redondea al peso más cercano (mitades hacia arriba). */
    public Dinero redondear() {
        return new Dinero(valor.setScale(0, RoundingMode.HALF_UP));
    }

    public boolean esCero() {
        return valor.signum() == 0;
    }

    public boolean esPositivo() {
        return valor.signum() > 0;
    }

    public boolean esNegativo() {
        return valor.signum() < 0;
    }

    public boolean esMenorQue(Dinero otro) {
        return compareTo(otro) < 0;
    }

    public BigDecimal valor() {
        return valor;
    }

    @Override
    public int compareTo(Dinero otro) {
        return valor.compareTo(otro.valor);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Dinero otro && valor.compareTo(otro.valor) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return "COP " + valor.toPlainString();
    }
}
