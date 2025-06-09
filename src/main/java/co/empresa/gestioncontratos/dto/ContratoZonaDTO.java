package co.empresa.gestioncontratos.dto;

import co.empresa.gestioncontratos.enums.EstadoContratoZona;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContratoZonaDTO {

    private UUID uuid;

    @NotNull(message = "El contrato es requerido")
    private UUID contratoUuid;

    @NotNull(message = "La zona es requerida")
    private UUID zonaUuid;

    @NotNull(message = "El plan de tarifa es requerido")
    private UUID planTarifaUuid;

    private UUID coordinadorZonaUuid;

    private UUID coordinadorOperativoUuid;

    @Builder.Default
    private EstadoContratoZona estado = EstadoContratoZona.ACTIVO;

    @Builder.Default
    private Boolean activo = true;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    // Campos adicionales para vistas (no se persisten, solo para mostrar)
    private String contratoNumero;
    private String zonaNombre;
    private String zonaDescripcion;
    private String planTarifaNombre;
    private String coordinadorZonaNombre;
    private String coordinadorOperativoNombre;

    // Estadísticas específicas de la zona en el contrato
    private Integer totalPredios;
    private Integer prediosAsignados;
    private Integer prediosCompletados;
    private Integer totalOperarios;
    private Double porcentajeAvance;

    // Métodos de utilidad
    public boolean estaActivo() {
        return Boolean.TRUE.equals(activo) && estado == EstadoContratoZona.ACTIVO;
    }

    public boolean tieneCoordinadorZona() {
        return coordinadorZonaUuid != null;
    }

    public boolean tieneCoordinadorOperativo() {
        return coordinadorOperativoUuid != null;
    }

    public boolean esCompletado() {
        return estado == EstadoContratoZona.COMPLETADO;
    }

    public String getEstadoDescripcion() {
        return estado != null ? estado.getDescripcion() : "Sin estado";
    }

    // Validación personalizada
    public boolean esValido() {
        return contratoUuid != null && zonaUuid != null && planTarifaUuid != null;
    }

    @Override
    public String toString() {
        return String.format("ContratoZonaDTO{uuid=%s, contrato=%s, zona=%s, estado=%s}",
                uuid, contratoNumero, zonaNombre, estado);
    }
}