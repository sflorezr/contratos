package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.entity.*;
import co.empresa.gestioncontratos.enums.EstadoContrato;
import co.empresa.gestioncontratos.enums.EstadoPredio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContratoPredioRepository extends JpaRepository<ContratoPredio, Long> {
    
    Optional<ContratoPredio> findByContratoUuidAndPredioUuid(UUID contratoUuid, UUID predioUuid);
    
    List<ContratoPredio> findByContrato(Contrato contrato);
    
    List<ContratoPredio> findByContratoAndActivoTrue(Contrato contrato);
    
    List<ContratoPredio> findByPredio(Predio predio);
    
    List<ContratoPredio> findByPredioOrderByFechaCreacionDesc(Predio predio);
    
    @Query("SELECT cp FROM ContratoPredio cp " +
           "WHERE cp.predio = :predio AND cp.contrato.estado = 'ACTIVO' " +
           "AND cp.activo = true " +
           "ORDER BY cp.fechaCreacion DESC")
    Optional<ContratoPredio> findActiveByPredio(@Param("predio") Predio predio);
    
    boolean existsByContratoAndPredio(Contrato contrato, Predio predio);
    
    boolean existsByPredioAndContratoEstado(Predio predio, EstadoContrato estadoContrato);
    
    long countByContrato(Contrato contrato);
    
    long countByContratoAndActivoTrue(Contrato contrato);
    
    long countByContratoAndEstado(Contrato contrato, EstadoPredio estado);
}