package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.entity.Actividad;
import co.empresa.gestioncontratos.enums.EstadoActividad;
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
public interface ActividadRepository extends JpaRepository<Actividad, Long> {

    Optional<Actividad> findByUuid(UUID uuid);
    
    List<Actividad> findByOperarioId(Long operarioId);
    List<Actividad> findByPredioId(Long predioId);
    List<Actividad> findByEstado(EstadoActividad estado);
    
    long countByEstado(EstadoActividad estado);
    long countByOperarioId(Long operarioId);
    
    @Query("SELECT COUNT(a) FROM Actividad a WHERE a.operario.id = :operarioId AND a.fechaActividad = :fecha")
    long countActividadesHoyByOperario(@Param("operarioId") Long operarioId, @Param("fecha") LocalDate fecha);
    
    @Query("SELECT COUNT(a) FROM Actividad a WHERE a.operario.id = :operarioId AND YEAR(a.fechaActividad) = :year AND MONTH(a.fechaActividad) = :month")
    long countActividadesMesByOperario(@Param("operarioId") Long operarioId, @Param("year") int year, @Param("month") int month);
    
    // Métodos por defecto para estadísticas
    default long countActividadesHoyByOperario(Long operarioId) {
        return countActividadesHoyByOperario(operarioId, LocalDate.now());
    }
    
    default long countActividadesMesByOperario(Long operarioId) {
        LocalDate now = LocalDate.now();
        return countActividadesMesByOperario(operarioId, now.getYear(), now.getMonthValue());
    }
    
    Page<Actividad> findByOperarioId(Long operarioId, Pageable pageable);
    Page<Actividad> findByPredioId(Long predioId, Pageable pageable);
}