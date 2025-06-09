package co.empresa.gestioncontratos.entity;

import co.empresa.gestioncontratos.enums.EstadoContratoZona;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contrato_zonas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContratoZona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID uuid;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false)
    private Contrato contrato;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_id", nullable = false)
    private Zona zona;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_tarifa_id", nullable = false)
    private PlanTarifa planTarifa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinador_zona_id")
    private Usuario coordinadorZona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinador_operativo_id")
    private Usuario coordinadorOperativo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoContratoZona estado = EstadoContratoZona.ACTIVO;

    @Builder.Default
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
    }

    // Métodos de utilidad
    public boolean estaActivo() {
        return Boolean.TRUE.equals(activo) && estado == EstadoContratoZona.ACTIVO;
    }

    public boolean tieneCoordinadorZona() {
        return coordinadorZona != null;
    }

    public boolean tieneCoordinadorOperativo() {
        return coordinadorOperativo != null;
    }

    public String getNombreZona() {
        return zona != null ? zona.getNombre() : "Sin zona";
    }

    public String getNombrePlanTarifa() {
        return planTarifa != null ? planTarifa.getNombre() : "Sin plan";
    }

    public String getNombreCoordinadorZona() {
        return coordinadorZona != null ? coordinadorZona.getNombreCompleto() : "Sin asignar";
    }

    public String getNombreCoordinadorOperativo() {
        return coordinadorOperativo != null ? coordinadorOperativo.getNombreCompleto() : "Sin asignar";
    }

    @Override
    public String toString() {
        return "ContratoZona{" +
                "uuid=" + uuid +
                ", contrato=" + (contrato != null ? contrato.getNumeroContrato() : "null") +
                ", zona=" + (zona != null ? zona.getNombre() : "null") +
                ", estado=" + estado +
                '}';
    }
}