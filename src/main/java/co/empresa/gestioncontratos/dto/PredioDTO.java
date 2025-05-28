package co.empresa.gestioncontratos.dto;

import java.util.UUID;

import co.empresa.gestioncontratos.enums.TipoPredio;
import groovy.transform.builder.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredioDTO {
    private UUID uuid;
    private String codigoCatastral;
    private String direccion;
    private UUID sectorUuid;
    private String sectorNombre;
    private TipoPredio tipo;
    private Double area;
    private Double latitud;
    private Double longitud;
    private String observaciones;
    private Boolean activo;
}
