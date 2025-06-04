package co.empresa.gestioncontratos.dto;

import co.empresa.gestioncontratos.entity.Usuario;
import co.empresa.gestioncontratos.enums.PerfilUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResumenDTO {
    
    private Long id;
    private UUID uuid;
    private String username;
    private String nombre;
    private String apellido;
    private String email;
    private PerfilUsuario perfil;
    private Boolean activo;
    
    // Campos calculados/adicionales
    private String nombreCompleto;
    private String perfilDescripcion;
    
    // Constructor adicional para facilitar la creación
    public UsuarioResumenDTO(Usuario usuario) {
        this.id = usuario.getId();
        this.uuid = usuario.getUuid();
        this.username = usuario.getUsername();
        this.nombre = usuario.getNombre();
        this.apellido = usuario.getApellido();
        this.email = usuario.getEmail();
        this.perfil = usuario.getPerfil();
        this.activo = usuario.getActivo();
        this.nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
        this.perfilDescripcion = usuario.getPerfil() != null ? usuario.getPerfil().getDescripcion() : "";
    }
    
    // Método para obtener el nombre completo
    public String getNombreCompleto() {
        if (nombreCompleto != null) {
            return nombreCompleto;
        }
        return (nombre != null ? nombre : "") + " " + (apellido != null ? apellido : "");
    }
    
    // Método para obtener las iniciales
    public String getIniciales() {
        String iniciales = "";
        if (nombre != null && !nombre.isEmpty()) {
            iniciales += nombre.charAt(0);
        }
        if (apellido != null && !apellido.isEmpty()) {
            iniciales += apellido.charAt(0);
        }
        return iniciales.toUpperCase();
    }
    
    // Método para obtener un identificador visual
    public String getIdentificador() {
        return username != null ? username : getIniciales();
    }
}