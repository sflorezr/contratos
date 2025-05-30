package co.empresa.gestioncontratos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sectores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sector {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private UUID uuid;
    
    @Column(nullable = false, unique = true, length = 100)
    private String nombre;
    
    @Column(unique = true, length = 20)
    private String codigo;
    
    @Column(columnDefinition = "TEXT")
    private String descripcion;
    
    @Column(name = "limite_norte", length = 200)
    private String limiteNorte;
    
    @Column(name = "limite_sur", length = 200)
    private String limiteSur;
    
    @Column(name = "limite_este", length = 200)
    private String limiteEste;
    
    @Column(name = "limite_oeste", length = 200)
    private String limiteOeste;
    
    @Column
    private Double area;
    
    @Column
    private Integer poblacion;
    
    @Column(nullable = false)
    private Boolean activo = true;
    
    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
    
    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;
    
    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (activo == null) {
            activo = true;
        }
    }
}