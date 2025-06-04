package co.empresa.gestioncontratos.service;

import co.empresa.gestioncontratos.dto.PlanTarifaDTO;
import co.empresa.gestioncontratos.entity.PlanTarifa;
import co.empresa.gestioncontratos.repository.PlanTarifaRepository;
import co.empresa.gestioncontratos.repository.TarifaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
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

    // ==================== CONSULTAS ====================

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
            long contratosActivos = planTarifaRepository.countContratosActivosByPlanTarifa(uuid);
            if (contratosActivos > 0) {
                throw new RuntimeException("No se puede desactivar el plan porque tiene " + contratosActivos + " contratos activos");
            }
        }
        
        planTarifa.setActivo(!planTarifa.getActivo());
        planTarifaRepository.save(planTarifa);
    }

    public void eliminar(UUID uuid) {
        log.info("Eliminando plan de tarifa: {}", uuid);
        
        PlanTarifa planTarifa = buscarPorUuid(uuid);
        
        // Verificar que no tenga tarifas asociadas
        if (planTarifa.getTarifas() != null && !planTarifa.getTarifas().isEmpty()) {
            throw new RuntimeException("No se puede eliminar el plan porque tiene tarifas asociadas");
        }
        
        // Verificar que no tenga contratos asociados
        if (planTarifa.getContratos() != null && !planTarifa.getContratos().isEmpty()) {
            throw new RuntimeException("No se puede eliminar el plan porque tiene contratos asociados");
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
        
        resumen.put("totalPlanes", totalPlanes);
        resumen.put("totalTarifas", totalTarifas);
        resumen.put("planesActivos", planesActivos);
        resumen.put("planesInactivos", planesInactivos);
        
        // Planes con más tarifas
        List<Object[]> planesConTarifas = planTarifaRepository.findPlanesConMasTarifas();
        Map<String, Long> distribucionTarifas = planesConTarifas.stream()
            .collect(Collectors.toMap(
                obj -> (String) obj[0], // nombre del plan
                obj -> (Long) obj[1]    // cantidad de tarifas
            ));
        resumen.put("planesPorTarifas", distribucionTarifas);
        
        return resumen;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDetallePlan(UUID uuid) {
        log.info("Obteniendo detalle del plan: {}", uuid);
        
        PlanTarifa planTarifa = buscarPorUuid(uuid);
        
        Map<String, Object> detalle = new HashMap<>();
        detalle.put("plan", convertirADTO(planTarifa));
        detalle.put("totalTarifas", planTarifa.getTarifas() != null ? planTarifa.getTarifas().size() : 0);
        detalle.put("tarifasActivas", planTarifa.getTarifas() != null ? 
            planTarifa.getTarifas().stream().filter(t -> t.getActivo()).count() : 0);
        detalle.put("totalContratos", planTarifa.getContratos() != null ? planTarifa.getContratos().size() : 0);
        
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

    // ==================== UTILIDADES ====================

    public PlanTarifaDTO convertirADTO(PlanTarifa planTarifa) {
        return PlanTarifaDTO.builder()
            .uuid(planTarifa.getUuid())
            .nombre(planTarifa.getNombre())
            .descripcion(planTarifa.getDescripcion())
            .activo(planTarifa.getActivo())
            .fechaCreacion(planTarifa.getFechaCreacion())
            .totalTarifas(planTarifa.getTarifas() != null ? planTarifa.getTarifas().size() : 0)
            .totalContratos(planTarifa.getContratos() != null ? planTarifa.getContratos().size() : 0)
            .build();
    }

    public List<PlanTarifaDTO> convertirADTOs(List<PlanTarifa> planes) {
        return planes.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
    }
}