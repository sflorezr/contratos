package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.entity.Contrato;
import co.empresa.gestioncontratos.entity.Sector;
import co.empresa.gestioncontratos.entity.Usuario;
import co.empresa.gestioncontratos.enums.EstadoContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {
    
    Optional<Contrato> findByUuid(UUID uuid);
    
    Optional<Contrato> findByNumeroContrato(String numeroContrato);
    
    boolean existsByNumeroContrato(String numeroContrato);
    
    List<Contrato> findAllByOrderByFechaInicioDesc();
    
    List<Contrato> findByEstadoOrderByFechaInicioDesc(EstadoContrato estado);
    
    List<Contrato> findBySupervisorOrderByFechaInicioDesc(Usuario supervisor);
    
    List<Contrato> findByCoordinadoresContainingOrderByFechaInicioDesc(Usuario coordinador);
    
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
    
    @Query("SELECT c FROM Contrato c WHERE c.estado = 'ACTIVO' AND c.fechaFin < CURRENT_DATE")
    List<Contrato> findContratosVencidos();

     @Query("SELECT COUNT(c) FROM Contrato c WHERE c.supervisor.id = :supervisorId")
    long countBySupervisorId(@Param("supervisorId") Long supervisorId);
    
    // Contar contratos por supervisor y estado
    @Query("SELECT COUNT(c) FROM Contrato c WHERE c.supervisor.id = :supervisorId AND c.estado = :estado")
    long countBySupervisorIdAndEstado(@Param("supervisorId") Long supervisorId, @Param("estado") EstadoContrato estado);
    long countBySector(Sector sector);
    
    long countBySectorAndEstado(Sector sector, EstadoContrato estado);
    
    boolean existsBySector(Sector sector);
}
