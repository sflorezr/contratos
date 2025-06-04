package co.empresa.gestioncontratos.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZonaDTO {
    private Long id;
    private UUID uuid;
    private String numeroContrato;
    private  Map<String, Object> estadisticas;
    
    // Estadísticas
    private Integer cantidadSectores;
    private Integer cantidadSectoresActivos;
    
    // Auditoría
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    
    // Lista de sectores (opcional, para vistas detalladas)
    private List<SectorDTO> sectores;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 50, message = "El código no puede exceder 50 caracteres")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "El código solo puede contener mayúsculas, números y guiones")
    private String codigo;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String descripcion;

    private Integer orden;
    
    @Builder.Default
    private Boolean activo = true;
}
