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

    private UUID supervisorUuid;
    
    private EstadoContrato estado;
    
    // NUEVA ESTRUCTURA: Lista de zonas con sus respectivos planes de tarifa y coordinadores
    @Builder.Default
    private List<ContratoZonaDTO> zonas = new ArrayList<>();
    
    @Builder.Default
    private List<UUID> predioUuids = new ArrayList<>();
    
    // Campos adicionales para vistas (no se persisten, solo para mostrar)
    private String supervisorNombre;
    private Integer totalPredios;
    private Integer prediosAsignados;
    private Integer totalOperarios;
    private Double porcentajeAvance;
    
    // NUEVOS CAMPOS para múltiples zonas
    private Integer totalZonas;
    private Integer zonasActivas;
    private Integer totalCoordinadoresZona;
    private Integer totalCoordinadoresOperativos;
    
    // Lista de operarios para mostrar
    @Builder.Default
    private List<UsuarioResumenDTO> operarios = new ArrayList<>();
    
    // ELIMINADOS:
    // - zonaUuid (ahora está en la lista de zonas)
    // - planTarifaUuid (ahora cada zona tiene su plan)
    // - coordinadorUuids (ahora cada zona tiene sus coordinadores)
    // - coordinadores (ahora cada zona maneja sus coordinadores)
    // - zonaNombre, planTarifaNombre, etc. (se obtienen de las zonas)
    
    // Validación personalizada
    @AssertTrue(message = "La fecha de fin debe ser posterior a la fecha de inicio")
    private boolean isFechaFinValida() {
        if (fechaInicio == null || fechaFin == null) {
            return true;
        }
        return fechaFin.isAfter(fechaInicio);
    }
    
    @AssertTrue(message = "El contrato debe tener al menos una zona")
    private boolean tieneZonas() {
        return zonas != null && !zonas.isEmpty();
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
    
    // Métodos de utilidad para zonas
    public void agregarZona(ContratoZonaDTO zona) {
        if (zonas == null) {
            zonas = new ArrayList<>();
        }
        zonas.add(zona);
    }
    
    public void removerZona(UUID zonaUuid) {
        if (zonas != null) {
            zonas.removeIf(zona -> zona.getZonaUuid().equals(zonaUuid));
        }
    }
    
    public ContratoZonaDTO buscarZona(UUID zonaUuid) {
        if (zonas == null) {
            return null;
        }
        return zonas.stream()
                .filter(zona -> zona.getZonaUuid().equals(zonaUuid))
                .findFirst()
                .orElse(null);
    }
    
    public boolean tieneZona(UUID zonaUuid) {
        return buscarZona(zonaUuid) != null;
    }
    
    public int getCantidadZonasActivas() {
        if (zonas == null) {
            return 0;
        }
        return (int) zonas.stream()
                .filter(ContratoZonaDTO::estaActivo)
                .count();
    }
    
    private boolean puedeSerEliminado;

    public boolean isPuedeSerEliminado() {
        return puedeSerEliminado;
    }

    public void setPuedeSerEliminado(boolean puedeSerEliminado) {
        this.puedeSerEliminado = puedeSerEliminado;
    }
}