package co.empresa.gestioncontratos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "contrato_predios", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"contrato_id", "predio_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContratoPredio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false)
    private Contrato contrato;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predio_id", nullable = false)
    private Predio predio;

    @CreationTimestamp
    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private LocalDateTime fechaAsignacion;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    // Métodos de utilidad para el contrato
    public String getNumeroContrato() {
        return contrato != null ? contrato.getNumeroContrato() : "Sin contrato";
    }

    public String getObjetivoContrato() {
        return contrato != null ? contrato.getObjetivo() : "Sin objetivo";
    }

    public String getSectorContrato() {
        return contrato != null ? contrato.getNombreSector() : "Sin sector";
    }

    // Métodos de utilidad para el predio
    public String getDireccionPredio() {
        return predio != null ? predio.getDireccion() : "Sin dirección";
    }

    public String getTipoPredio() {
        return predio != null ? predio.getTipoDisplay() : "Sin tipo";
    }

    public String getSectorPredio() {
        return predio != null ? predio.getNombreSector() : "Sin sector";
    }

    public String getColorTipoPredio() {
        return predio != null ? predio.getTipoColor() : "secondary";
    }

    // Métodos de fecha
    public String getFechaAsignacionFormateada() {
        if (fechaAsignacion == null) return "Sin fecha";
        return fechaAsignacion.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public String getFechaAsignacionCorta() {
        if (fechaAsignacion == null) return "Sin fecha";
        return fechaAsignacion.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // Métodos de validación
    public boolean puedeSerDesactivado() {
        return activo && contrato != null && contrato.estaActivo();
    }

    public boolean puedeSerReactivado() {
        return !activo && contrato != null && contrato.estaActivo();
    }

    // Método para desactivar la asignación
    public void desactivar() {
        this.activo = false;
    }

    // Método para reactivar la asignación
    public void reactivar() {
        this.activo = true;
    }

    // Verificar si la asignación está vigente
    public boolean estaVigente() {
        return activo && contrato != null && contrato.estaVigente() && 
               predio != null && predio.getActivo();
    }

    // Verificar compatibilidad de sectores
    public boolean sectoresCompatibles() {
        if (contrato == null || predio == null) return false;
        if (contrato.getSector() == null || predio.getSector() == null) return false;
        return contrato.getSector().getId().equals(predio.getSector().getId());
    }

    // Obtener el estado de la asignación
    public String getEstadoAsignacion() {
        if (!activo) return "Inactiva";
        if (contrato == null || predio == null) return "Datos incompletos";
        if (!contrato.estaActivo()) return "Contrato " + contrato.getEstado().getDescripcion();
        if (!predio.getActivo()) return "Predio inactivo";
        if (!sectoresCompatibles()) return "Sectores incompatibles";
        return "Activa";
    }

    // Obtener color para el estado
    public String getColorEstado() {
        if (!activo) return "secondary";
        if (contrato == null || predio == null) return "danger";
        if (!contrato.estaActivo() || !predio.getActivo()) return "warning";
        if (!sectoresCompatibles()) return "danger";
        return "success";
    }

    // Verificar si tiene operarios asignados
    public boolean tieneOperariosAsignados() {
        return predio != null && predio.tieneOperariosAsignados();
    }

    // Verificar si tiene actividades registradas
    public boolean tieneActividades() {
        return predio != null && predio.tieneActividades();
    }

    // Obtener cantidad de actividades
    public int getCantidadActividades() {
        return predio != null ? predio.getCantidadActividades() : 0;
    }

    // Obtener cantidad de actividades pendientes
    public long getCantidadActividadesPendientes() {
        return predio != null ? predio.getCantidadActividadesPendientes() : 0;
    }

    // Método para obtener información completa
    public String getInformacionCompleta() {
        return String.format("Contrato %s - Predio %s (%s)", 
                getNumeroContrato(), 
                getDireccionPredio(), 
                getTipoPredio());
    }

    // Verificar si puede ser eliminado
    public boolean puedeSerEliminado() {
        return !tieneOperariosAsignados() && !tieneActividades();
    }

    @Override
    public String toString() {
        return "ContratoPredio{" +
                "contrato=" + getNumeroContrato() +
                ", predio=" + getDireccionPredio() +
                ", activo=" + activo +
                ", fechaAsignacion=" + getFechaAsignacionCorta() +
                '}';
    }
}