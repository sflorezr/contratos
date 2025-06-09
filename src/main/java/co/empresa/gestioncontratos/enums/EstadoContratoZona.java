package co.empresa.gestioncontratos.enums;
import lombok.Getter;

@Getter
public enum EstadoContratoZona {
    ACTIVO("Activo", "La zona está activa en el contrato"),
    SUSPENDIDO("Suspendido", "La zona está temporalmente suspendida"),
    COMPLETADO("Completado", "Todas las actividades de la zona están completas"),
    CANCELADO("Cancelado", "La zona fue cancelada del contrato");

    private final String descripcion;
    private final String detalle;

    EstadoContratoZona(String descripcion, String detalle) {
        this.descripcion = descripcion;
        this.detalle = detalle;
    }

    public boolean esActivo() {
        return this == ACTIVO;
    }

    public boolean esCompletado() {
        return this == COMPLETADO;
    }

    public boolean esSuspendido() {
        return this == SUSPENDIDO;
    }

    public boolean esCancelado() {
        return this == CANCELADO;
    }
}
