package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.entity.Servicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long>, JpaSpecificationExecutor<Servicio> {
    
    // Búsquedas básicas
    Optional<Servicio> findByUuid(UUID uuid);
    
    Optional<Servicio> findByNombre(String nombre);
    
    // Verificación de existencia
    boolean existsByNombre(String nombre);
    
    boolean existsByUuid(UUID uuid);
    
    // Búsquedas por estado
    List<Servicio> findByActivoTrue();
    
    List<Servicio> findByActivoFalse();
    
    Page<Servicio> findByActivoTrue(Pageable pageable);
    
    // Ordenamiento
    List<Servicio> findAllByOrderByNombreAsc();
    
    List<Servicio> findByActivoTrueOrderByNombreAsc();
    
    // Búsquedas con filtros
    @Query("SELECT s FROM Servicio s WHERE " +
           "(:activo IS NULL OR s.activo = :activo) AND " +
           "(LOWER(s.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(s.descripcion) LIKE LOWER(CONCAT('%', :filtro, '%')))")
    Page<Servicio> buscarConFiltros(@Param("activo") Boolean activo,
                                    @Param("filtro") String filtro,
                                    Pageable pageable);
    
    // Contadores
    long countByActivoTrue();
    
    long countByActivoFalse();
    
    // Servicios con tarifas
    @Query("SELECT DISTINCT s FROM Servicio s JOIN s.tarifas t WHERE t.activo = true")
    List<Servicio> findServiciosConTarifasActivas();
    
    @Query("SELECT s FROM Servicio s WHERE s.activo = true AND SIZE(s.tarifas) > 0")
    List<Servicio> findServiciosActivosConTarifas();
    
    // Servicios sin tarifas
    @Query("SELECT s FROM Servicio s WHERE s.activo = true AND SIZE(s.tarifas) = 0")
    List<Servicio> findServiciosActivosSinTarifas();
    
    // Servicios con actividades
    @Query("SELECT DISTINCT s FROM Servicio s JOIN s.actividades a WHERE a.estado = 'PENDIENTE'")
    List<Servicio> findServiciosConActividadesPendientes();
    
    @Query("SELECT s, COUNT(a) FROM Servicio s LEFT JOIN s.actividades a " +
           "WHERE s.activo = true " +
           "GROUP BY s " +
           "ORDER BY COUNT(a) DESC")
    List<Object[]> findServiciosConContadorActividades();
    
    // Estadísticas
    @Query("SELECT COUNT(DISTINCT s) FROM Servicio s JOIN s.tarifas t")
    long countServiciosConTarifas();
    
    @Query("SELECT COUNT(DISTINCT s) FROM Servicio s JOIN s.actividades a")
    long countServiciosConActividades();
    
    @Query("SELECT COUNT(DISTINCT s) FROM Servicio s " +
           "WHERE NOT EXISTS (SELECT t FROM Tarifa t WHERE t.servicio = s)")
    long countServiciosSinTarifas();
    
    // Búsqueda de servicios por plan de tarifa
    @Query("SELECT DISTINCT s FROM Servicio s " +
           "JOIN s.tarifas t " +
           "JOIN t.planTarifa pt " +
           "WHERE pt.uuid = :planTarifaUuid AND s.activo = true")
    List<Servicio> findServiciosByPlanTarifa(@Param("planTarifaUuid") UUID planTarifaUuid);
    
    // Servicios más utilizados (con más actividades)
    @Query("SELECT s FROM Servicio s " +
           "LEFT JOIN s.actividades a " +
           "WHERE s.activo = true " +
           "GROUP BY s " +
           "ORDER BY COUNT(a) DESC")
    Page<Servicio> findServiciosMasUtilizados(Pageable pageable);
    
    // Búsqueda por nombre similar (para autocompletado)
    @Query("SELECT s FROM Servicio s WHERE s.activo = true AND LOWER(s.nombre) LIKE LOWER(CONCAT(:prefijo, '%'))")
    List<Servicio> findByNombreStartingWith(@Param("prefijo") String prefijo);
}