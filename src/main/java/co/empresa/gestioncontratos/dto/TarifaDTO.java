package co.empresa.gestioncontratos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TarifaDTO {

    private UUID uuid;

    @NotNull(message = "El UUID del plan de tarifa es obligatorio")
    private UUID planTarifaUuid;

    @NotNull(message = "El UUID del servicio es obligatorio")
    private UUID servicioUuid;

    @NotNull(message = "El precio urbano es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio urbano debe ser mayor a cero")
    private BigDecimal precioUrbano;

    @NotNull(message = "El precio rural es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio rural debe ser mayor a cero")
    private BigDecimal precioRural;

    private Boolean activo;

    private LocalDateTime fechaCreacion;

    // Campos adicionales para consultas con detalles
    private String planTarifaNombre;
    private String servicioNombre;
    private String servicioDescripcion;
    private String planTarifaDescripcion;

    // Constructores de conveniencia
    public TarifaDTO(UUID planTarifaUuid, UUID servicioUuid, 
                     BigDecimal precioUrbano, BigDecimal precioRural) {
        this.planTarifaUuid = planTarifaUuid;
        this.servicioUuid = servicioUuid;
        this.precioUrbano = precioUrbano;
        this.precioRural = precioRural;
        this.activo = true;
    }

    // Métodos utilitarios
    public BigDecimal getPrecioPorTipo(String tipoPredio) {
        return "URBANO".equals(tipoPredio) ? precioUrbano : precioRural;
    }

    public boolean esActivo() {
        return activo != null && activo;
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    // Validación personalizada
    public boolean tienePreciosValidos() {
        return precioUrbano != null && precioUrbano.compareTo(BigDecimal.ZERO) > 0 &&
               precioRural != null && precioRural.compareTo(BigDecimal.ZERO) > 0;
    }
    public boolean tieneReferencesCompletas() {
        return planTarifaUuid != null && servicioUuid != null;
    }

    @Override
    public String toString() {
        return String.format("TarifaDTO{uuid=%s, planTarifa=%s, servicio=%s, precioUrbano=%s, precioRural=%s, activo=%s}",
                uuid, planTarifaUuid, servicioUuid, precioUrbano, precioRural, activo);
    }
}