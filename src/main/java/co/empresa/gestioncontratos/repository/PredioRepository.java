package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.entity.Predio;
import co.empresa.gestioncontratos.entity.Sector;
import co.empresa.gestioncontratos.enums.TipoPredio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PredioRepository extends JpaRepository<Predio, Long>, JpaSpecificationExecutor<Predio> {
    
    Optional<Predio> findByUuid(UUID uuid);
    
    Optional<Predio> findByCodigoCatastral(String codigoCatastral);
    
    boolean existsByCodigoCatastral(String codigoCatastral);
    
    List<Predio> findBySectorOrderByDireccion(Sector sector);
    
    List<Predio> findByTipoOrderByDireccion(TipoPredio tipo);
    
    List<Predio> findBySectorAndTipo(Sector sector, TipoPredio tipo);
    
    @Query("SELECT p FROM Predio p WHERE p.activo = true AND " +
           "NOT EXISTS (SELECT cp FROM ContratoPredio cp WHERE cp.predio = p AND cp.contrato.estado = 'ACTIVO')")
    List<Predio> findPrediosDisponibles();
    
    @Query("SELECT p FROM Predio p WHERE p.activo = true AND " +
           "NOT EXISTS (SELECT cp FROM ContratoPredio cp WHERE cp.predio = p AND cp.contrato.uuid = :contratoUuid)")
    List<Predio> findPrediosDisponiblesParaContrato(@Param("contratoUuid") UUID contratoUuid);
    
    long countByActivoTrue();
    
    long countByTipo(TipoPredio tipo);
    
    @Query("SELECT COUNT(DISTINCT p) FROM Predio p " +
           "JOIN ContratoPredio cp ON cp.predio = p " +
           "WHERE cp.contrato.estado = 'ACTIVO'")
    long countPrediosEnContratosActivos();
    
    @Query("SELECT COUNT(p) FROM Predio p WHERE p.activo = true AND " +
           "NOT EXISTS (SELECT cp FROM ContratoPredio cp WHERE cp.predio = p AND cp.contrato.estado = 'ACTIVO')")
    long countPrediosDisponibles();
}