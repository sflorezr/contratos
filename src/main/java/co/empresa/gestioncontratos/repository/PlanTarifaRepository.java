package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.entity.PlanTarifa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanTarifaRepository extends JpaRepository<PlanTarifa, Long> {
    
    Optional<PlanTarifa> findByUuid(UUID uuid);
    
    Optional<PlanTarifa> findByNombre(String nombre);
    
    boolean existsByNombre(String nombre);
}