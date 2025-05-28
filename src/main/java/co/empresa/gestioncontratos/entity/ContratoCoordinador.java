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
@Table(name = "contrato_coordinadores", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"contrato_id", "coordinador_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContratoCoordinador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false)
    private Contrato contrato;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinador_id", nullable = false)
    private Usuario coordinador;

    @CreationTimestamp
    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private LocalDateTime fechaAsignacion;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    // Métodos de utilidad
    public String getNombreCoordinador() {
        return coordinador != null ? coordinador.getNombreCompleto() : "Sin coordinador";
    }

    public String getEmailCoordinador() {
        return coordinador != null ? coordinador.getEmail() : "Sin email";
    }

    public String getTelefonoCoordinador() {
        return coordinador != null ? coordinador.getTelefono() : "Sin teléfono";
    }

    public String getNumeroContrato() {
        return contrato != null ? contrato.getNumeroContrato() : "Sin contrato";
    }

    public String getObjetivoContrato() {
        return contrato != null ? contrato.getObjetivo() : "Sin objetivo";
    }

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
        return activo && contrato != null && contrato.estaVigente();
    }

    // Verificar si el contrato está activo
    public boolean contratoEstaActivo() {
        return contrato != null && contrato.estaActivo();
    }

    // Obtener el estado de la asignación
    public String getEstadoAsignacion() {
        if (!activo) return "Inactiva";
        if (contrato == null) return "Sin contrato";
        if (!contrato.estaActivo()) return "Contrato " + contrato.getEstado().getDescripcion();
        return "Activa";
    }

    // Obtener color para el estado
    public String getColorEstado() {
        if (!activo) return "secondary";
        if (contrato == null) return "danger";
        if (!contrato.estaActivo()) return "warning";
        return "success";
    }

    // Verificar si el coordinador tiene el perfil correcto
    public boolean coordinadorTienePerfilValido() {
        return coordinador != null && coordinador.esCoordinador();
    }

    @Override
    public String toString() {
        return "ContratoCoordinador{" +
                "contrato=" + getNumeroContrato() +
                ", coordinador=" + getNombreCoordinador() +
                ", activo=" + activo +
                ", fechaAsignacion=" + getFechaAsignacionCorta() +
                '}';
    }
}