package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.entity.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
    
    List<Sector> findAllByOrderByNombreAsc();
    
    List<Sector> findByActivoTrueOrderByNombreAsc();
    
    long countByActivoTrue();
    
    long countByActivoFalse();
    
    @Query("SELECT COUNT(DISTINCT s) FROM Sector s JOIN Predio p ON p.sector = s")
    long countSectoresConPredios();
    
    @Query("SELECT COUNT(DISTINCT s) FROM Sector s JOIN Contrato c ON c.sector = s")
    long countSectoresConContratos();
    
    @Query("SELECT COUNT(s) FROM Sector s WHERE NOT EXISTS (SELECT p FROM Predio p WHERE p.sector = s) " +
           "AND NOT EXISTS (SELECT c FROM Contrato c WHERE c.sector = s)")
    long countSectoresSinActividad();
}
