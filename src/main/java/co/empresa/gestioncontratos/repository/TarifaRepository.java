package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.entity.Servicio;
import co.empresa.gestioncontratos.entity.Tarifa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TarifaRepository extends JpaRepository<Tarifa, Long> {
    
    List<Tarifa> findByServicioOrderByPrecioUrbanoAsc(Servicio servicio);
   // boolean existsByNombre(String nombre);    
    long countByServicio(Servicio servicio);

    // Métodos que necesitabas
    Optional<Tarifa> findByUuid(UUID uuid);
    
    @Query("SELECT t FROM Tarifa t WHERE t.planTarifa.uuid = :planTarifaUuid AND t.servicio.uuid = :servicioUuid")
    Optional<Tarifa> findByPlanTarifaUuidAndServicioUuid(
        @Param("planTarifaUuid") UUID planTarifaUuid, 
        @Param("servicioUuid") UUID servicioUuid
    );
    Page<Tarifa> findAll(Specification<Tarifa> spec, Pageable pageable);
    // Métodos para estadísticas
    long countByActivo(Boolean activo);
    
    @Query("SELECT pt.nombre, COUNT(t) FROM Tarifa t JOIN t.planTarifa pt GROUP BY pt.nombre ORDER BY COUNT(t) DESC")
    List<Object[]> contarTarifasPorPlan();
    
    @Query("SELECT AVG(t.precioUrbano) FROM Tarifa t WHERE t.activo = true")
    BigDecimal obtenerPromedioPrecioUrbano();
    
    @Query("SELECT AVG(t.precioRural) FROM Tarifa t WHERE t.activo = true")
    BigDecimal obtenerPromedioPrecioRural();
    
    @Query("SELECT MIN(t.precioUrbano) FROM Tarifa t WHERE t.activo = true")
    BigDecimal obtenerPrecioMinimoUrbano();
    
    @Query("SELECT MAX(t.precioUrbano) FROM Tarifa t WHERE t.activo = true")
    BigDecimal obtenerPrecioMaximoUrbano();
    
    @Query("SELECT MIN(t.precioRural) FROM Tarifa t WHERE t.activo = true")
    BigDecimal obtenerPrecioMinimoRural();
    
    @Query("SELECT MAX(t.precioRural) FROM Tarifa t WHERE t.activo = true")
    BigDecimal obtenerPrecioMaximoRural();
    
    // Consultas adicionales útiles
    @Query("SELECT t FROM Tarifa t WHERE t.planTarifa.uuid = :planTarifaUuid AND t.activo = true")
    List<Tarifa> findByPlanTarifaUuidAndActivoTrue(@Param("planTarifaUuid") UUID planTarifaUuid);
    
    @Query("SELECT t FROM Tarifa t WHERE t.servicio.uuid = :servicioUuid AND t.activo = true")
    List<Tarifa> findByServicioUuidAndActivoTrue(@Param("servicioUuid") UUID servicioUuid);
    
    @Query("SELECT COUNT(t) FROM Tarifa t WHERE t.planTarifa.uuid = :planTarifaUuid")
    long countByPlanTarifaUuid(@Param("planTarifaUuid") UUID planTarifaUuid);
}