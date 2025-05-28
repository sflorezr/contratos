package co.empresa.gestioncontratos.dto;

import co.empresa.gestioncontratos.enums.PerfilUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {

    private UUID uuid;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
    private String username;

    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    private String confirmPassword;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
    private String apellido;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    private String email;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String telefono;

    @NotNull(message = "El perfil es obligatorio")
    private PerfilUsuario perfil;

    @Builder.Default
    private Boolean activo = true;

    // ==================== MÉTODOS DE UTILIDAD ====================

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    public String getIniciales() {
        if (nombre != null && apellido != null && !nombre.isEmpty() && !apellido.isEmpty()) {
            return nombre.substring(0, 1).toUpperCase() + apellido.substring(0, 1).toUpperCase();
        }
        return "??";
    }

    // Validación personalizada para confirmar contraseña
    public boolean isPasswordsMatch() {
        if (password == null || confirmPassword == null) {
            return false;
        }
        return password.equals(confirmPassword);
    }

    // Método para limpiar espacios
    public void trim() {
        if (username != null) username = username.trim();
        if (nombre != null) nombre = nombre.trim();
        if (apellido != null) apellido = apellido.trim();
        if (email != null) email = email.trim().toLowerCase();
        if (telefono != null) telefono = telefono.trim();
    }

    // Método para verificar si es nuevo usuario
    public boolean esNuevo() {
        return uuid == null;
    }

    // Método para obtener descripción del perfil
    public String getDescripcionPerfil() {
        return perfil != null ? perfil.getDescripcion() : "Sin perfil";
    }

    // Validación de datos completa
    public boolean esValido() {
        return username != null && !username.trim().isEmpty() &&
               nombre != null && !nombre.trim().isEmpty() &&
               apellido != null && !apellido.trim().isEmpty() &&
               email != null && !email.trim().isEmpty() &&
               perfil != null;
    }

    // Método para obtener estado como texto
    public String getEstadoTexto() {
        return Boolean.TRUE.equals(activo) ? "Activo" : "Inactivo";
    }

    @Override
    public String toString() {
        return "UsuarioDTO{" +
                "uuid=" + uuid +
                ", username='" + username + '\'' +
                ", nombre='" + nombre + '\'' +
                ", apellido='" + apellido + '\'' +
                ", email='" + email + '\'' +
                ", telefono='" + telefono + '\'' +
                ", perfil=" + perfil +
                ", activo=" + activo +
                '}';
    }
}