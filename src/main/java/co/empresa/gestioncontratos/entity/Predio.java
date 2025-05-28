package co.empresa.gestioncontratos.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import co.empresa.gestioncontratos.enums.TipoPredio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "predios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Predio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID uuid;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String direccion;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoPredio tipo;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    // Relaciones
    @OneToMany(mappedBy = "predio", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ContratoPredio> contratoPredios;

    @OneToMany(mappedBy = "predio", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<PredioOperario> predioOperarios;

    @OneToMany(mappedBy = "predio", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Actividad> actividades;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

    // Métodos de utilidad
    public boolean esUrbano() {
        return TipoPredio.URBANO.equals(tipo);
    }

    public boolean esRural() {
        return TipoPredio.RURAL.equals(tipo);
    }

    public String getTipoDisplay() {
        return tipo != null ? tipo.getDescripcion() : "Sin tipo";
    }

    public String getTipoColor() {
        if (tipo == null) return "secondary";
        return switch (tipo) {
            case URBANO -> "primary";
            case RURAL -> "success";
        };
    }

    public String getNombreSector() {
        return sector != null ? sector.getNombre() : "Sin sector";
    }

    public boolean tieneOperariosAsignados() {
        return predioOperarios != null && 
               predioOperarios.stream().anyMatch(po -> po.getActivo());
    }

    public boolean estaEnContratos() {
        return contratoPredios != null && 
               contratoPredios.stream().anyMatch(cp -> cp.getActivo());
    }

    public boolean tieneActividades() {
        return actividades != null && !actividades.isEmpty();
    }

    public int getCantidadOperarios() {
        if (predioOperarios == null) {
            return 0;
        }
        return (int) predioOperarios.stream()
                .filter(po -> po.getActivo())
                .count();
    }

    public int getCantidadContratos() {
        if (contratoPredios == null) {
            return 0;
        }
        return (int) contratoPredios.stream()
                .filter(cp -> cp.getActivo())
                .count();
    }

    public int getCantidadActividades() {
        return actividades != null ? actividades.size() : 0;
    }

    public long getCantidadActividadesPendientes() {
        if (actividades == null) {
            return 0;
        }
        return actividades.stream()
                .filter(a -> a.estaPendiente())
                .count();
    }

    // Método para verificar si puede ser editado
    public boolean puedeSerEditado() {
        return activo;
    }

    // Método para verificar si puede ser eliminado
    public boolean puedeSerEliminado() {
        return !tieneOperariosAsignados() && !estaEnContratos() && !tieneActividades();
    }

    // Método para obtener la dirección formateada
    public String getDireccionCompleta() {
        return direccion + " (" + getTipoDisplay() + " - " + getNombreSector() + ")";
    }

    @Override
    public String toString() {
        return "Predio{" +
                "direccion='" + direccion + '\'' +
                ", tipo=" + tipo +
                ", sector=" + (sector != null ? sector.getNombre() : "null") +
                ", activo=" + activo +
                '}';
    }
}