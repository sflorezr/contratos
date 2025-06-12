package co.empresa.gestioncontratos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZonaDTO {
    
    private Long id;
    private UUID uuid;
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre;
    
    @NotBlank(message = "El código es obligatorio")
    @Size(min = 2, max = 50, message = "El código debe tener entre 2 y 50 caracteres")
    private String codigo;
    
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String descripcion;
    
    @Builder.Default
    private Boolean activo = true;
    
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    
    // ==================== CAMPOS ADICIONALES PARA VISTAS ====================
    
    // Información del contexto de contrato (cuando se consulta desde un contrato)
    private String coordinadorZona;
    private String coordinadorOperativo;
    private String planTarifa;
    private String estadoEnContrato;
    
    // Estadísticas de la zona
    private Map<String, Object> estadisticas;
    
    // Contadores para vistas resumidas
    private Long totalSectores;
    private Long sectoresActivos;
    private Long totalPredios;
    private Long prediosAsignados;
    private Long totalContratos;
    private Long contratosActivos;
    
    // ==================== MÉTODOS DE UTILIDAD ====================
    
    public boolean estaActiva() {
        return Boolean.TRUE.equals(activo);
    }
    
    public boolean tieneSectores() {
        return totalSectores != null && totalSectores > 0;
    }
    
    public boolean tieneContratos() {
        return totalContratos != null && totalContratos > 0;
    }
    
    public double getPorcentajeAsignacion() {
        if (totalPredios == null || totalPredios == 0) {
            return 0.0;
        }
        if (prediosAsignados == null) {
            return 0.0;
        }
        return (double) prediosAsignados / totalPredios * 100;
    }
    
    public String getEstadoTexto() {
        return estaActiva() ? "Activa" : "Inactiva";
    }
    
    public String getResumenCompleto() {
        StringBuilder resumen = new StringBuilder();
        resumen.append(codigo).append(" - ").append(nombre);
        
        if (totalSectores != null && totalSectores > 0) {
            resumen.append(" (").append(totalSectores).append(" sectores)");
        }
        
        if (totalContratos != null && totalContratos > 0) {
            resumen.append(" [").append(totalContratos).append(" contratos]");
        }
        
        return resumen.toString();
    }
    
    // Para validaciones en formularios
    public boolean puedeSerEditada() {
        return estaActiva();
    }
    
    public boolean puedeSerEliminada() {
        return !tieneContratos() && !tieneSectores();
    }
}