package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.dto.SectorDTO;
import co.empresa.gestioncontratos.entity.Sector;
import co.empresa.gestioncontratos.entity.Zona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {
    
    Optional<Sector> findByUuid(UUID uuid);
    
    Optional<Sector> findByNombre(String nombre);
    
    boolean existsByNombre(String nombre);
    
    Optional<Sector> findByCodigo(String codigo);
    
    boolean existsByCodigo(String codigo);
    
    boolean existsByCodigoAndIdNot(String codigo, Long id);
    
    List<Sector> findAllByOrderByNombreAsc();
    
    List<Sector> findByActivoTrueOrderByNombreAsc();
    
    // Buscar sectores por zona
    List<Sector> findByZonaOrderByNombreAsc(Zona zona);
    
    List<Sector> findByZonaIdOrderByNombreAsc(Long zonaId);
    
    List<Sector> findByZonaAndActivoTrueOrderByNombreAsc(Zona zona);
    
    long countByActivoTrue();
    
    long countByActivoFalse();
    
    @Query("SELECT COUNT(DISTINCT s) FROM Sector s WHERE EXISTS (SELECT p FROM Predio p WHERE p.sector = s)")
    long countSectoresConPredios();
    
    @Query("SELECT COUNT(s) FROM Sector s " +
           "WHERE s.zona.id = (SELECT c.zona.id FROM ContratoZona c WHERE c.id = :contratoId)")
    long countSectoresPorContrato(@Param("contratoId") Long contratoId);

    @Query("SELECT COUNT(DISTINCT s) FROM Sector s " +
           "WHERE EXISTS (SELECT c FROM ContratoZona c WHERE c.zona = s.zona)")
    long countSectoresConContratos();
    
    @Query("SELECT COUNT(s) FROM Sector s " +
           "WHERE NOT EXISTS (SELECT p FROM Predio p WHERE p.sector = s) " +
           "AND NOT EXISTS (SELECT cz FROM ContratoZona cz WHERE cz.zona = s.zona)")
    long countSectoresSinActividad();

    // Contar predios por zona
    @Query("SELECT COUNT(DISTINCT p) FROM Predio p " +
           "WHERE p.sector.zona.id = :zonaId")
    long contarPrediosPorZona(@Param("zonaId") Long zonaId);
    
    // Contar predios asignados por zona (con operario en ContratoPredio)
    @Query("SELECT COUNT(DISTINCT cp.predio.id) FROM ContratoPredio cp " +
           "WHERE cp.predio.sector.zona.id = :zonaId " +
           "AND cp.operario IS NOT NULL " +
           "AND cp.activo = true")
    long contarPrediosAsignadosPorZona(@Param("zonaId") Long zonaId);
    
    // Queries adicionales útiles
    
    @Query("SELECT s FROM Sector s WHERE s.zona.id = :zonaId AND s.activo = true ORDER BY s.nombre ASC")
    List<Sector> findActivosPorZona(@Param("zonaId") Long zonaId);
    
    @Query("SELECT COUNT(s) FROM Sector s WHERE s.zona.id = :zonaId")
    long countByZonaId(@Param("zonaId") Long zonaId);
    
    @Query("SELECT COUNT(s) FROM Sector s WHERE s.zona.id = :zonaId AND s.activo = true")
    long countActivosByZonaId(@Param("zonaId") Long zonaId);
    
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Predio p WHERE p.sector.id = :sectorId")
    boolean tienePredios(@Param("sectorId") Long sectorId);
    
    @Query("SELECT COUNT(p) FROM Predio p WHERE p.sector.id = :sectorId")
    long contarPredios(@Param("sectorId") Long sectorId);
    
    @Query("SELECT COUNT(p) FROM Predio p WHERE p.sector.id = :sectorId AND p.activo = true")
    long contarPrediosActivos(@Param("sectorId") Long sectorId);
    
    @Query("SELECT s FROM Sector s JOIN FETCH s.zona WHERE s.uuid = :uuid")
    Optional<Sector> findByUuidWithZona(@Param("uuid") UUID uuid);
    
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Sector s WHERE s.uuid = :sectorUuid AND s.zona.uuid = :zonaUuid")
    boolean perteneceAZona(@Param("sectorUuid") UUID sectorUuid, @Param("zonaUuid") UUID zonaUuid);

       @Query("SELECT COUNT(s) FROM Sector s WHERE s.zona.id = :zonaId")
       long contarSectoresPorZona(@Param("zonaId") Long zonaId);

       @Query("SELECT COUNT(s) FROM Sector s WHERE s.zona.id = :zonaId AND s.activo = true")
       long contarSectoresActivosPorZona(@Param("zonaId") Long zonaId);

       @Query("SELECT s FROM Sector s WHERE s.zona = :zona ORDER BY s.nombre ASC")
       List<Sector> findByZonaOrderByOrdenAscNombreAsc(@Param("zona") Zona zona);
}