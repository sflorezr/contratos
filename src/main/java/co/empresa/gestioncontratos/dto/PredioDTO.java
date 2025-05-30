package co.empresa.gestioncontratos.dto;

import co.empresa.gestioncontratos.enums.TipoPredio;
import lombok.*;
import jakarta.validation.constraints.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredioDTO {
    
    private UUID uuid;
    
    @NotBlank(message = "La dirección es requerida")
    @Size(max = 255, message = "La dirección no puede exceder 255 caracteres")
    private String direccion;
    
    @NotNull(message = "El sector es requerido")
    private UUID sectorUuid;
    
    @NotNull(message = "El tipo de predio es requerido")
    private TipoPredio tipo;
    
    @Builder.Default
    private Boolean activo = true;
    
    // Campos adicionales para vistas (no se persisten, solo para mostrar)
    private String sectorNombre;
    private String contratoActual;
    private UUID contratoUuid;
    private String operarioAsignado;
    private UUID operarioUuid;
    private String estadoAsignacion;
    private Integer totalActividades;
    private Integer actividadesPendientes;
    
    // Método para limpiar espacios
    public void trim() {
        if (direccion != null) {
            direccion = direccion.trim();
        }
    }
    
    // Método para obtener dirección completa
    public String getDireccionCompleta() {
        if (sectorNombre != null) {
            return direccion + " - " + sectorNombre;
        }
        return direccion;
    }
    
    // Método para obtener tipo como texto
    public String getTipoTexto() {
        return tipo != null ? tipo.toString() : "";
    }
}