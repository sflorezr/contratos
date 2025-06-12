package co.empresa.gestioncontratos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanTarifaDTO {

    private UUID uuid;

    @NotBlank(message = "El nombre del plan de tarifa es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
    private String descripcion;

    @Builder.Default
    private Boolean activo = true;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    // ==================== ESTADÍSTICAS DE TARIFAS ====================
    
    private Integer totalTarifas;
    private Integer tarifasActivas;
    private Integer tarifasInactivas;

    // ==================== ESTADÍSTICAS DE CONTRATOS-ZONAS ====================
    
    /**
     * Total de zonas de contratos que usan este plan
     */
    private Integer totalContratos; // Renombrado para mantener compatibilidad
    
    /**
     * Zonas de contratos activas que usan este plan
     */
    private Integer contratosActivos; // Renombrado para mantener compatibilidad
    
    /**
     * Total de zonas específicas que usan este plan
     */
    private Integer totalZonasContratos;
    
    /**
     * Zonas activas que usan este plan
     */
    private Integer zonasActivasContratos;
    
    /**
     * Contratos únicos que tienen al menos una zona con este plan
     */
    private Integer contratosUnicos;
    
    /**
     * Contratos únicos activos que usan este plan
     */
    private Integer contratosUnicosActivos;

    // ==================== INDICADORES DE ESTADO ====================
    
    private Boolean puedeSerEliminado;
    private Boolean puedeSerDesactivado;
    private Boolean tieneUsoActivo;

    // ==================== MÉTRICAS DE RENDIMIENTO ====================
    
    /**
     * Ratio de uso: zonas activas / total tarifas
     */
    private Double ratioUsoTarifas;
    
    /**
     * Promedio de zonas por contrato
     */
    private Double promedioZonasPorContrato;

    // ==================== MÉTODOS DE UTILIDAD ====================

    public boolean esActivo() {
        return activo != null && activo;
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    public boolean tieneTarifas() {
        return totalTarifas != null && totalTarifas > 0;
    }

    public boolean tieneContratos() {
        return totalContratos != null && totalContratos > 0;
    }

    public boolean tieneContratosActivos() {
        return contratosActivos != null && contratosActivos > 0;
    }

    public boolean esValido() {
        return nombre != null && !nombre.trim().isEmpty();
    }

    // ==================== MÉTODOS DE CÁLCULO ====================

    public int getTarifasInactivas() {
        if (totalTarifas == null || tarifasActivas == null) {
            return 0;
        }
        return totalTarifas - tarifasActivas;
    }

    public double getPorcentajeTarifasActivas() {
        if (totalTarifas == null || totalTarifas == 0) {
            return 0.0;
        }
        return (tarifasActivas != null ? tarifasActivas : 0) * 100.0 / totalTarifas;
    }

    public double getPorcentajeContratosActivos() {
        if (totalContratos == null || totalContratos == 0) {
            return 0.0;
        }
        return (contratosActivos != null ? contratosActivos : 0) * 100.0 / totalContratos;
    }

    public double getRatioUsoCalculado() {
        if (totalTarifas == null || totalTarifas == 0) {
            return 0.0;
        }
        int zonasActivas = zonasActivasContratos != null ? zonasActivasContratos : 0;
        return zonasActivas * 1.0 / totalTarifas;
    }

    public String getEstadoTexto() {
        return esActivo() ? "Activo" : "Inactivo";
    }

    public String getNivelUso() {
        if (!tieneContratos()) {
            return "Sin uso";
        }
        
        double porcentajeUso = getPorcentajeContratosActivos();
        if (porcentajeUso >= 80) {
            return "Alto uso";
        } else if (porcentajeUso >= 40) {
            return "Uso moderado";
        } else if (porcentajeUso > 0) {
            return "Bajo uso";
        } else {
            return "Sin uso activo";
        }
    }

    // ==================== VALIDACIONES DE NEGOCIO ====================

    public boolean puedeSerEliminadoSeguro() {
        return (puedeSerEliminado != null && puedeSerEliminado) ||
               (!tieneTarifas() && !tieneContratos());
    }

    public boolean puedeSerDesactivadoSeguro() {
        return (puedeSerDesactivado != null && puedeSerDesactivado) ||
               !tieneContratosActivos();
    }

    public boolean necesitaAtencion() {
        return esActivo() && tieneTarifas() && !tieneContratosActivos();
    }

    // ==================== FORMATEO PARA VISTAS ====================

    public String getResumenUso() {
        if (!tieneContratos()) {
            return "Sin uso";
        }
        
        StringBuilder resumen = new StringBuilder();
        if (totalZonasContratos != null && totalZonasContratos > 0) {
            resumen.append(totalZonasContratos).append(" zonas");
            if (contratosUnicos != null && contratosUnicos > 0) {
                resumen.append(" en ").append(contratosUnicos).append(" contratos");
            }
        } else {
            resumen.append("Sin zonas asignadas");
        }
        
        return resumen.toString();
    }

    public String getResumenTarifas() {
        if (!tieneTarifas()) {
            return "Sin tarifas";
        }
        
        StringBuilder resumen = new StringBuilder();
        resumen.append(totalTarifas).append(" tarifas");
        if (tarifasActivas != null) {
            resumen.append(" (").append(tarifasActivas).append(" activas)");
        }
        
        return resumen.toString();
    }

    // ==================== MÉTODOS LEGACY (Para compatibilidad) ====================

    /**
     * @deprecated Usar totalZonasContratos
     */
    @Deprecated
    public Integer getTotalContratosLegacy() {
        return totalZonasContratos;
    }

    /**
     * @deprecated Usar zonasActivasContratos
     */
    @Deprecated
    public Integer getContratosActivosLegacy() {
        return zonasActivasContratos;
    }

    @Override
    public String toString() {
        return String.format("PlanTarifaDTO{uuid=%s, nombre='%s', activo=%s, totalTarifas=%d, totalContratos=%d}",
                uuid, nombre, activo, 
                totalTarifas != null ? totalTarifas : 0,
                totalContratos != null ? totalContratos : 0);
    }
}