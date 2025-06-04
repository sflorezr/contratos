package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.entity.Zona;
import co.empresa.gestioncontratos.entity.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ZonaRepository extends JpaRepository<Zona, Long> {
    
    Optional<Zona> findByUuid(UUID uuid);
    
    Optional<Zona> findByCodigo(String codigo);
    
    boolean existsByCodigo(String codigo);
    
    boolean existsByCodigoAndIdNot(String codigo, Long id);
    
    List<Zona> findByActivoTrueOrderByNombreAsc();
    List<Zona> findByActivoFalseOrderByNombreAsc();
    
    @Query("SELECT DISTINCT z FROM Contrato c "+
           "JOIN c.zona z "+
           "WHERE c.id = :contratoId ORDER BY z.nombre ASC")
    List<Zona> findByContratoAndActivoTrueOrderByOrdenAscNombreAsc(Contrato contrato);
    
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Sector s WHERE s.zona.id = :zonaId")
    boolean tieneSectores(@Param("zonaId") Long zonaId);
    
    @Query("SELECT COUNT(s) FROM Sector s WHERE s.zona.id = :zonaId")
    long contarSectores(@Param("zonaId") Long zonaId);
    
    @Query("SELECT COUNT(s) FROM Sector s WHERE s.zona.id = :zonaId AND s.activo = true")
    long contarSectoresActivos(@Param("zonaId") Long zonaId);
    
    
    @Query("SELECT z FROM Sector s JOIN s.zona z WHERE z.id = :id")
    Optional<Zona> findByIdWithSectores(@Param("id") Long id);

    @Query("SELECT z FROM Zona z WHERE z.activo = :estado")
    long contarZonasEstado(@Param("estado") boolean estado);    
    
    @Query("SELECT DISTINCT z FROM Contrato c "+
           "JOIN c.zona z "+
           "WHERE c.id = :contratoId ORDER BY z.nombre ASC")
    List<Zona> findByContratoIdWithSectores(@Param("contratoId") Long contratoId);
    
    // Consultas para estadísticas


}