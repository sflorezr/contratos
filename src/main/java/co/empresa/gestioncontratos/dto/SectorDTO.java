package co.empresa.gestioncontratos.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorDTO {
    
    private UUID uuid;
    
    @NotBlank(message = "El nombre del sector es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;
    
    @Size(max = 20, message = "El código no puede exceder 20 caracteres")
    @Pattern(regexp = "^[A-Z0-9-]*$", message = "El código solo puede contener letras mayúsculas, números y guiones")
    private String codigo;
    
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String descripcion;
    
    @Size(max = 200, message = "El límite norte no puede exceder 200 caracteres")
    private String limiteNorte;
    
    @Size(max = 200, message = "El límite sur no puede exceder 200 caracteres")
    private String limiteSur;
    
    @Size(max = 200, message = "El límite este no puede exceder 200 caracteres")
    private String limiteEste;
    
    @Size(max = 200, message = "El límite oeste no puede exceder 200 caracteres")
    private String limiteOeste;
    
    @PositiveOrZero(message = "El área debe ser mayor o igual a 0")
    private Double area;
    
    @PositiveOrZero(message = "La población debe ser mayor o igual a 0")
    private Integer poblacion;
    
    private Boolean activo = true;
    
    // Campos adicionales para vistas
    private Long totalPredios;
    private Long contratosActivos;
    private Double areaPredios;
    
    // Método para limpiar espacios
    public void trim() {
        if (nombre != null) nombre = nombre.trim();
        if (codigo != null) codigo = codigo.trim().toUpperCase();
        if (descripcion != null) descripcion = descripcion.trim();
        if (limiteNorte != null) limiteNorte = limiteNorte.trim();
        if (limiteSur != null) limiteSur = limiteSur.trim();
        if (limiteEste != null) limiteEste = limiteEste.trim();
        if (limiteOeste != null) limiteOeste = limiteOeste.trim();
    }
}
