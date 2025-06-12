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
@Table(name = "planes_tarifas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanTarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID uuid;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // Relación con Tarifas (sin cambios)
    @OneToMany(mappedBy = "planTarifa", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Tarifa> tarifas;

    // NUEVA RELACIÓN: Un PlanTarifa puede estar en múltiples ContratoZonas
    @OneToMany(mappedBy = "planTarifa", fetch = FetchType.LAZY)
    private List<ContratoZona> contratosZonas;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

    // ==================== MÉTODOS DE UTILIDAD ====================

    public boolean estaActivo() {
        return Boolean.TRUE.equals(activo);
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    public boolean tieneTarifas() {
        return tarifas != null && !tarifas.isEmpty();
    }

    public boolean tieneContratosZonas() {
        return contratosZonas != null && !contratosZonas.isEmpty();
    }

    public int getCantidadTarifas() {
        return tarifas != null ? tarifas.size() : 0;
    }

    public int getCantidadContratosZonas() {
        return contratosZonas != null ? contratosZonas.size() : 0;
    }

    public long getCantidadTarifasActivas() {
        return tarifas != null ? 
            tarifas.stream().filter(t -> Boolean.TRUE.equals(t.getActivo())).count() : 0;
    }

    public long getCantidadContratosZonasActivas() {
        return contratosZonas != null ? 
            contratosZonas.stream()
                .filter(cz -> Boolean.TRUE.equals(cz.getActivo()) && 
                             cz.getContrato() != null && 
                             cz.getContrato().getEstado() != null)
                .count() : 0;
    }

    public boolean puedeSerEliminado() {
        return !tieneTarifas() && !tieneContratosZonas();
    }

    public boolean puedeSerDesactivado() {
        // Puede ser desactivado si no tiene contratos activos
        return contratosZonas == null || 
               contratosZonas.stream()
                   .noneMatch(cz -> Boolean.TRUE.equals(cz.getActivo()) && 
                                   cz.getContrato() != null && 
                                   "ACTIVO".equals(cz.getContrato().getEstado().toString()));
    }

    @Override
    public String toString() {
        return String.format("PlanTarifa{id=%d, uuid=%s, nombre='%s', activo=%s}", 
            id, uuid, nombre, activo);
    }
}