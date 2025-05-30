package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.entity.Predio;
import co.empresa.gestioncontratos.entity.Sector;
import co.empresa.gestioncontratos.entity.Usuario;
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
    
    List<Predio> findBySectorOrderByDireccion(Sector sector);
    
    List<Predio> findByTipoOrderByDireccion(TipoPredio tipo);
    
    List<Predio> findBySectorAndTipo(Sector sector, TipoPredio tipo);
    
    @Query("SELECT p FROM Predio p WHERE p.activo = true AND " +
           "NOT EXISTS (SELECT cp FROM ContratoPredio cp WHERE cp.predio = p AND cp.contrato.estado = 'ACTIVO' AND cp.activo = true)")
    List<Predio> findPrediosDisponibles();
    
    @Query("SELECT p FROM Predio p WHERE p.activo = true AND " +
           "NOT EXISTS (SELECT cp FROM ContratoPredio cp WHERE cp.predio = p AND cp.contrato.uuid = :contratoUuid AND cp.activo = true)")
    List<Predio> findPrediosDisponiblesParaContrato(@Param("contratoUuid") UUID contratoUuid);
    
    @Query("SELECT DISTINCT p FROM Predio p " +
           "JOIN p.predioOperarios po " +
           "WHERE po.operario = :operario AND po.activo = true")
    List<Predio> findPrediosPorOperario(@Param("operario") Usuario operario);
    
    long countByActivoTrue();
    
    long countByTipo(TipoPredio tipo);
    
    @Query("SELECT COUNT(DISTINCT p) FROM Predio p " +
           "JOIN ContratoPredio cp ON cp.predio = p " +
           "WHERE cp.contrato.estado = 'ACTIVO' AND cp.activo = true")
    long countPrediosEnContratosActivos();
    
    @Query("SELECT COUNT(p) FROM Predio p WHERE p.activo = true AND " +
           "NOT EXISTS (SELECT cp FROM ContratoPredio cp WHERE cp.predio = p AND cp.contrato.estado = 'ACTIVO' AND cp.activo = true)")
    long countPrediosDisponibles();

        @Query("SELECT COUNT(DISTINCT cp.predio) FROM ContratoPredio cp WHERE cp.operario.id = :operarioId")
    long countPrediosByOperarioId(@Param("operarioId") Long operarioId);
    long countBySector(Sector sector);
    
    long countBySectorAndActivoTrue(Sector sector);
    
    long countBySectorAndTipo(Sector sector, TipoPredio tipo);
    
    boolean existsBySector(Sector sector);
    
    @Query("SELECT SUM(p.area) FROM Predio p WHERE p.sector = :sector")
    Double sumAreaBySector(@Param("sector") Sector sector);

    Optional<Predio> findByCodigoCatastral(String codigoCatastral);
       boolean existsByCodigoCatastral(String codigoCatastral);
}