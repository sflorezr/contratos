package co.empresa.gestioncontratos.enums;

public enum EstadoPredio {
    ACTIVO("Activo"),
    FINALIZADO("Finalizado"),
    SUSPENDIDO("Suspendido");

    private final String descripcion;

    EstadoPredio(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
