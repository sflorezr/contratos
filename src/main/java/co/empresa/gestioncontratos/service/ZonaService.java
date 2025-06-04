package co.empresa.gestioncontratos.service;

import co.empresa.gestioncontratos.dto.*;
import co.empresa.gestioncontratos.entity.Contrato;
import co.empresa.gestioncontratos.entity.PlanTarifa;
import co.empresa.gestioncontratos.entity.Zona;
import co.empresa.gestioncontratos.entity.Sector;
import co.empresa.gestioncontratos.repository.ContratoRepository;
import co.empresa.gestioncontratos.repository.SectorRepository;
import co.empresa.gestioncontratos.repository.ZonaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ZonaService {
    
    private final ZonaRepository zonaRepository;
    private final SectorRepository sectorRepository;
    private final ContratoRepository contratoRepository;
    
    /**
     * Crear una nueva zona
     */
    public ZonaDTO crear(ZonaDTO crearZonaDTO) {
        log.info("Creando nueva zona: {}", crearZonaDTO.getNombre());
        
        // Validar que el código no esté duplicado
        if (zonaRepository.existsByCodigo(crearZonaDTO.getCodigo())) {
            throw new IllegalArgumentException("Ya existe una zona con el código: " + crearZonaDTO.getCodigo());
        }
        
                
        // Crear la zona
        Zona zona = Zona.builder()
            .nombre(crearZonaDTO.getNombre())
            .codigo(crearZonaDTO.getCodigo())
            .descripcion(crearZonaDTO.getDescripcion())
            .activo(crearZonaDTO.getActivo())
            .build();
        
        zona = zonaRepository.save(zona);
        log.info("Zona creada exitosamente con ID: {}", zona.getId());
        
        return convertirADTO(zona);
    }
    
    /**
     * Actualizar una zona existente
     */
    public ZonaDTO actualizar(UUID uuid, ZonaDTO actualizarZonaDTO) {
        log.info("Actualizando zona con UUID: {}", uuid);
        
        Zona zona = zonaRepository.findByUuid(uuid)
            .orElseThrow(() -> new EntityNotFoundException("Zona no encontrada"));
        
        // Verificar que la zona pueda ser editada
        if (!zona.puedeSerEditada()) {
            throw new IllegalStateException("La zona no puede ser editada en su estado actual");
        }
        
        // Actualizar campos si se proporcionan
        if (actualizarZonaDTO.getNombre() != null) {
            zona.setNombre(actualizarZonaDTO.getNombre());
        }
        
        if (actualizarZonaDTO.getDescripcion() != null) {
            zona.setDescripcion(actualizarZonaDTO.getDescripcion());
        }
        
        if (actualizarZonaDTO.getActivo() != null) {
            // Si se está desactivando, verificar que no tenga sectores activos
            zona.setActivo(actualizarZonaDTO.getActivo());
        }
        
        
        zona = zonaRepository.save(zona);
        log.info("Zona actualizada exitosamente");
        
        return convertirADTO(zona);
    }
    
    /**
     * Buscar zona por UUID
     */
    @Transactional(readOnly = true)
    public ZonaDTO buscarPorUuid(UUID uuid) {
        Zona zona = zonaRepository.findByUuid(uuid)
            .orElseThrow(() -> new EntityNotFoundException("Zona no encontrada"));
        return convertirADTO(zona);
    }
 
    
     @Transactional(readOnly = true)
    public Zona buscarPorUuidSF(UUID uuid) {
        Zona zona = zonaRepository.findByUuid(uuid)
            .orElseThrow(() -> new EntityNotFoundException("Zona no encontrada"));
        return zona;
    }
       
    /**
     * Buscar zona por ID
     */
    @Transactional(readOnly = true)
    public Zona buscarPorId(Long id) {
        return zonaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Zona no encontrada"));
    }
    
    /**
     * Listar todas las zonas de un contrato
     */
    @Transactional(readOnly = true)
    public List<ZonaDTO> listarPorContrato(Long contratoId) {
        List<Zona> zonas = zonaRepository.findByContratoIdWithSectores(contratoId);
        return zonas.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Listar zonas activas de un contrato
     */
    
    /**
     * Eliminar una zona
     */
    public void eliminar(UUID uuid) {
        log.info("Eliminando zona con UUID: {}", uuid);
        
        Zona zona = zonaRepository.findByUuid(uuid)
            .orElseThrow(() -> new EntityNotFoundException("Zona no encontrada"));
        
        
        zonaRepository.delete(zona);
        log.info("Zona eliminada exitosamente");
    }
    
    /**
     * Verificar si una zona pertenece a un contrato
     */
    
    /**
     * Obtener estadísticas de una zona
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas(Long zonaId) {
        Map<String, Object> estadisticas = new HashMap<>();
        
        estadisticas.put("totalSectores", zonaRepository.contarSectores(zonaId));
        estadisticas.put("sectoresActivos", zonaRepository.contarSectoresActivos(zonaId));
        estadisticas.put("totalPredios", sectorRepository.contarPrediosPorZona(zonaId));
        estadisticas.put("prediosAsignados", sectorRepository.contarPrediosAsignadosPorZona(zonaId));
        
        long totalPredios = (long) estadisticas.get("totalPredios");
        long prediosAsignados = (long) estadisticas.get("prediosAsignados");
        
        double porcentajeAsignacion = totalPredios > 0 ? 
            (double) prediosAsignados / totalPredios * 100 : 0;
        
        estadisticas.put("porcentajeAsignacion", porcentajeAsignacion);
        estadisticas.put("prediosSinAsignar", totalPredios - prediosAsignados);
        
        return estadisticas;
    }
 
    /**
     * Convertir entidad a DTO
     */
    private ZonaDTO convertirADTO(Zona zona) {
        return ZonaDTO.builder()
            .id(zona.getId())
            .uuid(zona.getUuid())
            .nombre(zona.getNombre())
            .codigo(zona.getCodigo())
            .descripcion(zona.getDescripcion())
            .activo(zona.getActivo())
            .fechaCreacion(zona.getFechaCreacion())
            .fechaActualizacion(zona.getFechaActualizacion())
            .build();
    }
    
    /**
     * Convertir lista de sectores a DTO
     */
    private List<SectorDTO> convertirSectoresADTO(List<Sector> sectores) {
        if (sectores == null || sectores.isEmpty()) {
            return new ArrayList<>();
        }
        
        return sectores.stream()
            .map(sector -> SectorDTO.builder()
                .codigo(sector.getCodigo())
                .nombre(sector.getNombre())
                .activo(sector.getActivo())
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * Obtener zonas para select/dropdown
     */
    @Transactional(readOnly = true)
    public List<Zona> listarParaSelect(Long contratoId) {
        List<Zona> zonas = zonaRepository.findAll();
        return zonas.stream()
            .map(zona -> Zona.builder()
                .id(zona.getId())
                .nombre(zona.getNombre())
                .codigo(zona.getCodigo())
                .build())
            .collect(Collectors.toList());
    }
    public void cambiarEstado(UUID uuid) {
        log.info("Cambiando estado del plan de la zona: {}", uuid);
        
        Zona zona = buscarPorUuidSF(uuid);
        
        // Si se va a desactivar, verificar que no tenga contratos activos
        if (zona.getActivo()) {
            zona.setActivo(false);
        }else{
            zona.setActivo(true);
        }
        zonaRepository.save(zona);
    }
    // Buscar zonas con filtros y paginación
    public Page<Zona> buscarConFiltros(String filtro, Boolean activo, Long contratoId, Pageable pageable) {
        // Implementar búsqueda con filtros
        if (filtro != null && !filtro.isEmpty()) {
            if (activo != null) {
                if (contratoId != null) {
                   return zonaRepository.findAll(pageable);
                } else {
                    return zonaRepository.findAll(pageable);
                }
            } else if (contratoId != null) {
                return zonaRepository.findAll(pageable);
            } else {
                return zonaRepository.findAll(pageable);
            }
        } else if (activo != null) {
            return zonaRepository.findAll(pageable);
        } else if (contratoId != null) {
            return zonaRepository.findAll(pageable);
        } else {
            return zonaRepository.findAll(pageable);
        }
    }

    // Obtener resumen general (estadísticas)
    @Transactional(readOnly = true)
    public Map<String, Long> obtenerResumenGeneral() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalZonas", zonaRepository.count());
        stats.put("zonasActivas", zonaRepository.contarZonasEstado(true));
        stats.put("zonasInactivas", zonaRepository.contarZonasEstado(false));
        stats.put("totalSectores", sectorRepository.count());
        return stats;
    }

    // Listar todas las zonas con estadísticas
    @Transactional(readOnly = true)
    public List<ZonaDTO> listarTodasConEstadisticas() {
        List<Zona> zonas = zonaRepository.findAll();
        return zonas.stream()
            .map(zona -> {
                ZonaDTO dto = convertirADTO(zona);
                Map<String, Object> stats = obtenerEstadisticas(zona.getId());
                dto.setEstadisticas(stats);
                return dto;
            })
            .collect(Collectors.toList());
    }

    // Listar zonas activas
    @Transactional(readOnly = true)
    public List<ZonaDTO> listarActivas() {
        List<Zona> zonas = zonaRepository.findByActivoTrueOrderByNombreAsc();
        return zonas.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
    }

    // Verificar si existe código
    @Transactional(readOnly = true)
    public boolean existeCodigo(String codigo) {
        return zonaRepository.existsByCodigo(codigo);
    }

    // Buscar zona por código
    @Transactional(readOnly = true)
    public ZonaDTO buscarPorCodigo(String codigo) {
        Zona zona = zonaRepository.findByCodigo(codigo)
            .orElseThrow(() -> new EntityNotFoundException("Zona no encontrada"));
        return convertirADTO(zona);
    }    
}