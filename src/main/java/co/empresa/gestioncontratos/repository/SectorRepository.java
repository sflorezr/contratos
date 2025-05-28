package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.entity.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {
    
    Optional<Sector> findByUuid(UUID uuid);
    
    Optional<Sector> findByNombre(String nombre);
    
    boolean existsByNombre(String nombre);
}
