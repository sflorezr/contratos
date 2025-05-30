package co.empresa.gestioncontratos.dto;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignacionPredioOperario {
    private UUID predioUuid;
    private UUID operarioUuid;
    
    // getters y setters
    public UUID getPredioUuid() {
        return predioUuid;
    }
    
    public void setPredioUuid(UUID predioUuid) {
        this.predioUuid = predioUuid;
    }
    
    public UUID getOperarioUuid() {
        return operarioUuid;
    }
    
    public void setOperarioUuid(UUID operarioUuid) {
        this.operarioUuid = operarioUuid;
    }
}