package co.empresa.gestioncontratos.dto;

import lombok.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignacionMasivaDTO {
    private List<AsignacionPredioOperario> asignaciones;
    
    // getters y setters
    public List<AsignacionPredioOperario> getAsignaciones() {
        return asignaciones;
    }
    
    public void setAsignaciones(List<AsignacionPredioOperario> asignaciones) {
        this.asignaciones = asignaciones;
    }
}