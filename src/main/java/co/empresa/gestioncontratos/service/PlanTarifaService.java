package co.empresa.gestioncontratos.service;

import co.empresa.gestioncontratos.dto.PlanTarifaDTO;
import co.empresa.gestioncontratos.entity.PlanTarifa;
import co.empresa.gestioncontratos.repository.ContratoZonaRepository;
import co.empresa.gestioncontratos.repository.PlanTarifaRepository;
import co.empresa.gestioncontratos.repository.TarifaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlanTarifaService {

    private final PlanTarifaRepository planTarifaRepository;
    private final TarifaRepository tarifaRepository;
    private final ContratoZonaRepository contratoZonaRepository;

    // ==================== CONSULTAS BÁSICAS ====================

    @Transactional(readOnly = true)
    public List<PlanTarifa> listarTodos() {
        log.info("Listando todos los planes de tarifa");
        return planTarifaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<PlanTarifa> listarActivos() {
        log.info("Listando planes de tarifa activos");
        return planTarifaRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<PlanTarifaDTO> listarActivosDTO() {
        log.info("Listando planes de tarifa activos");
        return planTarifaRepository.findByAllDTO();
    }

    @Transactional(readOnly = true)
    public Page<PlanTarifa> listarTodosPaginado(Pageable pageable) {
        return planTarifaRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<PlanTarifa> buscarConFiltros(String filtro, Boolean activo, Pageable pageable) {
        log.info("Buscando planes de tarifa con filtros - texto: {}, activo: {}", filtro, activo);
        
        Specification<PlanTarifa> spec = Specification.where(null);
        
        // Filtro por texto (nombre o descripción)
        if (filtro != null && !filtro.trim().isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> {
                String filtroLike = "%" + filtro.toLowerCase() + "%";
                return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("nombre")), filtroLike),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("descripcion")), filtroLike)
                );
            });
        }
        
        // Filtro por estado activo
        if (activo != null) {
            spec = spec.and((root, query, criteriaBuilder) -> 
                criteriaBuilder.equal(root.get("activo"), activo)
            );
        }
        
        return planTarifaRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public PlanTarifa buscarPorUuid(UUID uuid) {
        return planTarifaRepository.findByUuid(uuid)
            .orElseThrow(() -> new RuntimeException("Plan de tarifa no encontrado: " + uuid));
    }

    @Transactional(readOnly = true)
    public Optional<PlanTarifa> buscarPorNombre(String nombre) {
        return planTarifaRepository.findByNombre(nombre);
    }

    // ==================== GESTIÓN DE PLANES ====================

    public PlanTarifa crear(PlanTarifaDTO planTarifaDTO) {
        log.info("Creando nuevo plan de tarifa: {}", planTarifaDTO.getNombre());
        
        // Validar que no exista un plan con el mismo nombre
        if (planTarifaRepository.findByNombre(planTarifaDTO.getNombre()).isPresent()) {
            throw new RuntimeException("Ya existe un plan de tarifa con el nombre: " + planTarifaDTO.getNombre());
        }
        
        PlanTarifa planTarifa = PlanTarifa.builder()
            .nombre(planTarifaDTO.getNombre().trim())
            .descripcion(planTarifaDTO.getDescripcion() != null ? planTarifaDTO.getDescripcion().trim() : null)
            .activo(true)
            .build();
        
        return planTarifaRepository.save(planTarifa);
    }

    public PlanTarifa actualizar(UUID uuid, PlanTarifaDTO planTarifaDTO) {
        log.info("Actualizando plan de tarifa: {}", uuid);
        
        PlanTarifa planTarifa = buscarPorUuid(uuid);
        
        // Validar que no exista otro plan con el mismo nombre
        Optional<PlanTarifa> planExistente = planTarifaRepository.findByNombre(planTarifaDTO.getNombre());
        if (planExistente.isPresent() && !planExistente.get().getUuid().equals(uuid)) {
            throw new RuntimeException("Ya existe un plan de tarifa con el nombre: " + planTarifaDTO.getNombre());
        }
        
        planTarifa.setNombre(planTarifaDTO.getNombre().trim());
        planTarifa.setDescripcion(planTarifaDTO.getDescripcion() != null ? planTarifaDTO.getDescripcion().trim() : null);
        
        return planTarifaRepository.save(planTarifa);
    }

    public void cambiarEstado(UUID uuid) {
        log.info("Cambiando estado del plan de tarifa: {}", uuid);
        
        PlanTarifa planTarifa = buscarPorUuid(uuid);
        
        // Si se va a desactivar, verificar que no tenga contratos activos
        if (planTarifa.getActivo()) {
            if (!planTarifaRepository.puedeSerDesactivado(uuid)) {
                long zonasActivas = planTarifaRepository.countZonasActivasByPlanTarifa(uuid);
                throw new RuntimeException("No se puede desactivar el plan porque tiene " + zonasActivas + " zonas de contratos activas");
            }
        }
        
        planTarifa.setActivo(!planTarifa.getActivo());
        planTarifaRepository.save(planTarifa);
    }

    public void eliminar(UUID uuid) {
        log.info("Eliminando plan de tarifa: {}", uuid);
        
        PlanTarifa planTarifa = buscarPorUuid(uuid);
        
        // Verificar que puede ser eliminado
        if (!planTarifaRepository.puedeSerEliminado(uuid)) {
            throw new RuntimeException("No se puede eliminar el plan porque tiene tarifas o zonas de contratos asociadas");
        }
        
        planTarifaRepository.delete(planTarifa);
    }

    // ==================== CONSULTAS ESPECÍFICAS ====================

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerResumenGeneral() {
        log.info("Obteniendo resumen general de planes de tarifa");
        
        Map<String, Object> resumen = new HashMap<>();
        
        long totalPlanes = planTarifaRepository.count();
        long planesActivos = planTarifaRepository.countByActivo(true);
        long planesInactivos = totalPlanes - planesActivos;
        long totalTarifas = tarifaRepository.count();
        long totalZonasContratos = contratoZonaRepository.count();
        
        resumen.put("totalPlanes", totalPlanes);
        resumen.put("totalTarifas", totalTarifas);
        resumen.put("totalZonasContratos", totalZonasContratos);
        resumen.put("planesActivos", planesActivos);
        resumen.put("planesInactivos", planesInactivos);
        
        // Planes con más tarifas
        List<Object[]> planesConTarifas = planTarifaRepository.findPlanesConMasTarifas();
        Map<String, Long> distribucionTarifas = planesConTarifas.stream()
            .limit(5) // Top 5
            .collect(Collectors.toMap(
                obj -> (String) obj[0], // nombre del plan
                obj -> (Long) obj[1]    // cantidad de tarifas
            ));
        resumen.put("planesPorTarifas", distribucionTarifas);
        
        // Planes con más contratos-zonas
        List<Object[]> planesConZonas = planTarifaRepository.findPlanesConMasContratosZonas();
        Map<String, Long> distribucionZonas = planesConZonas.stream()
            .limit(5) // Top 5
            .collect(Collectors.toMap(
                obj -> (String) obj[0], // nombre del plan
                obj -> (Long) obj[1]    // cantidad de zonas
            ));
        resumen.put("planesPorZonas", distribucionZonas);
        
        return resumen;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDetallePlan(UUID uuid) {
        log.info("Obteniendo detalle del plan: {}", uuid);
        
        PlanTarifa planTarifa = buscarPorUuid(uuid);
        
        Map<String, Object> detalle = new HashMap<>();
        detalle.put("plan", convertirADTO(planTarifa));
        
        // Estadísticas de tarifas
        detalle.put("totalTarifas", planTarifa.getCantidadTarifas());
        detalle.put("tarifasActivas", planTarifa.getCantidadTarifasActivas());
        
        // Estadísticas de contratos-zonas
        detalle.put("totalZonasContratos", planTarifa.getCantidadContratosZonas());
        detalle.put("zonasActivasContratos", planTarifa.getCantidadContratosZonasActivas());
        
        // Información de contratos únicos que usan este plan
        long contratosUnicos = planTarifaRepository.countContratosActivosByPlanTarifa(uuid);
        detalle.put("contratosUnicos", contratosUnicos);
        
        // Verificaciones de estado
        detalle.put("puedeSerEliminado", planTarifaRepository.puedeSerEliminado(uuid));
        detalle.put("puedeSerDesactivado", planTarifaRepository.puedeSerDesactivado(uuid));
        
        return detalle;
    }

    @Transactional(readOnly = true)
    public List<PlanTarifa> buscarPlanesConTarifas() {
        log.info("Buscando planes que tienen tarifas asociadas");
        return planTarifaRepository.findPlanesConTarifas();
    }

    @Transactional(readOnly = true)
    public List<PlanTarifa> buscarPlanesSinTarifas() {
        log.info("Buscando planes que no tienen tarifas asociadas");
        return planTarifaRepository.findPlanesSinTarifas();
    }

    @Transactional(readOnly = true)
    public List<PlanTarifa> buscarPlanesConContratosZonas() {
        log.info("Buscando planes que tienen contratos-zonas asociadas");
        return planTarifaRepository.findPlanesConContratosZonas();
    }

    @Transactional(readOnly = true)
    public List<PlanTarifa> buscarPlanesSinContratosZonas() {
        log.info("Buscando planes que no tienen contratos-zonas asociadas");
        return planTarifaRepository.findPlanesSinContratosZonas();
    }

    @Transactional(readOnly = true)
    public List<PlanTarifa> buscarPlanesActivosCompletos() {
        log.info("Buscando planes activos con tarifas y contratos-zonas");
        return planTarifaRepository.findPlanesActivosCompletos();
    }

    @Transactional(readOnly = true)
    public List<Object[]> obtenerPlanesMasUtilizados() {
        log.info("Obteniendo planes más utilizados por cantidad de zonas");
        return planTarifaRepository.findPlanesMasUtilizados();
    }

    @Transactional(readOnly = true)
    public List<Object[]> obtenerEstadisticasCompletas() {
        log.info("Obteniendo estadísticas completas de todos los planes");
        return planTarifaRepository.findPlanesConEstadisticas();
    }

    // ==================== VALIDACIONES ====================

    @Transactional(readOnly = true)
    public boolean puedeSerEliminado(UUID uuid) {
        return planTarifaRepository.puedeSerEliminado(uuid);
    }

    @Transactional(readOnly = true)
    public boolean puedeSerDesactivado(UUID uuid) {
        return planTarifaRepository.puedeSerDesactivado(uuid);
    }

    @Transactional(readOnly = true)
    public long contarContratosActivos(UUID uuid) {
        return planTarifaRepository.countContratosActivosByPlanTarifa(uuid);
    }

    @Transactional(readOnly = true)
    public long contarZonasActivas(UUID uuid) {
        return planTarifaRepository.countZonasActivasByPlanTarifa(uuid);
    }

    // ==================== UTILIDADES Y CONVERSIONES ====================

    public PlanTarifaDTO convertirADTO(PlanTarifa planTarifa) {
        return PlanTarifaDTO.builder()
            .uuid(planTarifa.getUuid())
            .nombre(planTarifa.getNombre())
            .descripcion(planTarifa.getDescripcion())
            .activo(planTarifa.getActivo())
            .fechaCreacion(planTarifa.getFechaCreacion())
            .totalTarifas(planTarifa.getCantidadTarifas())
            .totalContratos(planTarifa.getCantidadContratosZonas())
            .build();
    }

    public PlanTarifaDTO convertirADTOConEstadisticas(PlanTarifa planTarifa) {
        PlanTarifaDTO dto = convertirADTO(planTarifa);
        
        // Agregar estadísticas detalladas
        dto.setTotalTarifas(planTarifa.getCantidadTarifas());
        dto.setTarifasActivas((int) planTarifa.getCantidadTarifasActivas());
        dto.setTotalContratos(planTarifa.getCantidadContratosZonas());
        dto.setContratosActivos((int) planTarifa.getCantidadContratosZonasActivas());
        
        // Indicadores de estado
        dto.setPuedeSerEliminado(puedeSerEliminado(planTarifa.getUuid()));
        dto.setPuedeSerDesactivado(puedeSerDesactivado(planTarifa.getUuid()));
        dto.setTieneUsoActivo(planTarifa.getCantidadContratosZonasActivas() > 0);
        
        return dto;
    }

    public List<PlanTarifaDTO> convertirADTOs(List<PlanTarifa> planes) {
        return planes.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
    }

    public List<PlanTarifaDTO> convertirADTOsConEstadisticas(List<PlanTarifa> planes) {
        return planes.stream()
            .map(this::convertirADTOConEstadisticas)
            .collect(Collectors.toList());
    }

    // ==================== BÚSQUEDA Y FILTRADO ====================

    @Transactional(readOnly = true)
    public List<PlanTarifa> buscarPorNombreParcial(String nombre) {
        log.info("Buscando planes por nombre parcial: {}", nombre);
        return planTarifaRepository.buscarPorNombreParcial(nombre);
    }

    @Transactional(readOnly = true)
    public List<PlanTarifa> buscarActivosParaSeleccion() {
        log.info("Obteniendo planes activos para selección en formularios");
        return planTarifaRepository.findByActivoTrueOrderByNombreAsc();
    }

    // ==================== VALIDACIONES DE NEGOCIO ====================

    public void validarParaEliminacion(UUID uuid) {
        if (!puedeSerEliminado(uuid)) {
            PlanTarifa plan = buscarPorUuid(uuid);
            StringBuilder mensaje = new StringBuilder("No se puede eliminar el plan '")
                .append(plan.getNombre()).append("' porque tiene:");
            
            if (plan.tieneTarifas()) {
                mensaje.append("\n- ").append(plan.getCantidadTarifas()).append(" tarifas asociadas");
            }
            
            if (plan.tieneContratosZonas()) {
                mensaje.append("\n- ").append(plan.getCantidadContratosZonas()).append(" zonas de contratos asociadas");
            }
            
            throw new RuntimeException(mensaje.toString());
        }
    }

    public void validarParaDesactivacion(UUID uuid) {
        if (!puedeSerDesactivado(uuid)) {
            long zonasActivas = contarZonasActivas(uuid);
            long contratosActivos = contarContratosActivos(uuid);
            
            throw new RuntimeException(String.format(
                "No se puede desactivar el plan porque tiene %d zonas activas en %d contratos activos",
                zonasActivas, contratosActivos
            ));
        }
    }

    // ==================== OPERACIONES MASIVAS ====================

    @Transactional
    public int activarPlanesMasivo(List<UUID> uuids) {
        log.info("Activando {} planes masivamente", uuids.size());
        
        int activados = 0;
        for (UUID uuid : uuids) {
            try {
                PlanTarifa plan = buscarPorUuid(uuid);
                if (!plan.getActivo()) {
                    plan.setActivo(true);
                    planTarifaRepository.save(plan);
                    activados++;
                }
            } catch (Exception e) {
                log.warn("Error activando plan {}: {}", uuid, e.getMessage());
            }
        }
        
        return activados;
    }

    @Transactional
    public int desactivarPlanesMasivo(List<UUID> uuids) {
        log.info("Desactivando {} planes masivamente", uuids.size());
        
        int desactivados = 0;
        for (UUID uuid : uuids) {
            try {
                if (puedeSerDesactivado(uuid)) {
                    PlanTarifa plan = buscarPorUuid(uuid);
                    if (plan.getActivo()) {
                        plan.setActivo(false);
                        planTarifaRepository.save(plan);
                        desactivados++;
                    }
                }
            } catch (Exception e) {
                log.warn("Error desactivando plan {}: {}", uuid, e.getMessage());
            }
        }
        
        return desactivados;
    }

    // ==================== REPORTES Y ESTADÍSTICAS ====================

    @Transactional(readOnly = true)
    public Map<String, Object> generarReporteUso() {
        log.info("Generando reporte de uso de planes de tarifa");
        
        Map<String, Object> reporte = new HashMap<>();
        
        // Estadísticas generales
        reporte.put("totalPlanes", planTarifaRepository.count());
        reporte.put("planesActivos", planTarifaRepository.countByActivo(true));
        reporte.put("planesConTarifas", planTarifaRepository.findPlanesConTarifas().size());
        reporte.put("planesConContratos", planTarifaRepository.findPlanesConContratosZonas().size());
        
        // Top planes más utilizados
        List<Object[]> planesMasUtilizados = planTarifaRepository.findPlanesMasUtilizados();
        reporte.put("planesMasUtilizados", planesMasUtilizados.stream()
            .limit(10)
            .collect(Collectors.toList()));
        
        // Distribución de uso
        List<Object[]> distribucionTarifas = planTarifaRepository.findPlanesConMasTarifas();
        reporte.put("distribucionTarifas", distribucionTarifas.stream()
            .limit(10)
            .collect(Collectors.toList()));
        
        // Planes que requieren atención
        List<PlanTarifa> planesSinUso = planTarifaRepository.findPlanesSinContratosZonas();
        reporte.put("planesSinUso", planesSinUso.stream()
            .filter(p -> p.getActivo())
            .map(this::convertirADTO)
            .collect(Collectors.toList()));
        
        return reporte;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerMetricasComparativas() {
        log.info("Obteniendo métricas comparativas de planes");
        
        Map<String, Object> metricas = new HashMap<>();
        
        // Comparación de eficiencia
        List<Object[]> estadisticas = planTarifaRepository.findPlanesConEstadisticas();
        
        List<Map<String, Object>> comparacion = estadisticas.stream()
            .map(stat -> {
                Map<String, Object> planMetrica = new HashMap<>();
                planMetrica.put("uuid", stat[0]);
                planMetrica.put("nombre", stat[1]);
                planMetrica.put("activo", stat[2]);
                planMetrica.put("totalTarifas", stat[3]);
                planMetrica.put("tarifasActivas", stat[4]);
                planMetrica.put("totalZonas", stat[5]);
                planMetrica.put("zonasActivas", stat[6]);
                planMetrica.put("contratosActivos", stat[7]);
                
                // Calcular ratios de eficiencia
                Long totalTarifas = (Long) stat[3];
                Long totalZonas = (Long) stat[5];
                if (totalTarifas > 0 && totalZonas > 0) {
                    double ratioUso = totalZonas.doubleValue() / totalTarifas.doubleValue();
                    planMetrica.put("ratioUsoTarifas", ratioUso);
                } else {
                    planMetrica.put("ratioUsoTarifas", 0.0);
                }
                
                return planMetrica;
            })
            .collect(Collectors.toList());
        
        metricas.put("comparacionPlanes", comparacion);
        
        return metricas;
    }

    // ==================== MÉTODOS DE UTILIDAD ADICIONALES ====================

    @Transactional(readOnly = true)
    public boolean existePlanConNombre(String nombre) {
        return planTarifaRepository.findByNombre(nombre).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean existePlanConNombre(String nombre, UUID excludeUuid) {
        Optional<PlanTarifa> planExistente = planTarifaRepository.findByNombre(nombre);
        return planExistente.isPresent() && !planExistente.get().getUuid().equals(excludeUuid);
    }

    @Transactional(readOnly = true)
    public long contarPlanesPorEstado(boolean activo) {
        return planTarifaRepository.countByActivo(activo);
    }

    @Transactional(readOnly = true)
    public List<PlanTarifa> buscarPlanesOrdenados(String campo, boolean ascendente) {
        // Implementar ordenamiento personalizado según necesidades
        return planTarifaRepository.findAll();
    }

    // ==================== IMPORTACIÓN Y EXPORTACIÓN ====================

    public Map<String, Object> exportarDatosParaBackup() {
        log.info("Exportando datos de planes de tarifa para backup");
        
        Map<String, Object> backup = new HashMap<>();
        List<PlanTarifa> planes = planTarifaRepository.findAll();
        
        backup.put("planes", planes.stream()
            .map(this::convertirADTOConEstadisticas)
            .collect(Collectors.toList()));
        backup.put("fechaExportacion", java.time.LocalDateTime.now());
        backup.put("totalRegistros", planes.size());
        
        return backup;
    }

    @Transactional
    public int importarPlanesDesdeBackup(List<PlanTarifaDTO> planesDTO) {
        log.info("Importando {} planes desde backup", planesDTO.size());
        
        int importados = 0;
        for (PlanTarifaDTO dto : planesDTO) {
            try {
                if (!existePlanConNombre(dto.getNombre())) {
                    crear(dto);
                    importados++;
                }
            } catch (Exception e) {
                log.warn("Error importando plan {}: {}", dto.getNombre(), e.getMessage());
            }
        }
        
        return importados;
    }
}