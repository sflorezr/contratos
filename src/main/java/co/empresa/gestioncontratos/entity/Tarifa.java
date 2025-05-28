package co.empresa.gestioncontratos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tarifas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID uuid;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_tarifa_id", nullable = false)
    private PlanTarifa planTarifa;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "precio_urbano", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUrbano;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "precio_rural", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioRural;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

    public BigDecimal getPrecioPorTipo(String tipoPredio) {
        return "URBANO".equals(tipoPredio) ? precioUrbano : precioRural;
    }
}