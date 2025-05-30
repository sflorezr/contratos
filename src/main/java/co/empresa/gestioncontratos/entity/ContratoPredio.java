package co.empresa.gestioncontratos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import co.empresa.gestioncontratos.enums.EstadoPredio;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column(unique = true, nullable = false)
    private UUID uuid;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operario_id")
    private Usuario operario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false)
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predio_id", nullable = false)
    private Predio predio;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoPredio estado = EstadoPredio.PENDIENTE;

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

    public Usuario getOperario() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getOperario'");
    }
}