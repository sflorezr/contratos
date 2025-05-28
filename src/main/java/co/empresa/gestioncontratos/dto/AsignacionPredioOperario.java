package co.empresa.gestioncontratos.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class AsignacionPredioOperario {
    private UUID predioUuid;
    private UUID operarioUuid;
}