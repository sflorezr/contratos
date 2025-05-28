package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.entity.Contrato;
import co.empresa.gestioncontratos.entity.Usuario;
import co.empresa.gestioncontratos.enums.EstadoContrato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {

    Optional<Contrato> findByUuid(UUID uuid);
    
    Optional<Contrato> findByCodigo(String codigo);
    
    boolean existsByCodigo(String codigo);
    
    List<Contrato> findAllByOrderByFechaInicioDesc();
    
    List<Contrato> findByEstadoOrderByFechaInicioDesc(EstadoContrato estado);
    
    List<Contrato> findBySupervisorOrderByFechaInicioDesc(Usuario supervisor);
    
    List<Contrato> findByCoordinadoresContainingOrderByFechaInicioDesc(Usuario coordinador);
    
    @Query("SELECT DISTINCT c FROM Contrato c " +
           "JOIN c.predios cp " +
           "WHERE cp.operario = :operario " +
           "ORDER BY c.fechaInicio DESC")
    List<Contrato> findContratosConOperario(@Param("operario") Usuario operario);
    
    @Query("SELECT COUNT(c) FROM Contrato c WHERE c.estado = :estado")
    long countByEstado(@Param("estado") EstadoContrato estado);
    
    @Query("SELECT c FROM Contrato c WHERE c.estado = 'ACTIVO' AND c.fechaFin < CURRENT_DATE")
    List<Contrato> findContratosVencidos();
}