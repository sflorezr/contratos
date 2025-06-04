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

    private Boolean activo;

    private LocalDateTime fechaCreacion;

    // Campos adicionales para consultas con detalles
    private Integer totalTarifas;
    private Integer totalContratos;
    private Integer tarifasActivas;
    private Integer contratosActivos;

    // Métodos utilitarios
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

    public boolean esValido() {
        return nombre != null && !nombre.trim().isEmpty();
    }

    @Override
    public String toString() {
        return String.format("PlanTarifaDTO{uuid=%s, nombre='%s', activo=%s, totalTarifas=%d}",
                uuid, nombre, activo, totalTarifas != null ? totalTarifas : 0);
    }
}