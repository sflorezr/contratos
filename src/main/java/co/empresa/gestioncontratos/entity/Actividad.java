package co.empresa.gestioncontratos.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import co.empresa.gestioncontratos.enums.EstadoActividad;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Entity
@Table(name = "actividades")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID uuid;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predio_id", nullable = false)
    private Predio predio;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operario_id", nullable = false)
    private Usuario operario;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_predio_id", nullable = false)
    private ContratoPredio contratoPredio;
    
    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @NotNull
    @Column(name = "fecha_actividad", nullable = false)
    private LocalDate fechaActividad;

    @Builder.Default
    @DecimalMin(value = "0.1")
    @Column(precision = 10, scale = 2)
    private BigDecimal cantidad = BigDecimal.ONE;

    @Column(name = "precio_aplicado", precision = 10, scale = 2)
    private BigDecimal precioAplicado;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoActividad estado = EstadoActividad.PENDIENTE;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

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
        calcularPrecioAplicado();
    }

    @PreUpdate  
    public void preUpdate() {
        calcularPrecioAplicado();
    }

    // Métodos de estado
    public boolean estaPendiente() {
        return EstadoActividad.PENDIENTE.equals(estado);
    }

    public boolean estaAprobada() {
        return EstadoActividad.APROBADA.equals(estado);
    }

    public boolean estaRechazada() {
        return EstadoActividad.RECHAZADA.equals(estado);
    }

    public String getEstadoDisplay() {
        return estado != null ? estado.getDescripcion() : "Sin estado";
    }

    public String getEstadoColor() {
        if (estado == null) return "secondary";
        return switch (estado) {
            case PENDIENTE -> "warning";
            case APROBADA -> "success";
            case RECHAZADA -> "danger";
        };
    }

    public String getEstadoIcon() {
        if (estado == null) return "fas fa-question";
        return switch (estado) {
            case PENDIENTE -> "fas fa-clock";
            case APROBADA -> "fas fa-check-circle";
            case RECHAZADA -> "fas fa-times-circle";
        };
    }

    // Métodos de cálculo
    public BigDecimal calcularTotal() {
        if (precioAplicado != null && cantidad != null) {
            return precioAplicado.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private void calcularPrecioAplicado() {
        if (servicio != null && predio != null && precioAplicado == null) {
            // Aquí se podría implementar la lógica para obtener el precio desde las tarifas
            // Por ahora asignamos un valor por defecto
            this.precioAplicado = BigDecimal.valueOf(50.00); // Precio por defecto
        }
    }

    // Métodos de información
    public String getNombreOperario() {
        return operario != null ? operario.getNombreCompleto() : "Sin operario";
    }

    public String getNombreServicio() {
        return servicio != null ? servicio.getNombre() : "Sin servicio";
    }

    public String getDireccionPredio() {
        return predio != null ? predio.getDireccion() : "Sin predio";
    }

    public String getTipoPredio() {
        return predio != null ? predio.getTipoDisplay() : "Sin tipo";
    }

    public String getSectorPredio() {
        return predio != null ? predio.getNombreSector() : "Sin sector";
    }

    // Métodos de fecha
    public String getFechaActividadFormateada() {
        if (fechaActividad == null) return "Sin fecha";
        return fechaActividad.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String getFechaCreacionFormateada() {
        if (fechaCreacion == null) return "Sin fecha";
        return fechaCreacion.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public boolean esFechaReciente() {
        if (fechaActividad == null) return false;
        return fechaActividad.isEqual(LocalDate.now()) || 
               fechaActividad.isAfter(LocalDate.now().minusDays(7));
    }

    public boolean esFechaFutura() {
        if (fechaActividad == null) return false;
        return fechaActividad.isAfter(LocalDate.now());
    }

    // Métodos de validación
    public boolean puedeSerEditada() {
        return estaPendiente();
    }

    public boolean puedeSerAprobada() {
        return estaPendiente();
    }

    public boolean puedeSerRechazada() {
        return estaPendiente();
    }

    public boolean puedeSerEliminada() {
        return estaPendiente() || estaRechazada();
    }

    // Método para aprobar actividad
    public void aprobar(String observacionesAprobacion) {
        this.estado = EstadoActividad.APROBADA;
        if (observacionesAprobacion != null && !observacionesAprobacion.trim().isEmpty()) {
            this.observaciones = (this.observaciones != null ? this.observaciones + "\n" : "") + 
                                "APROBADA: " + observacionesAprobacion;
        }
    }

    // Método para rechazar actividad
    public void rechazar(String motivoRechazo) {
        this.estado = EstadoActividad.RECHAZADA;
        if (motivoRechazo != null && !motivoRechazo.trim().isEmpty()) {
            this.observaciones = (this.observaciones != null ? this.observaciones + "\n" : "") + 
                                "RECHAZADA: " + motivoRechazo;
        }
    }

    // Método para resetear a pendiente
    public void volverAPendiente() {
        this.estado = EstadoActividad.PENDIENTE;
    }

    // Método para obtener el resumen de la actividad
    public String getResumen() {
        return String.format("%s - %s (%s) - %s", 
                getNombreServicio(),
                getDireccionPredio(), 
                getTipoPredio(),
                getFechaActividadFormateada());
    }

    @Override
    public String toString() {
        return "Actividad{" +
                "descripcion='" + descripcion + '\'' +
                ", fechaActividad=" + fechaActividad +
                ", estado=" + estado +
                ", operario=" + (operario != null ? operario.getUsername() : "null") +
                ", servicio=" + (servicio != null ? servicio.getNombre() : "null") +
                '}';
    }
}