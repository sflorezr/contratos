package co.empresa.gestioncontratos.service;

import co.empresa.gestioncontratos.enums.EstadoActividad;
import co.empresa.gestioncontratos.enums.EstadoContrato;
import co.empresa.gestioncontratos.enums.PerfilUsuario;
import co.empresa.gestioncontratos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final UsuarioRepository usuarioRepository;
    private final ContratoRepository contratoRepository;
    private final PredioRepository predioRepository;
    private final ActividadRepository actividadRepository;

    public Map<String, Object> getAdminStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsuarios", usuarioRepository.countByActivoTrue());
            stats.put("totalContratos", contratoRepository.count());
            stats.put("contratosActivos", contratoRepository.countByEstado(EstadoContrato.ACTIVO));
            stats.put("totalPredios", predioRepository.countByActivoTrue());
            stats.put("actividadesPendientes", actividadRepository.countByEstado(EstadoActividad.PENDIENTE));
            
            log.info("Estadísticas de admin generadas: {}", stats);
            return stats;
        } catch (Exception e) {
            log.error("Error generando estadísticas de admin: ", e);
            return getDefaultStats();
        }
    }

    public Map<String, Object> getSupervisorStats(Long supervisorId) {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("contratosAsignados", contratoRepository.countBySupervisorId(supervisorId));
            stats.put("contratosActivos", contratoRepository.countBySupervisorIdAndEstado(supervisorId, EstadoContrato.ACTIVO));
            stats.put("totalCoordinadores", usuarioRepository.countByPerfilAndActivoTrue(PerfilUsuario.COORDINADOR));
            stats.put("actividadesPendientes", actividadRepository.countByEstado(EstadoActividad.PENDIENTE));
            
            log.info("Estadísticas de supervisor {} generadas: {}", supervisorId, stats);
            return stats;
        } catch (Exception e) {
            log.error("Error generando estadísticas de supervisor {}: ", supervisorId, e);
            return getDefaultStats();
        }
    }

    public Map<String, Object> getCoordinadorStats(Long coordinadorId) {
        try {
            Map<String, Object> stats = new HashMap<>();
            // Por ahora estadísticas básicas
            stats.put("prediosAsignados", 0L);
            stats.put("operariosAsignados", usuarioRepository.countByPerfilAndActivoTrue(PerfilUsuario.OPERARIO));
            stats.put("actividadesPendientes", actividadRepository.countByEstado(EstadoActividad.PENDIENTE));
            stats.put("totalActividades", actividadRepository.count());
            
            log.info("Estadísticas de coordinador {} generadas: {}", coordinadorId, stats);
            return stats;
        } catch (Exception e) {
            log.error("Error generando estadísticas de coordinador {}: ", coordinadorId, e);
            return getDefaultStats();
        }
    }

    public Map<String, Object> getOperarioStats(Long operarioId) {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("prediosAsignados", predioRepository.countPrediosByOperarioId(operarioId));
            stats.put("actividadesHoy", actividadRepository.countActividadesHoyByOperario(operarioId));
            stats.put("actividadesMes", actividadRepository.countActividadesMesByOperario(operarioId));
            stats.put("totalActividades", actividadRepository.countByOperarioId(operarioId));
            
            log.info("Estadísticas de operario {} generadas: {}", operarioId, stats);
            return stats;
        } catch (Exception e) {
            log.error("Error generando estadísticas de operario {}: ", operarioId, e);
            return getDefaultStats();
        }
    }

    private Map<String, Object> getDefaultStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsuarios", 0L);
        stats.put("totalContratos", 0L);
        stats.put("contratosActivos", 0L);
        stats.put("totalPredios", 0L);
        stats.put("actividadesPendientes", 0L);
        stats.put("contratosAsignados", 0L);
        stats.put("totalCoordinadores", 0L);
        stats.put("prediosAsignados", 0L);
        stats.put("operariosAsignados", 0L);
        stats.put("actividadesHoy", 0L);
        stats.put("actividadesMes", 0L);
        stats.put("totalActividades", 0L);
        return stats;
    }
}