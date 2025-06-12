package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.entity.Zona;
import co.empresa.gestioncontratos.entity.Contrato;
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
public interface ZonaRepository extends JpaRepository<Zona, Long> {
    
    // ==================== BÚSQUEDAS BÁSICAS ====================
    Optional<Zona> findByUuid(UUID uuid);
    Optional<Zona> findByCodigo(String codigo);
    
    boolean existsByCodigo(String codigo);
    boolean existsByCodigoAndIdNot(String codigo, Long id);
    
    // ==================== BÚSQUEDAS POR ESTADO ====================
    List<Zona> findByActivoTrueOrderByNombreAsc();
    List<Zona> findByActivoFalseOrderByNombreAsc();
    Page<Zona> findByActivoOrderByNombreAsc(Boolean activo, Pageable pageable);
    
    // ==================== BÚSQUEDAS CON FILTROS ====================
    Page<Zona> findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCaseOrderByNombreAsc(
        String nombre, String codigo, Pageable pageable);
        
    Page<Zona> findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCaseAndActivoOrderByNombreAsc(
        String nombre, String codigo, Boolean activo, Pageable pageable);
    
    // ==================== CONSULTAS RELACIONADAS CON SECTORES ====================
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Sector s WHERE s.zona.id = :zonaId")
    boolean tieneSectores(@Param("zonaId") Long zonaId);
    
    @Query("SELECT COUNT(s) FROM Sector s WHERE s.zona.id = :zonaId")
    long contarSectores(@Param("zonaId") Long zonaId);
    
    @Query("SELECT COUNT(s) FROM Sector s WHERE s.zona.id = :zonaId AND s.activo = true")
    long contarSectoresActivos(@Param("zonaId") Long zonaId);


    // ==================== ESTADÍSTICAS ====================
    @Query("SELECT COUNT(z) FROM Zona z WHERE z.activo = :estado")
    long contarZonasEstado(@Param("estado") boolean estado);
    
    // ==================== CONSULTAS PARA CONTRATOS (A TRAVÉS DE CONTRATO_ZONA) ====================
    
    // Obtener zonas de un contrato específico (ya no se usa la relación directa)
    @Query("SELECT DISTINCT cz.zona FROM ContratoZona cz " +
           "WHERE cz.contrato.id = :contratoId AND cz.activo = true " +
           "ORDER BY cz.zona.nombre ASC")
    List<Zona> findByContratoIdWithSectores(@Param("contratoId") Long contratoId);
    
    // Buscar zonas que NO están asignadas a un contrato específico
    @Query("SELECT z FROM Zona z WHERE z.activo = true AND " +
           "z.id NOT IN (SELECT cz.zona.id FROM ContratoZona cz " +
           "WHERE cz.contrato.uuid = :contratoUuid AND cz.activo = true) " +
           "ORDER BY z.nombre ASC")
    List<Zona> findZonasDisponiblesParaContrato(@Param("contratoUuid") UUID contratoUuid);
    
    // Obtener zonas con más contratos asignados
    @Query("SELECT z, COUNT(cz) as totalContratos FROM Zona z " +
           "LEFT JOIN ContratoZona cz ON cz.zona = z AND cz.activo = true " +
           "WHERE z.activo = true " +
           "GROUP BY z.id ORDER BY totalContratos DESC")
    List<Object[]> findZonasConMasContratos();
    
    // Obtener zonas sin contratos asignados
    @Query("SELECT z FROM Zona z WHERE z.activo = true AND " +
           "NOT EXISTS (SELECT 1 FROM ContratoZona cz WHERE cz.zona = z AND cz.activo = true)")
    List<Zona> findZonasSinContratos();
    
    // ==================== BÚSQUEDAS COMPLEJAS ====================
    
    // Buscar zonas por múltiples criterios
    @Query("SELECT z FROM Zona z WHERE " +
           "(:nombre IS NULL OR LOWER(z.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))) AND " +
           "(:codigo IS NULL OR LOWER(z.codigo) LIKE LOWER(CONCAT('%', :codigo, '%'))) AND " +
           "(:activo IS NULL OR z.activo = :activo) " +
           "ORDER BY z.nombre ASC")
    Page<Zona> findConFiltros(@Param("nombre") String nombre,
                             @Param("codigo") String codigo,
                             @Param("activo") Boolean activo,
                             Pageable pageable);
    
    // Obtener zonas con resumen de información
    @Query("SELECT z.id, z.uuid, z.nombre, z.codigo, z.activo, " +
           "COUNT(s) as totalSectores, " +
           "COUNT(CASE WHEN s.activo = true THEN 1 END) as sectoresActivos, " +
           "COUNT(cz) as totalContratos " +
           "FROM Zona z " +
           "LEFT JOIN Sector s ON s.zona = z " +
           "LEFT JOIN ContratoZona cz ON cz.zona = z AND cz.activo = true " +
           "GROUP BY z.id, z.uuid, z.nombre, z.codigo, z.activo " +
           "ORDER BY z.nombre ASC")
    List<Object[]> findZonasConResumen();
    
    // ==================== VALIDACIONES ====================
    
    // Verificar si una zona puede ser eliminada
    @Query("SELECT CASE WHEN COUNT(cz) = 0 AND COUNT(s) = 0 THEN true ELSE false END " +
           "FROM Zona z " +
           "LEFT JOIN ContratoZona cz ON cz.zona = z " +
           "LEFT JOIN Sector s ON s.zona = z " +
           "WHERE z.uuid = :uuid")
    boolean puedeSerEliminada(@Param("uuid") UUID uuid);
    
    // Verificar si una zona puede ser desactivada
    @Query("SELECT CASE WHEN COUNT(cz) = 0 THEN true ELSE false END " +
           "FROM ContratoZona cz " +
           "WHERE cz.zona.uuid = :uuid  "+
           "AND cz.contrato.estado = 'ACTIVO'")
    boolean puedeSerDesactivada(@Param("uuid") UUID uuid);
}