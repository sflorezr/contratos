package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.entity.Contrato;
import co.empresa.gestioncontratos.entity.ContratoZona;
import co.empresa.gestioncontratos.entity.Usuario;
import co.empresa.gestioncontratos.entity.Zona;
import co.empresa.gestioncontratos.enums.EstadoContratoZona;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContratoZonaRepository extends JpaRepository<ContratoZona, Long> {

    // ==================== BÚSQUEDAS POR UUID ====================
    Optional<ContratoZona> findByUuid(UUID uuid);
    Optional<ContratoZona> findByUuidAndActivoTrue(UUID uuid);

    // ==================== BÚSQUEDAS POR CONTRATO ====================
    List<ContratoZona> findByContratoOrderByFechaCreacionAsc(Contrato contrato);
    List<ContratoZona> findByContratoAndActivoTrueOrderByFechaCreacionAsc(Contrato contrato);
    List<ContratoZona> findByContratoAndEstadoOrderByFechaCreacionAsc(Contrato contrato, EstadoContratoZona estado);
    
    Page<ContratoZona> findByContrato(Contrato contrato, Pageable pageable);
    Page<ContratoZona> findByContratoAndActivoTrue(Contrato contrato, Pageable pageable);

    // ==================== BÚSQUEDAS POR ZONA ====================
    List<ContratoZona> findByZonaOrderByFechaCreacionDesc(Zona zona);
    List<ContratoZona> findByZonaAndActivoTrueOrderByFechaCreacionDesc(Zona zona);
    List<ContratoZona> findByZonaAndEstado(Zona zona, EstadoContratoZona estado);

    // ==================== BÚSQUEDAS POR COORDINADORES ====================
    List<ContratoZona> findByCoordinadorZonaOrderByFechaCreacionDesc(Usuario coordinadorZona);
    List<ContratoZona> findByCoordinadorZonaAndActivoTrueOrderByFechaCreacionDesc(Usuario coordinadorZona);
    
    List<ContratoZona> findByCoordinadorOperativoOrderByFechaCreacionDesc(Usuario coordinadorOperativo);
    List<ContratoZona> findByCoordinadorOperativoAndActivoTrueOrderByFechaCreacionDesc(Usuario coordinadorOperativo);

    // ==================== BÚSQUEDAS COMBINADAS ====================
    Optional<ContratoZona> findByContratoAndZona(Contrato contrato, Zona zona);
    Optional<ContratoZona> findByContratoAndZonaAndActivoTrue(Contrato contrato, Zona zona);
    
    @Query("SELECT cz FROM ContratoZona cz WHERE cz.contrato.uuid = :contratoUuid AND cz.zona.uuid = :zonaUuid")
    Optional<ContratoZona> findByContratoUuidAndZonaUuid(@Param("contratoUuid") UUID contratoUuid, 
                                                        @Param("zonaUuid") UUID zonaUuid);

    // ==================== VERIFICACIONES DE EXISTENCIA ====================
    boolean existsByContratoAndZona(Contrato contrato, Zona zona);
    boolean existsByContratoAndZonaAndActivoTrue(Contrato contrato, Zona zona);
    
    @Query("SELECT CASE WHEN COUNT(cz) > 0 THEN true ELSE false END FROM ContratoZona cz " +
           "WHERE cz.contrato.uuid = :contratoUuid AND cz.zona.uuid = :zonaUuid AND cz.activo = true")
    boolean existsActiveByContratoUuidAndZonaUuid(@Param("contratoUuid") UUID contratoUuid, 
                                                 @Param("zonaUuid") UUID zonaUuid);

    // ==================== CONTADORES ====================
    long countByContrato(Contrato contrato);
    long countByContratoAndActivoTrue(Contrato contrato);
    long countByContratoAndEstado(Contrato contrato, EstadoContratoZona estado);
    
    long countByZona(Zona zona);
    long countByZonaAndActivoTrue(Zona zona);
    
    long countByCoordinadorZona(Usuario coordinadorZona);
    long countByCoordinadorZonaAndActivoTrue(Usuario coordinadorZona);
    
    long countByCoordinadorOperativo(Usuario coordinadorOperativo);
    long countByCoordinadorOperativoAndActivoTrue(Usuario coordinadorOperativo);

    // ==================== CONSULTAS ESPECÍFICAS ====================
    
    // Obtener todas las zonas de contratos donde participa un coordinador (zona u operativo)
    @Query("SELECT DISTINCT cz FROM ContratoZona cz WHERE " +
           "(cz.coordinadorZona = :coordinador OR cz.coordinadorOperativo = :coordinador) " +
           "AND cz.activo = true ORDER BY cz.fechaCreacion DESC")
    List<ContratoZona> findZonasByCoordinador(@Param("coordinador") Usuario coordinador);

    // Obtener zonas sin coordinador asignado en un contrato específico
    @Query("SELECT cz FROM ContratoZona cz WHERE cz.contrato = :contrato AND cz.activo = true " +
           "AND (cz.coordinadorZona IS NULL OR cz.coordinadorOperativo IS NULL)")
    List<ContratoZona> findZonasSinCoordinadorCompleto(@Param("contrato") Contrato contrato);

    // Obtener contratos activos por rango de fechas que tienen zonas específicas
    @Query("SELECT DISTINCT cz.contrato FROM ContratoZona cz WHERE " +
           "cz.zona IN :zonas AND cz.activo = true AND cz.contrato.estado = 'ACTIVO' " +
           "AND cz.contrato.fechaInicio <= CURRENT_DATE AND cz.contrato.fechaFin >= CURRENT_DATE")
    List<Contrato> findContratosActivosByZonas(@Param("zonas") List<Zona> zonas);

    // Estadísticas por estado de zona
    @Query("SELECT cz.estado, COUNT(cz) FROM ContratoZona cz WHERE cz.contrato = :contrato " +
           "AND cz.activo = true GROUP BY cz.estado")
    List<Object[]> findEstadisticasEstadosByContrato(@Param("contrato") Contrato contrato);

    // Obtener zonas con mayor cantidad de contratos
    @Query("SELECT cz.zona, COUNT(cz) as cantidad FROM ContratoZona cz WHERE cz.activo = true " +
           "GROUP BY cz.zona ORDER BY cantidad DESC")
    List<Object[]> findZonasConMasContratos();

    // Verificar si un coordinador puede ser asignado (no tiene conflictos de horario)
    @Query("SELECT COUNT(cz) FROM ContratoZona cz WHERE " +
           "(cz.coordinadorZona = :coordinador OR cz.coordinadorOperativo = :coordinador) " +
           "AND cz.contrato.fechaInicio <= :fechaFin AND cz.contrato.fechaFin >= :fechaInicio " +
           "AND cz.activo = true AND cz.contrato.estado = 'ACTIVO'")
    long countConflictosCoordinador(@Param("coordinador") Usuario coordinador,
                                   @Param("fechaInicio") java.time.LocalDate fechaInicio,
                                   @Param("fechaFin") java.time.LocalDate fechaFin);

    // Obtener resumen de zonas por plan de tarifa
    @Query("SELECT cz.planTarifa.nombre, COUNT(cz) FROM ContratoZona cz WHERE cz.activo = true " +
           "GROUP BY cz.planTarifa.nombre ORDER BY COUNT(cz) DESC")
    List<Object[]> findResumenPorPlanTarifa();

    // Buscar zonas que necesitan atención (sin coordinadores o en estado problemático)
    @Query("SELECT cz FROM ContratoZona cz WHERE cz.activo = true AND " +
           "(cz.coordinadorZona IS NULL OR cz.coordinadorOperativo IS NULL OR " +
           "cz.estado IN ('SUSPENDIDO', 'CANCELADO')) " +
           "ORDER BY cz.fechaCreacion ASC")
    List<ContratoZona> findZonasQueNecesitanAtencion();

    // Obtener zonas por múltiples estados
    @Query("SELECT cz FROM ContratoZona cz WHERE cz.contrato = :contrato AND cz.activo = true " +
           "AND cz.estado IN :estados ORDER BY cz.fechaCreacion ASC")
    List<ContratoZona> findByContratoAndEstados(@Param("contrato") Contrato contrato,
                                               @Param("estados") List<EstadoContratoZona> estados);

    // Para reportes: obtener información consolidada
    @Query("SELECT cz.contrato.numeroContrato, cz.zona.nombre, cz.planTarifa.nombre, " +
           "cz.coordinadorZona.username, cz.coordinadorOperativo.username, cz.estado " +
           "FROM ContratoZona cz WHERE cz.activo = true ORDER BY cz.contrato.numeroContrato")
    List<Object[]> findResumenCompleto();

    // Buscar por múltiples criterios con filtros
    @Query("SELECT cz FROM ContratoZona cz WHERE " +
           "(:contratoUuid IS NULL OR cz.contrato.uuid = :contratoUuid) AND " +
           "(:zonaUuid IS NULL OR cz.zona.uuid = :zonaUuid) AND " +
           "(:estado IS NULL OR cz.estado = :estado) AND " +
           "(:activo IS NULL OR cz.activo = :activo) AND " +
           "(:coordinadorUuid IS NULL OR cz.coordinadorZona.uuid = :coordinadorUuid OR cz.coordinadorOperativo.uuid = :coordinadorUuid) " +
           "ORDER BY cz.fechaCreacion DESC")
    Page<ContratoZona> findConFiltros(@Param("contratoUuid") UUID contratoUuid,
                                     @Param("zonaUuid") UUID zonaUuid,
                                     @Param("estado") EstadoContratoZona estado,
                                     @Param("activo") Boolean activo,
                                     @Param("coordinadorUuid") UUID coordinadorUuid,
                                     Pageable pageable);

       @Query("SELECT COUNT(cz) FROM ContratoZona cz WHERE cz.activo = true")
       long countByActivoTrue();

       @Query("SELECT cz FROM ContratoZona cz WHERE cz.contrato = :contrato " +
              "AND cz.estado != 'CANCELADO' AND cz.activo = true " +
              "ORDER BY cz.fechaCreacion ASC")
       List<ContratoZona> findByContratoAndEstadoNoCanceladoOrderByFechaCreacionAsc(@Param("contrato") Contrato contrato);

       @Query("SELECT COUNT(cz) FROM ContratoZona cz WHERE cz.contrato = :contrato " +
              "AND cz.estado = 'COMPLETADO' AND cz.activo = true")
       long countByContratoAndEstadoCompletado(@Param("contrato") Contrato contrato);                                     


       @Query("SELECT CASE WHEN COUNT(cz) > 0 THEN true ELSE false END FROM ContratoZona cz " +
       "WHERE cz.contrato.uuid = :contratoUuid " +
       "AND (cz.coordinadorZona = :usuario OR cz.coordinadorOperativo = :usuario) " +
       "AND cz.activo = true")
       boolean esCoordinadorDeContrato(@Param("contratoUuid") UUID contratoUuid, 
                                   @Param("usuario") Usuario usuario);
}