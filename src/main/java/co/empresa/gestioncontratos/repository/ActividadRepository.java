package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.entity.Actividad;
import co.empresa.gestioncontratos.entity.ContratoPredio;
import co.empresa.gestioncontratos.entity.Predio;
import co.empresa.gestioncontratos.enums.EstadoActividad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ActividadRepository extends JpaRepository<Actividad, Long> {
    
    boolean existsByContratoPredio(ContratoPredio contratoPredio);
    
    boolean existsByContratoPredio_Predio(Predio predio);
    
   // boolean existsByContratoPredio_PredioAndCompletadaFalse(Predio predio);
    
    long countByContratoPredio(ContratoPredio contratoPredio);
    
    long countByContratoPredio_Predio(Predio predio);
    
    @Query("SELECT COUNT(a) FROM Actividad a WHERE a.contratoPredio.operario.uuid = :operarioUuid")
    long countByOperario(@Param("operarioUuid") UUID operarioUuid);

     long countByEstado(EstadoActividad estado);
    
    // Contar actividades de hoy por operario
    @Query("SELECT COUNT(a) FROM Actividad a WHERE a.operario.id = :operarioId " +
           "AND a.fechaCreacion = CURRENT_DATE")
    long countActividadesHoyByOperario(@Param("operarioId") Long operarioId);
    
    // Contar actividades del mes por operario
    @Query("SELECT COUNT(a) FROM Actividad a WHERE a.operario.id = :operarioId " +
           "AND MONTH(a.fechaCreacion) = MONTH(CURRENT_DATE) " +
           "AND YEAR(a.fechaCreacion) = YEAR(CURRENT_DATE)")
    long countActividadesMesByOperario(@Param("operarioId") Long operarioId);
    
    // Contar por operario
    @Query("SELECT COUNT(a) FROM Actividad a WHERE a.operario.id = :operarioId")
    long countByOperarioId(@Param("operarioId") Long operarioId);

}