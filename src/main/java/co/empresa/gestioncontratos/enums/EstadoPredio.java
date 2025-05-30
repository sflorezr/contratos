package co.empresa.gestioncontratos.enums;

public enum EstadoPredio {
    ACTIVO("Activo"),
    FINALIZADO("Finalizado"),
    PENDIENTE("Pendiente"),
    SUSPENDIDO("Suspendido"), ASIGNADO("Asignado"), COMPLETADO("Completado");

    private final String descripcion;

    EstadoPredio(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
