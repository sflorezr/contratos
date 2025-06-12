package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PredioOperarioRepository extends JpaRepository<PredioOperario, Long> {
    
    // Buscar por predio, contrato y activo
    Optional<PredioOperario> findByPredioAndContratoAndActivoTrue(Predio predio, Contrato contrato);
    
    // Buscar todos los operarios de un contrato
    @Query("SELECT DISTINCT po.operario FROM PredioOperario po " +
           "WHERE po.contrato = :contrato AND po.activo = true")
    List<Usuario> findOperariosByContrato(@Param("contrato") Contrato contrato);
    
    // Buscar todas las asignaciones de un operario en un contrato
    List<PredioOperario> findByOperarioAndContratoAndActivoTrue(Usuario operario, Contrato contrato);
    
    // Contar predios asignados a un operario en un contrato específico
    long countByOperarioAndContratoAndActivoTrue(Usuario operario, Contrato contrato);
    
    // Contar total de predios con operarios asignados en un contrato
    long countByContratoAndActivoTrue(Contrato contrato);
    
    // Buscar todas las asignaciones activas de un operario
    List<PredioOperario> findByOperarioAndActivoTrue(Usuario operario);
    
    // Verificar si un operario está asignado a un predio
    boolean existsByPredioAndOperarioAndActivoTrue(Predio predio, Usuario operario);
    
    // Buscar asignaciones por UUID
    Optional<PredioOperario> findByUuid(UUID uuid);
    
    // Buscar operarios por predio
    @Query("SELECT po.operario FROM PredioOperario po " +
           "WHERE po.predio = :predio AND po.activo = true")
    List<Usuario> findOperariosByPredio(@Param("predio") Predio predio);
    
    // Buscar predios asignados a un operario
    @Query("SELECT po.predio FROM PredioOperario po " +
           "WHERE po.operario = :operario AND po.activo = true")
    List<Predio> findPrediosByOperario(@Param("operario") Usuario operario);
    
    // Contar operarios únicos en un contrato
    @Query("SELECT COUNT(DISTINCT po.operario) FROM PredioOperario po " +
           "WHERE po.contrato = :contrato AND po.activo = true")
    long countDistinctOperariosByContrato(@Param("contrato") Contrato contrato);
    
    // Buscar asignaciones por contrato
    List<PredioOperario> findByContratoAndActivoTrue(Contrato contrato);
    
    // Buscar asignaciones históricas (incluye inactivas)
    List<PredioOperario> findByPredioOrderByFechaCreacionDesc(Predio predio);
    
    // Verificar si existe asignación para un predio en un contrato
    boolean existsByPredioAndContratoAndActivoTrue(Predio predio, Contrato contrato);


@Query("SELECT DISTINCT po.operario FROM PredioOperario po WHERE po.contrato = :contrato " +
       "AND po.activo = true ORDER BY po.operario.nombre")
List<Usuario> findOperariosByContratoDistinct(@Param("contrato") Contrato contrato);


}