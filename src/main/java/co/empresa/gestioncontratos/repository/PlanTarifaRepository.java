package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.dto.PlanTarifaDTO;
import co.empresa.gestioncontratos.entity.PlanTarifa;
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
    
    // ==================== CONSULTAS BÁSICAS ====================
    
    Optional<PlanTarifa> findByUuid(UUID uuid);
    
    Optional<PlanTarifa> findByNombre(String nombre);
    
    List<PlanTarifa> findByActivoTrueOrderByNombreAsc();
    
    Page<PlanTarifa> findAll(Specification<PlanTarifa> spec, Pageable pageable);
    
    long countByActivo(Boolean activo);
    
    // ==================== CONSULTAS RELACIONADAS CON CONTRATO-ZONA ====================
    
    /**
     * Cuenta contratos activos que usan este plan de tarifa a través de ContratoZona
     */
    @Query("""
        SELECT COUNT(DISTINCT cz.contrato) 
        FROM ContratoZona cz 
        WHERE cz.planTarifa.uuid = :planTarifaUuid 
        AND cz.activo = true 
        AND cz.contrato.estado = 'ACTIVO'
        """)
    long countContratosActivosByPlanTarifa(@Param("planTarifaUuid") UUID planTarifaUuid);


    @Query("""
    SELECT new co.empresa.gestioncontratos.dto.PlanTarifaDTO(
        p.uuid,
        p.nombre, 
        p.descripcion,
        p.activo,
        p.fechaCreacion,
        p.fechaActualizacion
    )
    FROM PlanTarifa p
    """)
    List<PlanTarifaDTO> findByAllDTO();
    /**
     * Cuenta todas las zonas de contratos que usan este plan de tarifa
     */
    @Query("""
        SELECT COUNT(cz) 
        FROM ContratoZona cz 
        WHERE cz.planTarifa.uuid = :planTarifaUuid 
        AND cz.activo = true
        """)
    long countZonasActivasByPlanTarifa(@Param("planTarifaUuid") UUID planTarifaUuid);
    
    /**
     * Encuentra planes con más tarifas asociadas
     */
    @Query("""
        SELECT pt.nombre, COUNT(t) 
        FROM PlanTarifa pt 
        LEFT JOIN pt.tarifas t 
        GROUP BY pt.id, pt.nombre 
        ORDER BY COUNT(t) DESC
        """)
    List<Object[]> findPlanesConMasTarifas();
    
    /**
     * Encuentra planes con más contratos-zonas asociadas
     */
    @Query("""
        SELECT pt.nombre, COUNT(cz) 
        FROM PlanTarifa pt 
        LEFT JOIN pt.contratosZonas cz 
        WHERE cz.activo = true
        GROUP BY pt.id, pt.nombre 
        ORDER BY COUNT(cz) DESC
        """)
    List<Object[]> findPlanesConMasContratosZonas();
    
    /**
     * Encuentra planes que tienen tarifas asociadas
     */
    @Query("""
        SELECT DISTINCT pt 
        FROM PlanTarifa pt 
        WHERE EXISTS (
            SELECT 1 FROM Tarifa t 
            WHERE t.planTarifa = pt
        )
        """)
    List<PlanTarifa> findPlanesConTarifas();
    
    /**
     * Encuentra planes que NO tienen tarifas asociadas
     */
    @Query("""
        SELECT pt 
        FROM PlanTarifa pt 
        WHERE NOT EXISTS (
            SELECT 1 FROM Tarifa t 
            WHERE t.planTarifa = pt
        )
        """)
    List<PlanTarifa> findPlanesSinTarifas();
    
    /**
     * Encuentra planes que tienen contratos-zonas asociadas
     */
    @Query("""
        SELECT DISTINCT pt 
        FROM PlanTarifa pt 
        WHERE EXISTS (
            SELECT 1 FROM ContratoZona cz 
            WHERE cz.planTarifa = pt 
            AND cz.activo = true
        )
        """)
    List<PlanTarifa> findPlanesConContratosZonas();
    
    /**
     * Encuentra planes que NO tienen contratos-zonas asociadas
     */
    @Query("""
        SELECT pt 
        FROM PlanTarifa pt 
        WHERE NOT EXISTS (
            SELECT 1 FROM ContratoZona cz 
            WHERE cz.planTarifa = pt 
            AND cz.activo = true
        )
        """)
    List<PlanTarifa> findPlanesSinContratosZonas();
    
    /**
     * Encuentra planes activos que tienen tanto tarifas como contratos-zonas activas
     */
    @Query("""
        SELECT pt 
        FROM PlanTarifa pt 
        WHERE pt.activo = true 
        AND EXISTS (
            SELECT 1 FROM Tarifa t 
            WHERE t.planTarifa = pt 
            AND t.activo = true
        )
        AND EXISTS (
            SELECT 1 FROM ContratoZona cz 
            WHERE cz.planTarifa = pt 
            AND cz.activo = true
        )
        """)
    List<PlanTarifa> findPlanesActivosCompletos();
    
    /**
     * Encuentra planes con estadísticas completas
     */
    @Query("""
        SELECT pt.uuid, pt.nombre, pt.activo,
               COUNT(DISTINCT t.id) as totalTarifas,
               COUNT(DISTINCT CASE WHEN t.activo = true THEN t.id END) as tarifasActivas,
               COUNT(DISTINCT cz.id) as totalZonas,
               COUNT(DISTINCT CASE WHEN cz.activo = true THEN cz.id END) as zonasActivas,
               COUNT(DISTINCT CASE WHEN cz.activo = true AND cz.contrato.estado = 'ACTIVO' THEN cz.contrato.id END) as contratosActivos
        FROM PlanTarifa pt 
        LEFT JOIN pt.tarifas t 
        LEFT JOIN pt.contratosZonas cz 
        GROUP BY pt.id, pt.uuid, pt.nombre, pt.activo
        ORDER BY pt.nombre
        """)
    List<Object[]> findPlanesConEstadisticas();
    
    /**
     * Verifica si un plan puede ser eliminado (sin tarifas ni contratos-zonas)
     */
    @Query("""
        SELECT CASE 
            WHEN (SELECT COUNT(t) FROM Tarifa t WHERE t.planTarifa.uuid = :planTarifaUuid) > 0 
                 OR (SELECT COUNT(cz) FROM ContratoZona cz WHERE cz.planTarifa.uuid = :planTarifaUuid AND cz.activo = true) > 0
            THEN false 
            ELSE true 
        END
        """)
    boolean puedeSerEliminado(@Param("planTarifaUuid") UUID planTarifaUuid);
    
    /**
     * Verifica si un plan puede ser desactivado (sin contratos activos)
     */
    @Query("""
        SELECT CASE 
            WHEN (SELECT COUNT(cz) FROM ContratoZona cz 
                  WHERE cz.planTarifa.uuid = :planTarifaUuid 
                  AND cz.activo = true 
                  AND cz.contrato.estado = 'ACTIVO') > 0
            THEN false 
            ELSE true 
        END
        """)
    boolean puedeSerDesactivado(@Param("planTarifaUuid") UUID planTarifaUuid);
    
    /**
     * Busca planes por nombre (búsqueda parcial, case insensitive)
     */
    @Query("""
        SELECT pt 
        FROM PlanTarifa pt 
        WHERE LOWER(pt.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))
        ORDER BY pt.nombre
        """)
    List<PlanTarifa> buscarPorNombreParcial(@Param("nombre") String nombre);
    
    /**
     * Obtiene planes más utilizados (por cantidad de zonas activas)
     */
    @Query("""
        SELECT pt, COUNT(cz) as cantidadUsos
        FROM PlanTarifa pt 
        LEFT JOIN pt.contratosZonas cz 
        WHERE cz.activo = true
        GROUP BY pt.id 
        HAVING COUNT(cz) > 0
        ORDER BY COUNT(cz) DESC
        """)
    List<Object[]> findPlanesMasUtilizados();

    List<PlanTarifa> findByActivoFalseOrderByNombreAsc();

    @Query("SELECT p FROM PlanTarifa p WHERE p.activo = true " +
        "ORDER BY p.nombre ASC")
    List<PlanTarifa> findActivosYVigentesOrderByNombre();

    @Query("SELECT COUNT(cz) FROM ContratoZona cz WHERE cz.planTarifa = :planTarifa AND cz.activo = true")
    long countContratoZonasActivos(@Param("planTarifa") PlanTarifa planTarifa);

    @Query("SELECT p FROM PlanTarifa p WHERE p.activo = true ORDER BY p.nombre ASC")
    List<PlanTarifa> findVigentesActuales();
}