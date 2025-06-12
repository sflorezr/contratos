package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.dto.ContratoDTO;
import co.empresa.gestioncontratos.entity.Contrato;
import co.empresa.gestioncontratos.entity.Usuario;
import co.empresa.gestioncontratos.enums.EstadoContrato;
import co.empresa.gestioncontratos.enums.PerfilUsuario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {
    
    Optional<Contrato> findByUuid(UUID uuid);
    
    Optional<Contrato> findByNumeroContrato(String numeroContrato);

   
    List<Contrato> findByEstadoOrderByFechaInicioDesc(EstadoContrato estado);
    
    
    
    @Query("SELECT DISTINCT c FROM Contrato c " +
           "JOIN c.contratoPredios cp " +
           "JOIN cp.predio p " +
           "JOIN p.predioOperarios po " +
           "WHERE po.operario = :operario AND po.activo = true " +
           "ORDER BY c.fechaInicio DESC")
    List<Contrato> findContratosConOperarioEnPredios(@Param("operario") Usuario operario);
 
    @Query("SELECT DISTINCT cp.operario FROM Contrato c " +
        "JOIN c.contratoPredios cp " +
        "WHERE c = :contrato " +
        "AND cp.operario IS NOT NULL " +
        "AND cp.activo = true " +
        "ORDER BY cp.operario.nombre, cp.operario.apellido")
    List<Usuario> findOperariosPorContrato(@Param("contrato") Contrato contrato);
    
    @Query("SELECT COUNT(c) FROM Contrato c WHERE c.estado = :estado")
    long countByEstado(@Param("estado") EstadoContrato estado);

    @Query("SELECT COUNT(c) FROM Contrato c WHERE c.uuid = :uuid")
    Optional<ContratoDTO> findDTOByUuid(@Param("uuid")UUID uuid);
    
    @Query("SELECT c FROM Contrato c WHERE c.estado = 'ACTIVO' AND c.fechaFin < CURRENT_DATE")
    List<Contrato> findContratosVencidos();

    @Query("SELECT c FROM Contrato c WHERE c.estado = 'ACTIVO'")
    List<Contrato> findContratosActivos();    

     @Query("SELECT COUNT(c) FROM Contrato c WHERE c.supervisor.id = :supervisorId")
    long countBySupervisorId(@Param("supervisorId") Long supervisorId);
    
    // Contar contratos por supervisor y estado
    @Query("SELECT COUNT(c) FROM Contrato c WHERE c.supervisor.id = :supervisorId AND c.estado = :estado")
    long countBySupervisorIdAndEstado(@Param("supervisorId") Long supervisorId, @Param("estado") EstadoContrato estado);


    @Query("SELECT DISTINCT c FROM Contrato c " +
        "JOIN c.contratoPredios cp " +
        "WHERE cp.operario = :operario " +
        "AND cp.activo = true " +
        "ORDER BY c.fechaInicio DESC")
    List<Contrato> findContratosConOperario(@Param("operario") Usuario operario);    
    // ==================== MÉTODOS FALTANTES PARA ContratoRepository ====================

    // Agregar estos métodos al ContratoRepository.java existente:

    // ==================== BÚSQUEDAS POR SUPERVISOR ====================
    List<Contrato> findBySupervisorOrderByFechaInicioDesc(Usuario supervisor);

    @Query("SELECT c FROM Contrato c WHERE c.supervisor = :supervisor   " +
        "ORDER BY c.fechaInicio DESC")
    List<Contrato> findBySupervisorAndActivoTrueOrderByFechaInicioDesc(@Param("supervisor") Usuario supervisor);

    // ==================== BÚSQUEDAS BÁSICAS ORDENADAS ====================
    @Query("SELECT c FROM Contrato c ORDER BY c.fechaInicio DESC")
    List<Contrato> findAllByOrderByFechaInicioDesc();

    // ==================== VALIDACIONES DE EXISTENCIA ====================
    boolean existsByNumeroContrato(String numeroContrato);
    boolean existsByNumeroContratoAndIdNot(String numeroContrato, Long id);

    // ==================== CONTADORES POR ESTADO ====================

    @Query("SELECT COUNT(c) FROM Contrato c WHERE c.fechaInicio <= CURRENT_DATE " +
        "AND c.fechaFin >= CURRENT_DATE   AND c.estado = 'ACTIVO'")
    long countVigentes();

    // ==================== ESTADÍSTICAS AVANZADAS ====================
    @Query("SELECT c.estado, COUNT(c) FROM Contrato c WHERE c.estado = 'ACTIVO' GROUP BY c.estado")
    List<Object[]> findEstadisticasPorEstado();

    @Query("SELECT FUNCTION('TO_CHAR', c.fechaCreacion, 'YYYY-MM') as mes, COUNT(c) " +
        "FROM Contrato c WHERE c.fechaCreacion >= :fechaDesde AND c.estado = 'ACTIVO' " +
        "GROUP BY FUNCTION('TO_CHAR', c.fechaCreacion, 'YYYY-MM') " +
        "ORDER BY mes DESC")
    List<Object[]> findContratosUltimos12Meses(@Param("fechaDesde") LocalDate fechaDesde);

    @Query("SELECT c FROM Contrato c WHERE c.fechaFin BETWEEN CURRENT_DATE AND :fechaLimite " +
        "AND c.estado = 'ACTIVO'   ORDER BY c.fechaFin ASC")
    List<Contrato> findProximosAVencer(@Param("fechaLimite") LocalDate fechaLimite);

    // ==================== CONTRATOS SIN ASIGNACIONES ====================
    @Query("SELECT c FROM Contrato c WHERE c.estado = 'ACTIVO' AND " +
        "NOT EXISTS (SELECT 1 FROM ContratoZona cz WHERE cz.contrato = c AND cz.activo = true)")
    List<Contrato> findSinZonasAsignadas();

    @Query("SELECT DISTINCT c FROM Contrato c " +
        "JOIN ContratoZona cz ON cz.contrato = c " +
        "WHERE c.estado = 'ACTIVO' AND cz.activo = true AND " +
        "(cz.coordinadorZona IS NULL OR cz.coordinadorOperativo IS NULL)")
    List<Contrato> findConZonasIncompletas();

    // ==================== BÚSQUEDAS CON FILTROS COMPLEJOS ====================
    @Query("SELECT c FROM Contrato c WHERE " +
        "(:numeroContrato IS NULL OR LOWER(c.numeroContrato) LIKE LOWER(CONCAT('%', :numeroContrato, '%'))) AND " +
        "(:empresa IS NULL OR LOWER(c.empresaContratante) LIKE LOWER(CONCAT('%', :empresa, '%'))) AND " +
        "(:estado IS NULL OR c.estado = :estado) AND " +
        "(:fechaDesde IS NULL OR c.fechaInicio >= :fechaDesde) AND " +
        "(:fechaHasta IS NULL OR c.fechaFin <= :fechaHasta) " +
        "ORDER BY c.fechaCreacion DESC")
    Page<Contrato> findConFiltros(@Param("numeroContrato") String numeroContrato,
                                @Param("empresa") String empresa,
                                @Param("estado") EstadoContrato estado,                               
                                @Param("fechaDesde") LocalDate fechaDesde,
                                @Param("fechaHasta") LocalDate fechaHasta,
                                Pageable pageable);

    // ==================== RESÚMENES Y REPORTES ====================
    @Query("SELECT c.numeroContrato, c.empresaContratante, c.estado, c.fechaInicio, c.fechaFin, " +
        "COUNT(cz) as totalZonas, " +
        "COUNT(CASE WHEN cz.coordinadorZona IS NOT NULL AND cz.coordinadorOperativo IS NOT NULL THEN 1 END) as zonasCompletas " +
        "FROM Contrato c LEFT JOIN ContratoZona cz ON cz.contrato = c AND cz.activo = true " +
        "WHERE c.estado = 'ACTIVO' GROUP BY c.id, c.numeroContrato, c.estado, c.fechaInicio, c.fechaFin " +
        "ORDER BY c.fechaCreacion DESC")
    List<Object[]> findResumenEjecutivo();

    @Query("SELECT c, COUNT(cz) as totalZonas FROM Contrato c " +
        "LEFT JOIN ContratoZona cz ON cz.contrato = c AND cz.activo = true " +
        "WHERE c.estado = 'ACTIVO' GROUP BY c ORDER BY c.fechaCreacion DESC")
    List<Object[]> findContratosConTotalZonas();

    // ==================== MÉTODOS PARA SEGURIDAD Y PERMISOS ====================
    @Query("SELECT c FROM Contrato c WHERE " +
        "(:perfil = 'ADMINISTRADOR' OR " +
        "(:perfil = 'SUPERVISOR' AND c.supervisor.uuid = :usuarioUuid) OR " +
        "(:perfil = 'COORDINADOR' AND EXISTS (SELECT 1 FROM ContratoZona cz WHERE cz.contrato = c AND " +
        "(cz.coordinadorZona.uuid = :usuarioUuid OR cz.coordinadorOperativo.uuid = :usuarioUuid) AND cz.activo = true)) OR " +
        "(:perfil = 'OPERARIO' AND EXISTS (SELECT 1 FROM PredioOperario po WHERE po.contrato = c AND " +
        "po.operario.uuid = :usuarioUuid AND po.activo = true))) " +
        "AND c.estado = 'ACTIVO' ORDER BY c.fechaCreacion DESC")
    List<Contrato> findAccesiblesPorUsuario(@Param("usuarioUuid") UUID usuarioUuid, 
                                        @Param("perfil") PerfilUsuario perfil);

    // ==================== MÉTODOS DE VALIDACIÓN DE FECHAS ====================
    @Query("SELECT c FROM Contrato c WHERE c.fechaInicio <= :fecha AND c.fechaFin >= :fecha " +
        "AND c.estado = 'ACTIVO'")
    List<Contrato> findVigentesPorFecha(@Param("fecha") LocalDate fecha);

    @Query("SELECT c FROM Contrato c WHERE " +
        "((c.fechaInicio BETWEEN :fechaInicio AND :fechaFin) OR " +
        "(c.fechaFin BETWEEN :fechaInicio AND :fechaFin) OR " +
        "(c.fechaInicio <= :fechaInicio AND c.fechaFin >= :fechaFin)) " +
        "  AND c.estado = 'ACTIVO' " +
        "AND (:contratoExcluir IS NULL OR c.uuid != :contratoExcluir)")
    List<Contrato> findSolapamientosFechas(@Param("fechaInicio") LocalDate fechaInicio,
                                        @Param("fechaFin") LocalDate fechaFin,
                                        @Param("contratoExcluir") UUID contratoExcluir);

    // ==================== MÉTODOS DE BÚSQUEDA POR EMPRESA ====================
  /*  List<Contrato> findByEmpresaContratanteContainingIgnoreCaseOrderByFechaCreacionDesc(String empresa);
    List<Contrato> findByEmpresaContratanteContainingIgnoreCaseAndActivoTrueOrderByFechaCreacionDesc(String empresa);

    @Query("SELECT c FROM Contrato c WHERE LOWER(c.empresaContratante) LIKE LOWER(CONCAT('%', :empresa, '%')) " +
        "  ORDER BY c.fechaCreacion DESC")
    Page<Contrato> findByEmpresaContratante(@Param("empresa") String empresa, Pageable pageable);
*/ 
    // ==================== MÉTODOS DE BÚSQUEDA POR RANGO DE FECHAS ====================
    List<Contrato> findByFechaInicioBetween(LocalDate fechaInicio, LocalDate fechaFin);
    List<Contrato> findByFechaFinBetween(LocalDate fechaInicio, LocalDate fechaFin);

    @Query("SELECT c FROM Contrato c WHERE c.fechaCreacion BETWEEN :fechaDesde AND :fechaHasta " +
        "  ORDER BY c.fechaCreacion DESC")
    List<Contrato> findByFechaCreacionBetween(@Param("fechaDesde") LocalDate fechaDesde, 
                                            @Param("fechaHasta") LocalDate fechaHasta);

    // ==================== MÉTODOS PARA DASHBOARDS Y MÉTRICAS ====================
    @Query("SELECT COUNT(c) FROM Contrato c WHERE c.fechaCreacion >= :fechaDesde ")
    long countContratosCreados(@Param("fechaDesde") LocalDate fechaDesde);

    @Query("SELECT COUNT(c) FROM Contrato c WHERE c.fechaFin <= CURRENT_DATE " +
        "AND c.estado = 'ACTIVO' ")
    long countContratosVencidos();

    @Query("SELECT COUNT(c) FROM Contrato c WHERE c.fechaFin BETWEEN CURRENT_DATE AND :fechaLimite " +
        "AND c.estado = 'ACTIVO' ")
    long countContratosPorVencer(@Param("fechaLimite") LocalDate fechaLimite);

    // ==================== MÉTODOS DE COORDINADORES POR CONTRATO ====================
    @Query("SELECT DISTINCT u FROM Usuario u " +
        "JOIN ContratoZona cz ON (cz.coordinadorZona = u OR cz.coordinadorOperativo = u) " +
        "WHERE cz.contrato.uuid = :contratoUuid AND cz.activo = true AND u.activo = true")
    List<Usuario> findCoordinadoresByContrato(@Param("contratoUuid") UUID contratoUuid);

    @Query("SELECT COUNT(DISTINCT u) FROM Usuario u " +
        "JOIN ContratoZona cz ON (cz.coordinadorZona = u OR cz.coordinadorOperativo = u) " +
        "WHERE cz.contrato = :contrato AND cz.activo = true")
    long countCoordinadoresByContrato(@Param("contrato") Contrato contrato);

    // ==================== MÉTODOS DE SUPERVISORES ====================
    @Query("SELECT DISTINCT c.supervisor FROM Contrato c WHERE c.supervisor IS NOT NULL " +
        " ORDER BY c.supervisor.nombre")
    List<Usuario> findSupervisoresActivos();

    @Query("SELECT c.supervisor, COUNT(c) FROM Contrato c WHERE c.supervisor IS NOT NULL " +
        " GROUP BY c.supervisor ORDER BY COUNT(c) DESC")
    List<Object[]> findResumenPorSupervisor();

    // ==================== MÉTODOS DE VALIDACIÓN DE SOLAPAMIENTOS ====================
    @Query("SELECT COUNT(c) FROM Contrato c " +
        "JOIN ContratoZona cz ON cz.contrato = c " +
        "WHERE (cz.coordinadorZona.uuid = :coordinadorUuid OR cz.coordinadorOperativo.uuid = :coordinadorUuid) " +
        "AND c.fechaInicio <= :fechaFin AND c.fechaFin >= :fechaInicio " +
        "  AND cz.activo = true AND c.estado = 'ACTIVO' " +
        "AND (:contratoUuidExcluir IS NULL OR c.uuid != :contratoUuidExcluir)")
    long countSolapamientosCoordinadorEnContrato(@Param("coordinadorUuid") UUID coordinadorUuid,
                                                @Param("fechaInicio") LocalDate fechaInicio,
                                                @Param("fechaFin") LocalDate fechaFin,
                                                @Param("contratoUuidExcluir") UUID contratoUuidExcluir);

    // ==================== MÉTODOS PARA REPORTES AVANZADOS ====================

    @Query("SELECT EXTRACT(YEAR  FROM c.fechaCreacion) as año, " +
        "EXTRACT(MONTH FROM c.fechaCreacion) as mes, " +
        "COUNT(c) as totalContratos, " +
        "COUNT(CASE WHEN c.estado = 'FINALIZADO' THEN 1 END) as finalizados " +
        "FROM Contrato c WHERE c.estado = 'ACTIVO' " +
        "GROUP BY EXTRACT( YEAR FROM c.fechaCreacion), " +
        "EXTRACT( MONTH FROM c.fechaCreacion) " +
        "ORDER BY año DESC, mes DESC")
    List<Object[]> findEvolucionMensualContratos();

    // ==================== MÉTODOS DE BÚSQUEDA POR MÚLTIPLES CRITERIOS ====================
    @Query("SELECT c FROM Contrato c WHERE " +
        "(:estados IS NULL OR c.estado IN :estados) AND " +
        "(:supervisorUuid IS NULL OR c.supervisor.uuid = :supervisorUuid) AND " +
        "(:fechaInicioDesde IS NULL OR c.fechaInicio >= :fechaInicioDesde) AND " +
        "(:fechaInicioHasta IS NULL OR c.fechaInicio <= :fechaInicioHasta) AND " +
        "(:activo IS NULL OR c.estado = 'ACTIVO') " +
        "ORDER BY c.fechaCreacion DESC")
    Page<Contrato> findByMultiplesCriterios(@Param("estados") List<EstadoContrato> estados,
                                        @Param("supervisorUuid") UUID supervisorUuid,
                                        @Param("fechaInicioDesde") LocalDate fechaInicioDesde,
                                        @Param("fechaInicioHasta") LocalDate fechaInicioHasta,
                                        @Param("activo") Boolean activo,
                                        Pageable pageable);

    // ==================== MÉTODOS DE AUDITORÍA Y TRAZABILIDAD ====================
    @Query("SELECT c FROM Contrato c WHERE c.fechaActualizacion >= :fechaDesde " +
        "ORDER BY c.fechaActualizacion DESC")
    List<Contrato> findModificadosDespuesDe(@Param("fechaDesde") LocalDateTime fechaDesde);

    @Query("SELECT c.uuid, c.numeroContrato, c.fechaCreacion, c.fechaActualizacion, " +
        "c.estado, c.supervisor.nombre " +
        "FROM Contrato c WHERE c.estado = 'ACTIVO' " +
        "ORDER BY c.fechaActualizacion DESC")
    List<Object[]> findAuditoriaContratos();

    @Query("SELECT c, " +
        "COUNT(cz) as totalZonas, " +
        "COUNT(CASE WHEN cz.coordinadorZona IS NOT NULL AND cz.coordinadorOperativo IS NOT NULL THEN 1 END) as zonasCompletas, " +
        "COUNT(cp) as totalPredios, " +
        "COUNT(CASE WHEN cp.estado = 'COMPLETADO' THEN 1 END) as prediosCompletados " +
        "FROM Contrato c " +
        "LEFT JOIN ContratoZona cz ON cz.contrato = c AND cz.activo = true " +
        "LEFT JOIN ContratoPredio cp ON cp.contrato = c AND cp.activo = true " +
        "WHERE c.estado = 'ACTIVO' " +
        "GROUP BY c " +
        "ORDER BY c.fechaCreacion DESC")
    List<Object[]> findContratosConMetricasCompletas();

    // ==================== MÉTODOS DE BÚSQUEDA TEXTUAL AVANZADA ====================
    @Query("SELECT c FROM Contrato c WHERE " +
        "(LOWER(c.numeroContrato) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
        "LOWER(c.objetivo) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
        "LOWER(c.empresaContratante) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
        "LOWER(c.supervisor.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))) " +
        "  " +
        "ORDER BY " +
        "CASE WHEN LOWER(c.numeroContrato) LIKE LOWER(CONCAT('%', :texto, '%')) THEN 1 " +
        "WHEN LOWER(c.empresaContratante) LIKE LOWER(CONCAT('%', :texto, '%')) THEN 2 " +
        "WHEN LOWER(c.objetivo) LIKE LOWER(CONCAT('%', :texto, '%')) THEN 3 " +
        "ELSE 4 END, c.fechaCreacion DESC")
    Page<Contrato> findByBusquedaTextual(@Param("texto") String texto, Pageable pageable);

    // ==================== MÉTODOS DE EXPORT Y BACKUP ====================
    @Query("SELECT c.uuid, c.numeroContrato, c.objetivo, " +
        "c.fechaInicio, c.fechaFin, c.estado, c.supervisor.nombre, " +
        "c.fechaCreacion, c.fechaActualizacion " +
        "FROM Contrato c WHERE c.estado = 'ACTIVO' " +
        "ORDER BY c.fechaCreacion DESC")
    List<Object[]> findDatosParaExport();


    // ==================== MÉTODOS DE VALIDACIÓN DE INTEGRIDAD ====================
    @Query("SELECT c FROM Contrato c WHERE c.estado = 'ACTIVO' AND " +
        "(c.numeroContrato IS NULL OR c.numeroContrato = '' OR " +
        "c.fechaInicio IS NULL OR c.fechaFin IS NULL OR " +
        "c.fechaFin < c.fechaInicio)")
    List<Contrato> findContratosConProblemasIntegridad();

    @Query("SELECT COUNT(c) FROM Contrato c WHERE c.numeroContrato = :numeroContrato " +
        "  AND (:excluirUuid IS NULL OR c.uuid != :excluirUuid)")
    long countByNumeroContratoExcluyendo(@Param("numeroContrato") String numeroContrato, 
                                        @Param("excluirUuid") UUID excluirUuid);

    // ==================== MÉTODOS PARA NOTIFICACIONES ====================

    @Query("SELECT DISTINCT cz.contrato FROM ContratoZona cz WHERE " +
        "(cz.coordinadorZona = :coordinador OR cz.coordinadorOperativo = :coordinador) " +
        "AND cz.activo = true ORDER BY cz.contrato.fechaCreacion DESC")
    List<Contrato> findByCoordinador(@Param("coordinador") Usuario coordinador);
}   


