package co.empresa.gestioncontratos.enums;

public enum PerfilUsuario {
    ADMINISTRADOR("Administrador"),
    SUPERVISOR("Supervisor"),
    COORDINADOR("Coordinador"),
    OPERARIO("Operario");

    private final String descripcion;

    PerfilUsuario(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}