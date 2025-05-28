package co.empresa.gestioncontratos.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import co.empresa.gestioncontratos.enums.EstadoContrato;
import groovy.transform.builder.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContratoDTO {
    private UUID uuid;
    private String codigo;
    private String objetivo;
    private UUID sectorUuid;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private UUID planTarifaUuid;
    private UUID supervisorUuid;
    private List<UUID> coordinadorUuids;
    private List<UUID> predioUuids;
    private EstadoContrato estado;
}
