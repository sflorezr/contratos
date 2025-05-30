package co.empresa.gestioncontratos.entity;

import co.empresa.gestioncontratos.enums.EstadoContrato;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "contratos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID uuid;

    @NotBlank
    @Column(name = "numero_contrato", unique = true, nullable = false, length = 50)
    private String numeroContrato;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_tarifa_id", nullable = false)
    private PlanTarifa planTarifa;

    @NotNull
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @NotNull
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String objetivo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoContrato estado = EstadoContrato.ACTIVO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private Usuario supervisor;

    @ManyToMany
    @JoinTable(
        name = "contrato_coordinador",
        joinColumns = @JoinColumn(name = "contrato_id"),
        inverseJoinColumns = @JoinColumn(name = "coordinador_id")
    )
    private Set<Usuario> coordinadores = new HashSet<>();
    
    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    // Relaciones
    @OneToMany(mappedBy = "contrato", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ContratoCoordinador> contratoCoordinadores;

    @OneToMany(mappedBy = "contrato", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ContratoPredio> contratoPredios;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        validarFechas();
    }

    @PreUpdate
    public void preUpdate() {
        validarFechas();
    }

    private void validarFechas() {
        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
    }

    // Métodos de utilidad
    public boolean estaActivo() {
        return EstadoContrato.ACTIVO.equals(estado);
    }

    public boolean estaFinalizado() {
        return EstadoContrato.FINALIZADO.equals(estado);
    }

    public boolean estaSuspendido() {
        return EstadoContrato.SUSPENDIDO.equals(estado);
    }

    public boolean estaVencido() {
        return LocalDate.now().isAfter(fechaFin);
    }
    public List<ContratoPredio> getPredios() {
        return contratoPredios;
    }
    public boolean estaVigente() {
        LocalDate hoy = LocalDate.now();
        return !hoy.isBefore(fechaInicio) && !hoy.isAfter(fechaFin) && estaActivo();
    }

    public boolean estaPorVencer() {
        if (estaVencido() || !estaActivo()) {
            return false;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), fechaFin) <= 30;
    }

    public long getDiasRestantes() {
        if (estaVencido()) {
            return 0;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), fechaFin);
    }

    public long getDuracionEnDias() {
        return ChronoUnit.DAYS.between(fechaInicio, fechaFin);
    }

    public double getPorcentajeTranscurrido() {
        LocalDate hoy = LocalDate.now();
        if (hoy.isBefore(fechaInicio)) {
            return 0.0;
        }
        if (hoy.isAfter(fechaFin)) {
            return 100.0;
        }
        
        long duracionTotal = getDuracionEnDias();
        long diasTranscurridos = ChronoUnit.DAYS.between(fechaInicio, hoy);
        
        return duracionTotal > 0 ? (double) diasTranscurridos / duracionTotal * 100 : 0.0;
    }

    public String getEstadoDisplay() {
        if (estaVencido() && estaActivo()) {
            return "VENCIDO";
        }
        return estado.getDescripcion();
    }

    public String getEstadoColor() {
        if (estaVencido() && estaActivo()) {
            return "danger";
        }
        return switch (estado) {
            case ACTIVO -> estaPorVencer() ? "warning" : "success";
            case FINALIZADO -> "secondary";
            case SUSPENDIDO -> "danger";
        };
    }

    public boolean tieneSupervisor() {
        return supervisor != null;
    }

    public boolean tieneCoordinadores() {
        return contratoCoordinadores != null && 
               contratoCoordinadores.stream().anyMatch(cc -> cc.getActivo());
    }

    public boolean tienePredios() {
        return contratoPredios != null && 
               contratoPredios.stream().anyMatch(cp -> cp.getActivo());
    }

    public int getCantidadCoordinadores() {
        if (contratoCoordinadores == null) {
            return 0;
        }
        return (int) contratoCoordinadores.stream()
                .filter(cc -> cc.getActivo())
                .count();
    }

    public int getCantidadPredios() {
        if (contratoPredios == null) {
            return 0;
        }
        return (int) contratoPredios.stream()
                .filter(cp -> cp.getActivo())
                .count();
    }

    // Método para obtener el nombre del sector
    public String getNombreSector() {
        return sector != null ? sector.getNombre() : "Sin sector";
    }

    // Método para obtener el nombre del plan de tarifa
    public String getNombrePlanTarifa() {
        return planTarifa != null ? planTarifa.getNombre() : "Sin plan";
    }

    // Método para obtener el nombre del supervisor
    public String getNombreSupervisor() {
        return supervisor != null ? supervisor.getNombreCompleto() : "Sin asignar";
    }

    // Método para verificar si puede ser editado
    public boolean puedeSerEditado() {
        return estaActivo() && !estaVencido();
    }

    // Método para verificar si puede ser eliminado
    public boolean puedeSerEliminado() {
        return !tieneCoordinadores() && !tienePredios();
    }

    @Override
    public String toString() {
        return "Contrato{" +
                "numeroContrato='" + numeroContrato + '\'' +
                ", estado=" + estado +
                ", fechaInicio=" + fechaInicio +
                ", fechaFin=" + fechaFin +
                '}';
    }
}