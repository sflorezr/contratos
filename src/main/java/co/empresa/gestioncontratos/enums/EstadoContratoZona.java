package co.empresa.gestioncontratos.enums;

public enum EstadoContratoZona {
    ACTIVO("Activo"),
    FINALIZADO("Finalizado"),
    SUSPENDIDO("Suspendido");

    private final String descripcion;

    EstadoContratoZona(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
