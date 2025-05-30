package co.empresa.gestioncontratos.service;

import co.empresa.gestioncontratos.dto.ServicioDTO;
import co.empresa.gestioncontratos.entity.Actividad;
import co.empresa.gestioncontratos.entity.Servicio;
import co.empresa.gestioncontratos.entity.Tarifa;
import co.empresa.gestioncontratos.repository.ActividadRepository;
import co.empresa.gestioncontratos.repository.ServicioRepository;
import co.empresa.gestioncontratos.repository.TarifaRepository;
import co.empresa.gestioncontratos.repository.PlanTarifaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class ServicioService {

    private final ServicioRepository servicioRepository;
    private final TarifaRepository tarifaRepository;
    private final ActividadRepository actividadRepository;

    // ==================== CONSULTAS ====================

    @Transactional(readOnly = true)
    public List<Servicio> listarTodos() {
        log.info("Listando todos los servicios");
        return servicioRepository.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Page<Servicio> listarTodosPaginado(Pageable pageable) {
        log.info("Listando servicios paginados: {}", pageable);
        return servicioRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Servicio> listarActivos() {
        log.info("Listando servicios activos");
        return servicioRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Page<Servicio> listarActivosPaginado(Pageable pageable) {
        log.info("Listando servicios activos paginados: {}", pageable);
        return servicioRepository.findByActivoTrue(pageable);
    }

    @Transactional(readOnly = true)
    public Servicio buscarPorId(Long id) {
        log.info("Buscando servicio por ID: {}", id);
        return servicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado con ID: " + id));
    }

    @Transactional(readOnly = true)
    public Servicio buscarPorUuid(UUID uuid) {
        log.info("Buscando servicio por UUID: {}", uuid);
        return servicioRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado con UUID: " + uuid));
    }

    @Transactional(readOnly = true)
    public Servicio buscarPorNombre(String nombre) {
        log.info("Buscando servicio por nombre: {}", nombre);
        return servicioRepository.findByNombre(nombre)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado con nombre: " + nombre));
    }

    @Transactional(readOnly = true)
    public boolean existePorNombre(String nombre) {
        return servicioRepository.existsByNombre(nombre);
    }

    // ==================== GESTIÓN DE SERVICIOS ====================

    public Servicio crear(ServicioDTO servicioDTO) {
        log.info("Creando nuevo servicio: {}", servicioDTO.getNombre());
        
        // Validar que no exista el nombre
        if (servicioRepository.existsByNombre(servicioDTO.getNombre())) {
            throw new RuntimeException("Ya existe un servicio con el nombre: " + servicioDTO.getNombre());
        }
        
        Servicio servicio = Servicio.builder()
                .nombre(servicioDTO.getNombre())
                .descripcion(servicioDTO.getDescripcion())
                .activo(servicioDTO.getActivo() != null ? servicioDTO.getActivo() : true)
                .build();
        
        return servicioRepository.save(servicio);
    }

    public Servicio actualizar(UUID uuid, ServicioDTO servicioDTO) {
        log.info("Actualizando servicio: {}", uuid);
        
        Servicio servicio = buscarPorUuid(uuid);
        
        // Validar nombre único si cambió
        if (!servicio.getNombre().equals(servicioDTO.getNombre()) &&
                servicioRepository.existsByNombre(servicioDTO.getNombre())) {
            throw new RuntimeException("Ya existe otro servicio con el nombre: " + servicioDTO.getNombre());
        }
        
        servicio.setNombre(servicioDTO.getNombre());
        servicio.setDescripcion(servicioDTO.getDescripcion());
        
        if (servicioDTO.getActivo() != null) {
            servicio.setActivo(servicioDTO.getActivo());
        }
        
        return servicioRepository.save(servicio);
    }

    public Servicio cambiarEstado(UUID uuid, boolean activo) {
        log.info("Cambiando estado del servicio {} a {}", uuid, activo ? "activo" : "inactivo");
        
        Servicio servicio = buscarPorUuid(uuid);
        servicio.setActivo(activo);
        return servicioRepository.save(servicio);
    }

    public void eliminar(UUID uuid) {
        log.info("Eliminando servicio: {}", uuid);
        
        Servicio servicio = buscarPorUuid(uuid);
        
        // Verificar si tiene tarifas o actividades asociadas
        if (!servicio.getTarifas().isEmpty() || !servicio.getActividades().isEmpty()) {
            throw new RuntimeException("No se puede eliminar el servicio porque tiene tarifas o actividades asociadas");
        }
        
        servicioRepository.delete(servicio);
    }

    // ==================== GESTIÓN DE RELACIONES ====================

    @Transactional(readOnly = true)
    public List<Tarifa> listarTarifasPorServicio(UUID servicioUuid) {
        log.info("Listando tarifas del servicio: {}", servicioUuid);
        Servicio servicio = buscarPorUuid(servicioUuid);
        return tarifaRepository.findByServicioOrderByPrecioUrbanoAsc(servicio);
    }

    @Transactional(readOnly = true)
    public List<Actividad> listarActividadesPorServicio(UUID servicioUuid) {
        log.info("Listando actividades del servicio: {}", servicioUuid);
        Servicio servicio = buscarPorUuid(servicioUuid);
        return actividadRepository.findByServicioOrderByNombreAsc(servicio);
    }

    // ==================== CONSULTAS Y ESTADÍSTICAS ====================

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas() {
        long totalServicios = servicioRepository.count();
        long serviciosActivos = servicioRepository.countByActivoTrue();
        long serviciosInactivos = totalServicios - serviciosActivos;
        
        return Map.of(
            "totalServicios", totalServicios,
            "serviciosActivos", serviciosActivos,
            "serviciosInactivos", serviciosInactivos,
            "porcentajeActivos", totalServicios > 0 ? (serviciosActivos * 100.0 / totalServicios) : 0
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDetallesServicio(UUID uuid) {
        Servicio servicio = buscarPorUuid(uuid);
        
        long cantidadTarifas = tarifaRepository.countByServicio(servicio);
        long cantidadActividades = actividadRepository.countByServicio(servicio);
        
        return Map.of(
            "uuid", servicio.getUuid(),
            "nombre", servicio.getNombre(),
            "descripcion", servicio.getDescripcion() != null ? servicio.getDescripcion() : "",
            "activo", servicio.getActivo(),
            "fechaCreacion", servicio.getFechaCreacion(),
            "cantidadTarifas", cantidadTarifas,
            "cantidadActividades", cantidadActividades
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarServiciosResumen() {
        List<Servicio> servicios = listarTodos();
        
        return servicios.stream()
            .map(servicio -> {
                Map<String, Object> resumen = new HashMap<>();
                resumen.put("uuid", servicio.getUuid());
                resumen.put("nombre", servicio.getNombre());
                resumen.put("activo", servicio.getActivo());
                resumen.put("cantidadTarifas", tarifaRepository.countByServicio(servicio));
                resumen.put("cantidadActividades", actividadRepository.countByServicio(servicio));
                
                return resumen;
            })
            .collect(Collectors.toList());
    }
}