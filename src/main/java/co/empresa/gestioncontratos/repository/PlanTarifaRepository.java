package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.entity.PlanTarifa;
import co.empresa.gestioncontratos.entity.Tarifa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanTarifaRepository extends JpaRepository<PlanTarifa, Long> {
    
    Optional<PlanTarifa> findByUuid(UUID uuid);
    
    Optional<PlanTarifa> findByNombre(String nombre);
     List<PlanTarifa> findByActivoTrueOrderByNombreAsc();
    Page<PlanTarifa> findAll(Specification<PlanTarifa> spec, Pageable pageable);
    long countByActivo(Boolean activo);
    
    @Query("SELECT COUNT(c) FROM Contrato c WHERE c.planTarifa.uuid = :planTarifaUuid AND c.estado = 'ACTIVO'")
    long countContratosActivosByPlanTarifa(@Param("planTarifaUuid") UUID planTarifaUuid);
    
    @Query("SELECT pt.nombre, COUNT(t) FROM PlanTarifa pt LEFT JOIN pt.tarifas t GROUP BY pt.nombre ORDER BY COUNT(t) DESC")
    List<Object[]> findPlanesConMasTarifas();
    
    @Query("SELECT DISTINCT pt FROM PlanTarifa pt WHERE EXISTS (SELECT 1 FROM Tarifa t WHERE t.planTarifa = pt)")
    List<PlanTarifa> findPlanesConTarifas();
    
    @Query("SELECT pt FROM PlanTarifa pt WHERE NOT EXISTS (SELECT 1 FROM Tarifa t WHERE t.planTarifa = pt)")
    List<PlanTarifa> findPlanesSinTarifas();
    
    @Query("SELECT pt FROM PlanTarifa pt WHERE pt.activo = true AND EXISTS (SELECT 1 FROM Tarifa t WHERE t.planTarifa = pt AND t.activo = true)")
    List<PlanTarifa> findPlanesActivosConTarifasActivas();        
}