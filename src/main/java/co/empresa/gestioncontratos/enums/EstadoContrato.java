package co.empresa.gestioncontratos.enums;

public enum EstadoContrato {
    ACTIVO("Activo"),
    FINALIZADO("Finalizado"),
    SUSPENDIDO("Suspendido");

    private final String descripcion;

    EstadoContrato(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
