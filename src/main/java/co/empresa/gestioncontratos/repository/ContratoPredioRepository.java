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
    
    List<ContratoPredio> findByPredio(Predio predio);
    
    List<ContratoPredio> findByPredioOrderByFechaCreacionDesc(Predio predio);
    
    List<ContratoPredio> findByOperario(Usuario operario);
    
    List<ContratoPredio> findByContratoAndOperario(Contrato contrato, Usuario operario);
    
    @Query("SELECT DISTINCT cp.operario FROM ContratoPredio cp " +
           "WHERE cp.contrato = :contrato AND cp.operario IS NOT NULL")
    List<Usuario> findOperariosDelContrato(@Param("contrato") Contrato contrato);
    
    @Query("SELECT cp.predio FROM ContratoPredio cp WHERE cp.operario = :operario")
    List<Predio> findPrediosByOperario(@Param("operario") Usuario operario);
    
    @Query("SELECT cp.predio FROM ContratoPredio cp " +
           "WHERE cp.operario = :operario AND cp.contrato.uuid = :contratoUuid")
    List<Predio> findPrediosByOperarioAndContratoUuid(@Param("operario") Usuario operario, 
                                                      @Param("contratoUuid") UUID contratoUuid);
    
    @Query("SELECT cp FROM ContratoPredio cp " +
           "WHERE cp.predio = :predio AND cp.contrato.estado = 'ACTIVO' " +
           "ORDER BY cp.fechaCreacion DESC")
    Optional<ContratoPredio> findActiveByPredio(@Param("predio") Predio predio);
    
    boolean existsByContratoAndPredio(Contrato contrato, Predio predio);
    
    boolean existsByPredioAndContratoEstado(Predio predio, EstadoContrato estadoContrato);
    
    long countByContrato(Contrato contrato);
    
    long countByContratoAndOperarioIsNotNull(Contrato contrato);
    
    long countByContratoAndOperarioIsNull(Contrato contrato);
    
    long countByContratoAndOperario(Contrato contrato, Usuario operario);
    
    long countByContratoAndEstado(Contrato contrato, EstadoPredio estado);
}