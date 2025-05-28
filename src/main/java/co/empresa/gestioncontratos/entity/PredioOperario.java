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
@Table(name = "predio_operarios", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"predio_id", "operario_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredioOperario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predio_id", nullable = false)
    private Predio predio;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operario_id", nullable = false)
    private Usuario operario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinador_id")
    private Usuario coordinador;

    @CreationTimestamp
    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private LocalDateTime fechaAsignacion;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

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

    // Métodos de utilidad para el operario
    public String getNombreOperario() {
        return operario != null ? operario.getNombreCompleto() : "Sin operario";
    }

    public String getEmailOperario() {
        return operario != null ? operario.getEmail() : "Sin email";
    }

    public String getTelefonoOperario() {
        return operario != null ? operario.getTelefono() : "Sin teléfono";
    }

    public String getUsernameOperario() {
        return operario != null ? operario.getUsername() : "Sin usuario";
    }

    // Métodos de utilidad para el coordinador
    public String getNombreCoordinador() {
        return coordinador != null ? coordinador.getNombreCompleto() : "Sin coordinador";
    }

    public String getEmailCoordinador() {
        return coordinador != null ? coordinador.getEmail() : "Sin email";
    }

    public boolean tieneCoordinador() {
        return coordinador != null;
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
        return activo && predio != null && predio.getActivo() && 
               operario != null && operario.getActivo();
    }

    public boolean puedeSerReactivado() {
        return !activo && predio != null && predio.getActivo() && 
               operario != null && operario.getActivo();
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
        return activo && predio != null && predio.getActivo() && 
               operario != null && operario.getActivo();
    }

    // Verificar si el operario tiene el perfil correcto
    public boolean operarioTienePerfilValido() {
        return operario != null && operario.esOperario();
    }

    // Verificar si el coordinador tiene el perfil correcto
    public boolean coordinadorTienePerfilValido() {
        return coordinador == null || coordinador.esCoordinador();
    }

    // Obtener el estado de la asignación
    public String getEstadoAsignacion() {
        if (!activo) return "Inactiva";
        if (predio == null || operario == null) return "Datos incompletos";
        if (!predio.getActivo()) return "Predio inactivo";
        if (!operario.getActivo()) return "Operario inactivo";
        if (!operarioTienePerfilValido()) return "Perfil inválido";
        if (!coordinadorTienePerfilValido()) return "Coordinador inválido";
        return "Activa";
    }

    // Obtener color para el estado
    public String getColorEstado() {
        if (!activo) return "secondary";
        if (predio == null || operario == null) return "danger";
        if (!predio.getActivo() || !operario.getActivo()) return "warning";
        if (!operarioTienePerfilValido() || !coordinadorTienePerfilValido()) return "danger";
        return "success";
    }

    // Verificar si el operario puede trabajar en este predio (mismo sector)
    public boolean sectoresCompatibles() {
        // Por ahora permitimos que cualquier operario trabaje en cualquier predio
        // Esta lógica se puede modificar según las reglas de negocio
        return true;
    }

    // Contar actividades del operario en este predio
    public long getCantidadActividades() {
        if (predio == null || operario == null) return 0;
        return predio.getActividades() != null ? 
               predio.getActividades().stream()
                     .filter(a -> a.getOperario().getId().equals(operario.getId()))
                     .count() : 0;
    }

    // Contar actividades pendientes del operario en este predio
    public long getCantidadActividadesPendientes() {
        if (predio == null || operario == null) return 0;
        return predio.getActividades() != null ? 
               predio.getActividades().stream()
                     .filter(a -> a.getOperario().getId().equals(operario.getId()) && a.estaPendiente())
                     .count() : 0;
    }

    // Verificar si tiene actividades registradas
    public boolean tieneActividades() {
        return getCantidadActividades() > 0;
    }

    // Método para cambiar coordinador
    public void asignarCoordinador(Usuario nuevoCoordinador) {
        if (nuevoCoordinador != null && nuevoCoordinador.esCoordinador()) {
            this.coordinador = nuevoCoordinador;
        }
    }

    // Método para remover coordinador
    public void removerCoordinador() {
        this.coordinador = null;
    }

    // Método para obtener información completa
    public String getInformacionCompleta() {
        return String.format("Operario %s - Predio %s (%s)", 
                getNombreOperario(), 
                getDireccionPredio(), 
                getTipoPredio());
    }

    // Verificar si puede ser eliminado
    public boolean puedeSerEliminado() {
        return !tieneActividades();
    }

    @Override
    public String toString() {
        return "PredioOperario{" +
                "predio=" + getDireccionPredio() +
                ", operario=" + getNombreOperario() +
                ", coordinador=" + getNombreCoordinador() +
                ", activo=" + activo +
                ", fechaAsignacion=" + getFechaAsignacionCorta() +
                '}';
    }
}