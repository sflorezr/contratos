package co.empresa.gestioncontratos.enums;

public enum TipoPredio {
    URBANO("Urbano"),
    RURAL("Rural");

    private final String descripcion;

    TipoPredio(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}