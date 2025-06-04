package co.empresa.gestioncontratos.dto;

import co.empresa.gestioncontratos.enums.EstadoContrato;
import lombok.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContratoDTO {
    
    private UUID uuid;
    
    @NotBlank(message = "El código del contrato es requerido")
    @Size(max = 50, message = "El código no puede exceder 50 caracteres")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "El código solo puede contener letras mayúsculas, números y guiones")
    private String codigo;
    
    @NotBlank(message = "El objetivo es requerido")
    @Size(max = 500, message = "El objetivo no puede exceder 500 caracteres")
    private String objetivo;
        
    @NotNull(message = "La fecha de inicio es requerida")
    private LocalDate fechaInicio;
    
    @NotNull(message = "La fecha de fin es requerida")
    private LocalDate fechaFin;
    
    @NotNull(message = "El plan de tarifa es requerido")
    private UUID planTarifaUuid;

    @NotNull(message = "La zona es requerida")
    private UUID zonaId;
    
    private UUID supervisorId;
    
    @Builder.Default
    private List<UUID> coordinadorUuids = new ArrayList<>();
    
    @Builder.Default
    private List<UUID> predioUuids = new ArrayList<>();
    
    private EstadoContrato estado;
    
    // Campos adicionales para vistas (no se persisten, solo para mostrar)
    private String planTarifaNombre;
    private String supervisorNombre;
    private Integer totalPredios;
    private Integer prediosAsignados;
    private Integer totalOperarios;
    private Double porcentajeAvance;
    private Integer cantidadCoordinadores;
    
    // NUEVOS CAMPOS para zonas
    private String zonaNombre;
    private Integer totalZonas;    
    private Integer totalSectores;
    private Integer sectoresActivos;
    

    // Lista de coordinadores para mostrar
    @Builder.Default
    private List<UsuarioResumenDTO> coordinadores = new ArrayList<>();
    
    // Lista de operarios para mostrar
    @Builder.Default
    private List<UsuarioResumenDTO> operarios = new ArrayList<>();
    
    // Validación personalizada
    @AssertTrue(message = "La fecha de fin debe ser posterior a la fecha de inicio")
    private boolean isFechaFinValida() {
        if (fechaInicio == null || fechaFin == null) {
            return true;
        }
        return fechaFin.isAfter(fechaInicio);
    }
    
    // Método para limpiar espacios
    public void trim() {
        if (codigo != null) {
            codigo = codigo.trim().toUpperCase();
        }
        if (objetivo != null) {
            objetivo = objetivo.trim();
        }
    }
    
    // Método para validar si el contrato está activo
    public boolean isActivo() {
        return estado == EstadoContrato.ACTIVO;
    }
    
    // Método para calcular días restantes
    public long getDiasRestantes() {
        if (fechaFin == null) {
            return 0;
        }
        LocalDate hoy = LocalDate.now();
        return java.time.temporal.ChronoUnit.DAYS.between(hoy, fechaFin);
    }
    private boolean puedeSerEliminado;

    public boolean isPuedeSerEliminado() {
        return puedeSerEliminado;
    }

    public void setPuedeSerEliminado(boolean puedeSerEliminado) {
        this.puedeSerEliminado = puedeSerEliminado;
    }
}