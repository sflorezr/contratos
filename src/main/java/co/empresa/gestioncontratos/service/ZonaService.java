package co.empresa.gestioncontratos.service;

import co.empresa.gestioncontratos.dto.*;
import co.empresa.gestioncontratos.entity.*;
import co.empresa.gestioncontratos.enums.EstadoContratoZona;
import co.empresa.gestioncontratos.enums.PerfilUsuario;
import co.empresa.gestioncontratos.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final ContratoZonaRepository contratoZonaRepository;
    private final PlanTarifaRepository planTarifaRepository;
    private final UsuarioRepository usuarioRepository;
    
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
            .activo(crearZonaDTO.getActivo() != null ? crearZonaDTO.getActivo() : true)
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
        
        // Validar código si se está cambiando
        if (actualizarZonaDTO.getCodigo() != null && 
            !actualizarZonaDTO.getCodigo().equals(zona.getCodigo())) {
            if (zonaRepository.existsByCodigo(actualizarZonaDTO.getCodigo())) {
                throw new IllegalArgumentException("Ya existe una zona con el código: " + actualizarZonaDTO.getCodigo());
            }
            zona.setCodigo(actualizarZonaDTO.getCodigo());
        }
        
        // Actualizar campos si se proporcionan
        if (actualizarZonaDTO.getNombre() != null) {
            zona.setNombre(actualizarZonaDTO.getNombre());
        }
        
        if (actualizarZonaDTO.getDescripcion() != null) {
            zona.setDescripcion(actualizarZonaDTO.getDescripcion());
        }
        
        if (actualizarZonaDTO.getActivo() != null) {
            // Si se está desactivando, verificar que no tenga contratos activos
            if (!actualizarZonaDTO.getActivo()) {
                long contratosActivos = contratoZonaRepository.countByZonaAndActivoTrue(zona);
                if (contratosActivos > 0) {
                    throw new IllegalStateException("No se puede desactivar la zona porque tiene " + 
                        contratosActivos + " contratos activos asignados");
                }
            }
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
        return zonaRepository.findByUuid(uuid)
            .orElseThrow(() -> new EntityNotFoundException("Zona no encontrada"));
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
     * Listar zonas de un contrato específico (a través de ContratoZona)
     */
    @Transactional(readOnly = true)
    public List<ZonaDTO> listarPorContrato(UUID contratoUuid) {
        Contrato contrato = contratoRepository.findByUuid(contratoUuid)
            .orElseThrow(() -> new EntityNotFoundException("Contrato no encontrado"));
            
        List<ContratoZona> contratoZonas = contratoZonaRepository.findByContratoAndActivoTrueOrderByFechaCreacionAsc(contrato);
        
        return contratoZonas.stream()
            .map(cz -> {
                ZonaDTO dto = convertirADTO(cz.getZona());
                // Agregar información adicional del contrato-zona
                dto.setCoordinadorZona(cz.getCoordinadorZona() != null ? 
                    cz.getCoordinadorZona().getNombreCompleto() : null);
                dto.setCoordinadorOperativo(cz.getCoordinadorOperativo() != null ? 
                    cz.getCoordinadorOperativo().getNombreCompleto() : null);
                dto.setPlanTarifa(cz.getPlanTarifa() != null ? 
                    cz.getPlanTarifa().getNombre() : null);
                dto.setEstadoEnContrato(cz.getEstado().name());
                return dto;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Listar zonas activas de un contrato
     */
    @Transactional(readOnly = true)
    public List<ZonaDTO> listarActivasPorContrato(UUID contratoUuid) {
        Contrato contrato = contratoRepository.findByUuid(contratoUuid)
            .orElseThrow(() -> new EntityNotFoundException("Contrato no encontrado"));
            
        List<ContratoZona> contratoZonas = contratoZonaRepository.findByContratoAndEstadoOrderByFechaCreacionAsc(
            contrato, EstadoContratoZona.ACTIVO);
        
        return contratoZonas.stream()
            .map(cz -> convertirADTO(cz.getZona()))
            .collect(Collectors.toList());
    }
    
    /**
     * Asignar zona a contrato con coordinadores y plan de tarifa
     */
    public ContratoZonaDTO asignarZonaAContrato(ContratoZonaDTO contratoZonaDTO) {
        log.info("Asignando zona {} a contrato {}", contratoZonaDTO.getZonaUuid(), contratoZonaDTO.getContratoUuid());
        
        // Buscar entidades
        Contrato contrato = contratoRepository.findByUuid(contratoZonaDTO.getContratoUuid())
            .orElseThrow(() -> new EntityNotFoundException("Contrato no encontrado"));
            
        Zona zona = zonaRepository.findByUuid(contratoZonaDTO.getZonaUuid())
            .orElseThrow(() -> new EntityNotFoundException("Zona no encontrada"));
            
        PlanTarifa planTarifa = planTarifaRepository.findByUuid(contratoZonaDTO.getPlanTarifaUuid())
            .orElseThrow(() -> new EntityNotFoundException("Plan de tarifa no encontrado"));
        
        // Verificar que no exista ya esta asignación
        if (contratoZonaRepository.existsByContratoAndZonaAndActivoTrue(contrato, zona)) {
            throw new IllegalArgumentException("La zona ya está asignada a este contrato");
        }
        
        // Buscar coordinadores si se proporcionan
        Usuario coordinadorZona = null;
        if (contratoZonaDTO.getCoordinadorZonaUuid() != null) {
            coordinadorZona = usuarioRepository.findByUuid(contratoZonaDTO.getCoordinadorZonaUuid())
                .orElseThrow(() -> new EntityNotFoundException("Coordinador de zona no encontrado"));
        }
        
        Usuario coordinadorOperativo = null;
        if (contratoZonaDTO.getCoordinadorOperativoUuid() != null) {
            coordinadorOperativo = usuarioRepository.findByUuid(contratoZonaDTO.getCoordinadorOperativoUuid())
                .orElseThrow(() -> new EntityNotFoundException("Coordinador operativo no encontrado"));
        }
        
        // Crear la asignación
        ContratoZona contratoZona = ContratoZona.builder()
            .contrato(contrato)
            .zona(zona)
            .planTarifa(planTarifa)
            .coordinadorZona(coordinadorZona)
            .coordinadorOperativo(coordinadorOperativo)
            .estado(EstadoContratoZona.ACTIVO)
            .activo(true)
            .build();
        
        contratoZona = contratoZonaRepository.save(contratoZona);
        log.info("Zona asignada exitosamente al contrato");
        
        return convertirContratoZonaADTO(contratoZona);
    }
    
    /**
     * Actualizar asignación de zona en contrato
     */
    public ContratoZonaDTO actualizarAsignacionZona(UUID contratoZonaUuid, ContratoZonaDTO actualizacionDTO) {
        log.info("Actualizando asignación de zona: {}", contratoZonaUuid);
        
        ContratoZona contratoZona = contratoZonaRepository.findByUuid(contratoZonaUuid)
            .orElseThrow(() -> new EntityNotFoundException("Asignación de zona no encontrada"));
        
        // Actualizar plan de tarifa si se proporciona
        if (actualizacionDTO.getPlanTarifaUuid() != null) {
            PlanTarifa planTarifa = planTarifaRepository.findByUuid(actualizacionDTO.getPlanTarifaUuid())
                .orElseThrow(() -> new EntityNotFoundException("Plan de tarifa no encontrado"));
            contratoZona.setPlanTarifa(planTarifa);
        }
        
        // Actualizar coordinador de zona
        if (actualizacionDTO.getCoordinadorZonaUuid() != null) {
            Usuario coordinadorZona = usuarioRepository.findByUuid(actualizacionDTO.getCoordinadorZonaUuid())
                .orElseThrow(() -> new EntityNotFoundException("Coordinador de zona no encontrado"));
            contratoZona.setCoordinadorZona(coordinadorZona);
        }
        
        // Actualizar coordinador operativo
        if (actualizacionDTO.getCoordinadorOperativoUuid() != null) {
            Usuario coordinadorOperativo = usuarioRepository.findByUuid(actualizacionDTO.getCoordinadorOperativoUuid())
                .orElseThrow(() -> new EntityNotFoundException("Coordinador operativo no encontrado"));
            contratoZona.setCoordinadorOperativo(coordinadorOperativo);
        }
        
        // Actualizar estado si se proporciona
        if (actualizacionDTO.getEstado() != null) {
            contratoZona.setActivo(actualizacionDTO.getActivo());
        }
        
        contratoZona = contratoZonaRepository.save(contratoZona);
        log.info("Asignación de zona actualizada exitosamente");
        
        return convertirContratoZonaADTO(contratoZona);
    }
    
    /**
     * Remover zona de contrato
     */
    public void removerZonaDeContrato(UUID contratoZonaUuid) {
        log.info("Removiendo zona de contrato: {}", contratoZonaUuid);
        
        ContratoZona contratoZona = contratoZonaRepository.findByUuid(contratoZonaUuid)
            .orElseThrow(() -> new EntityNotFoundException("Asignación de zona no encontrada"));
        
        // Verificar si se puede remover (no tiene sectores con predios asignados, etc.)
        // Esta lógica dependerá de las reglas de negocio específicas
        long sectoresAsignados = sectorRepository.contarSectoresPorZona(contratoZona.getZona().getId());
        if (sectoresAsignados > 0) {
            throw new IllegalStateException("No se puede remover la zona porque tiene sectores asignados");
        }
        
        contratoZona.setActivo(false);
        contratoZonaRepository.save(contratoZona);
        
        log.info("Zona removida del contrato exitosamente");
    }
    
    /**
     * Eliminar una zona
     */
    public void eliminar(UUID uuid) {
        log.info("Eliminando zona con UUID: {}", uuid);
        
        Zona zona = zonaRepository.findByUuid(uuid)
            .orElseThrow(() -> new EntityNotFoundException("Zona no encontrada"));
        
        // Verificar que no tenga contratos asignados
        long contratosAsignados = contratoZonaRepository.countByZona(zona);
        if (contratosAsignados > 0) {
            throw new IllegalStateException("No se puede eliminar la zona porque tiene " + 
                contratosAsignados + " contratos asignados");
        }
        
        // Verificar que no tenga sectores
        long sectores = zonaRepository.contarSectores(zona.getId());
        if (sectores > 0) {
            throw new IllegalStateException("No se puede eliminar la zona porque tiene " + 
                sectores + " sectores asignados");
        }
        
        zonaRepository.delete(zona);
        log.info("Zona eliminada exitosamente");
    }
    
    /**
     * Verificar si una zona pertenece a un contrato
     */
    @Transactional(readOnly = true)
    public boolean perteneceAContrato(UUID zonaUuid, UUID contratoUuid) {
        return contratoZonaRepository.existsActiveByContratoUuidAndZonaUuid(contratoUuid, zonaUuid);
    }
    
    /**
     * Obtener estadísticas de una zona
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas(Long zonaId) {
        Map<String, Object> estadisticas = new HashMap<>();
        
        Zona zona = buscarPorId(zonaId);
        
        estadisticas.put("totalSectores", zonaRepository.contarSectores(zonaId));
        estadisticas.put("sectoresActivos", zonaRepository.contarSectoresActivos(zonaId));
        estadisticas.put("totalPredios", sectorRepository.contarPrediosPorZona(zonaId));
        estadisticas.put("prediosAsignados", sectorRepository.contarPrediosAsignadosPorZona(zonaId));
        
        // Estadísticas de contratos
        estadisticas.put("totalContratos", contratoZonaRepository.countByZona(zona));
        estadisticas.put("contratosActivos", contratoZonaRepository.countByZonaAndActivoTrue(zona));
        
        long totalPredios = (long) estadisticas.get("totalPredios");
        long prediosAsignados = (long) estadisticas.get("prediosAsignados");
        
        double porcentajeAsignacion = totalPredios > 0 ? 
            (double) prediosAsignados / totalPredios * 100 : 0;
        
        estadisticas.put("porcentajeAsignacion", porcentajeAsignacion);
        estadisticas.put("prediosSinAsignar", totalPredios - prediosAsignados);
        
        return estadisticas;
    }
    
    /**
     * Obtener estadísticas de zona en contrato específico
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticasEnContrato(UUID zonaUuid, UUID contratoUuid) {
        Map<String, Object> estadisticas = new HashMap<>();
        
        ContratoZona contratoZona = contratoZonaRepository.findByContratoUuidAndZonaUuid(contratoUuid, zonaUuid)
            .orElseThrow(() -> new EntityNotFoundException("Asignación de zona no encontrada"));
        
        // Estadísticas básicas de la zona
        estadisticas.putAll(obtenerEstadisticas(contratoZona.getZona().getId()));
        
        // Información específica del contrato
        estadisticas.put("coordinadorZona", contratoZona.getNombreCoordinadorZona());
        estadisticas.put("coordinadorOperativo", contratoZona.getNombreCoordinadorOperativo());
        estadisticas.put("planTarifa", contratoZona.getNombrePlanTarifa());
        estadisticas.put("estado", contratoZona.getEstado().name());
        estadisticas.put("fechaAsignacion", contratoZona.getFechaCreacion());
        
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
     * Convertir ContratoZona a DTO
     */
    private ContratoZonaDTO convertirContratoZonaADTO(ContratoZona contratoZona) {
        return ContratoZonaDTO.builder()
            .uuid(contratoZona.getUuid())
            .contratoUuid(contratoZona.getContrato().getUuid())
            .contratoNumero(contratoZona.getContrato().getNumeroContrato())           
            .planTarifaUuid(contratoZona.getPlanTarifa().getUuid())
            .planTarifaNombre(contratoZona.getPlanTarifa().getNombre())
            .coordinadorZonaUuid(contratoZona.getCoordinadorZona() != null ? 
                contratoZona.getCoordinadorZona().getUuid() : null)
            .coordinadorZonaNombre(contratoZona.getNombreCoordinadorZona())            
            .coordinadorOperativoUuid(contratoZona.getCoordinadorOperativo() != null ? 
                contratoZona.getCoordinadorOperativo().getUuid() : null)
            .coordinadorOperativoNombre(contratoZona.getNombreCoordinadorOperativo())            
            .activo(contratoZona.getActivo())
            .fechaCreacion(contratoZona.getFechaCreacion())
            .fechaActualizacion(contratoZona.getFechaActualizacion())
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
                .uuid(sector.getUuid())
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
    public List<Zona> listarParaSelect() {
        List<Zona> zonas = zonaRepository.findByActivoTrueOrderByNombreAsc();
        return zonas.stream()
            .map(zona -> Zona.builder()
                .id(zona.getId())
                .uuid(zona.getUuid())
                .nombre(zona.getNombre())
                .codigo(zona.getCodigo())
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * Obtener zonas disponibles para asignar a un contrato
     */
    @Transactional(readOnly = true)
    public List<ZonaDTO> listarDisponiblesParaContrato(UUID contratoUuid) {
        Contrato contrato = contratoRepository.findByUuid(contratoUuid)
            .orElseThrow(() -> new EntityNotFoundException("Contrato no encontrado"));
        
        // Obtener todas las zonas activas
        List<Zona> todasLasZonas = zonaRepository.findByActivoTrueOrderByNombreAsc();
        
        // Obtener zonas ya asignadas al contrato
        List<ContratoZona> zonasAsignadas = contratoZonaRepository.findByContratoAndActivoTrueOrderByFechaCreacionAsc(contrato);
        Set<Long> idsZonasAsignadas = zonasAsignadas.stream()
            .map(cz -> cz.getZona().getId())
            .collect(Collectors.toSet());
        
        // Filtrar zonas disponibles
        return todasLasZonas.stream()
            .filter(zona -> !idsZonasAsignadas.contains(zona.getId()))
            .map(this::convertirADTO)
            .collect(Collectors.toList());
    }
    
    public void cambiarEstado(UUID uuid) {
        log.info("Cambiando estado de la zona: {}", uuid);
        
        Zona zona = buscarPorUuidSF(uuid);
        
        // Si se va a desactivar, verificar que no tenga contratos activos
        if (zona.getActivo()) {
            long contratosActivos = contratoZonaRepository.countByZonaAndActivoTrue(zona);
            if (contratosActivos > 0) {
                throw new IllegalStateException("No se puede desactivar la zona porque tiene " + 
                    contratosActivos + " contratos activos asignados");
            }
            zona.setActivo(false);
        } else {
            zona.setActivo(true);
        }
        zonaRepository.save(zona);
    }
    
    // Buscar zonas con filtros y paginación
    public Page<Zona> buscarConFiltros(String filtro, Boolean activo, Long contratoId, Pageable pageable) {
        // Si hay un contratoId específico, buscar zonas de ese contrato
        if (contratoId != null) {
            Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new EntityNotFoundException("Contrato no encontrado"));
                
            List<ContratoZona> contratoZonas = contratoZonaRepository.findByContratoAndActivoTrueOrderByFechaCreacionAsc(contrato);
            List<Zona> zonas = contratoZonas.stream()
                .map(ContratoZona::getZona)
                .collect(Collectors.toList());
                
            // Aplicar filtros adicionales
            if (filtro != null && !filtro.isEmpty()) {
                String filtroLower = filtro.toLowerCase();
                zonas = zonas.stream()
                    .filter(zona -> zona.getNombre().toLowerCase().contains(filtroLower) ||
                                   zona.getCodigo().toLowerCase().contains(filtroLower))
                    .collect(Collectors.toList());
            }
            
            if (activo != null) {
                zonas = zonas.stream()
                    .filter(zona -> zona.getActivo().equals(activo))
                    .collect(Collectors.toList());
            }
            
            // Crear página manual
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), zonas.size());
            List<Zona> paginatedZonas = start < zonas.size() ? zonas.subList(start, end) : new ArrayList<>();
            
            return new PageImpl<>(paginatedZonas, pageable, zonas.size());
        }
        
        // Búsqueda general con filtros usando métodos del repository
        return zonaRepository.findConFiltros(filtro, filtro, activo, pageable);
    }

    // Obtener resumen general (estadísticas)
    @Transactional(readOnly = true)
    public Map<String, Long> obtenerResumenGeneral() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalZonas", zonaRepository.count());
        stats.put("zonasActivas", zonaRepository.contarZonasEstado(true));
        stats.put("zonasInactivas", zonaRepository.contarZonasEstado(false));
        stats.put("totalSectores", sectorRepository.count());
        stats.put("totalAsignaciones", contratoZonaRepository.count());
        
        // Intentar obtener asignaciones activas, si el método no existe usar count general
        try {
            stats.put("asignacionesActivas", contratoZonaRepository.countByActivoTrue());
        } catch (Exception e) {
            log.warn("Método countByActivoTrue no encontrado, usando count general");
            stats.put("asignacionesActivas", contratoZonaRepository.count());
        }
        
        return stats;
    }

    // Listar todas las zonas con estadísticas
    @Transactional(readOnly = true)
    public List<ZonaDTO> listarTodasConEstadisticas() {
        List<Zona> zonas = zonaRepository.findAll();
        return zonas.stream()
            .map(zona -> {
                ZonaDTO dto = convertirADTO(zona);
                try {
                    Map<String, Object> stats = obtenerEstadisticas(zona.getId());
                    dto.setEstadisticas(stats);
                    
                    // Establecer contadores directamente
                    dto.setTotalSectores((Long) stats.get("totalSectores"));
                    dto.setSectoresActivos((Long) stats.get("sectoresActivos"));
                    dto.setTotalPredios((Long) stats.get("totalPredios"));
                    dto.setPrediosAsignados((Long) stats.get("prediosAsignados"));
                    dto.setTotalContratos((Long) stats.get("totalContratos"));
                    dto.setContratosActivos((Long) stats.get("contratosActivos"));
                } catch (Exception e) {
                    log.warn("Error al obtener estadísticas para zona {}: {}", zona.getId(), e.getMessage());
                    // Establecer valores por defecto
                    dto.setTotalSectores(0L);
                    dto.setSectoresActivos(0L);
                    dto.setTotalPredios(0L);
                    dto.setPrediosAsignados(0L);
                    dto.setTotalContratos(0L);
                    dto.setContratosActivos(0L);
                }
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
    
    /**
     * Listar contratos de una zona
     */
    @Transactional(readOnly = true)
    public List<ContratoZonaDTO> listarContratosPorZona(UUID zonaUuid) {
        Zona zona = zonaRepository.findByUuid(zonaUuid)
            .orElseThrow(() -> new EntityNotFoundException("Zona no encontrada"));
            
        List<ContratoZona> contratoZonas = contratoZonaRepository.findByZonaAndActivoTrueOrderByFechaCreacionDesc(zona);
        
        return contratoZonas.stream()
            .map(this::convertirContratoZonaADTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtener zonas que necesitan atención
     */
    @Transactional(readOnly = true)
    public List<ContratoZonaDTO> obtenerZonasQueNecesitanAtencion() {
        List<ContratoZona> zonasConProblemas = contratoZonaRepository.findZonasQueNecesitanAtencion();
        
        return zonasConProblemas.stream()
            .map(this::convertirContratoZonaADTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Buscar coordinadores disponibles para una zona
     */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> buscarCoordinadoresDisponibles(UUID contratoUuid, LocalDate fechaInicio, LocalDate fechaFin) {
        // Obtener coordinadores que no tienen conflictos de horario
        List<Usuario> coordinadores = usuarioRepository.findByPerfilAndActivoTrue(PerfilUsuario.COORDINADOR);
        
        return coordinadores.stream()
            .filter(coordinador -> {
                long conflictos = contratoZonaRepository.countConflictosCoordinador(
                    coordinador, fechaInicio, fechaFin);
                return conflictos == 0;
            })
            .map(this::convertirUsuarioADTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Validar disponibilidad de coordinador
     */
    @Transactional(readOnly = true)
    public boolean coordinadorDisponible(UUID coordinadorUuid, LocalDate fechaInicio, LocalDate fechaFin, UUID contratoExcluir) {
        Usuario coordinador = usuarioRepository.findByUuid(coordinadorUuid)
            .orElseThrow(() -> new EntityNotFoundException("Coordinador no encontrado"));
            
        // Buscar conflictos de horario
        long conflictos = contratoZonaRepository.countConflictosCoordinador(coordinador, fechaInicio, fechaFin);
        
        return conflictos == 0;
    }
    
    /**
     * Obtener resumen de asignaciones por coordinador
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerResumenPorCoordinador(UUID coordinadorUuid) {
        Usuario coordinador = usuarioRepository.findByUuid(coordinadorUuid)
            .orElseThrow(() -> new EntityNotFoundException("Coordinador no encontrado"));
            
        Map<String, Object> resumen = new HashMap<>();
        
        // Contar asignaciones como coordinador de zona
        long asignacionesZona = contratoZonaRepository.countByCoordinadorZonaAndActivoTrue(coordinador);
        
        // Contar asignaciones como coordinador operativo
        long asignacionesOperativo = contratoZonaRepository.countByCoordinadorOperativoAndActivoTrue(coordinador);
        
        // Obtener zonas donde participa
        List<ContratoZona> zonasParticipacion = contratoZonaRepository.findZonasByCoordinador(coordinador);
        
        resumen.put("totalAsignacionesZona", asignacionesZona);
        resumen.put("totalAsignacionesOperativo", asignacionesOperativo);
        resumen.put("totalAsignaciones", asignacionesZona + asignacionesOperativo);
        resumen.put("zonasParticipacion", zonasParticipacion.size());
        resumen.put("coordinador", convertirUsuarioADTO(coordinador));
        
        return resumen;
    }
    
    /**
     * Obtener estadísticas de rendimiento de zonas
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerEstadisticasRendimiento() {
        List<Object[]> zonasConMasContratos = contratoZonaRepository.findZonasConMasContratos();
        
        return zonasConMasContratos.stream()
            .map(resultado -> {
                Zona zona = (Zona) resultado[0];
                Long cantidad = (Long) resultado[1];
                
                Map<String, Object> estadistica = new HashMap<>();
                estadistica.put("zona", convertirADTO(zona));
                estadistica.put("totalContratos", cantidad);
                estadistica.putAll(obtenerEstadisticas(zona.getId()));
                
                return estadistica;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Obtener dashboard de zonas
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Estadísticas generales
        dashboard.put("resumenGeneral", obtenerResumenGeneral());
        
        // Zonas que necesitan atención
        List<ContratoZonaDTO> zonasAtencion = obtenerZonasQueNecesitanAtencion();
        dashboard.put("zonasQueNecesitanAtencion", zonasAtencion);
        dashboard.put("totalZonasAtencion", zonasAtencion.size());
        
        // Estadísticas por estado
        List<Object[]> estadisticasPorEstado = contratoZonaRepository.findEstadisticasEstadosByContrato(null);
        dashboard.put("estadisticasPorEstado", estadisticasPorEstado);
        
        // Top 5 zonas con más contratos
        List<Object[]> topZonas = contratoZonaRepository.findZonasConMasContratos();
        List<Map<String, Object>> topZonasFormateado = topZonas.stream()
            .limit(5)
            .map(resultado -> {
                Zona zona = (Zona) resultado[0];
                Long cantidad = (Long) resultado[1];
                
                Map<String, Object> item = new HashMap<>();
                item.put("zona", convertirADTO(zona));
                item.put("cantidad", cantidad);
                return item;
            })
            .collect(Collectors.toList());
        dashboard.put("topZonas", topZonasFormateado);
        
        return dashboard;
    }
    
    /**
     * Exportar datos de zona para reportes
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> exportarDatosZona(UUID zonaUuid) {
        Zona zona = zonaRepository.findByUuid(zonaUuid)
            .orElseThrow(() -> new EntityNotFoundException("Zona no encontrada"));
            
        List<ContratoZona> asignaciones = contratoZonaRepository.findByZonaOrderByFechaCreacionDesc(zona);
        
        return asignaciones.stream()
            .map(cz -> {
                Map<String, Object> datos = new HashMap<>();
                datos.put("zonaId", cz.getZona().getId());
                datos.put("zonaNombre", cz.getZona().getNombre());
                datos.put("zonaCodigo", cz.getZona().getCodigo());
                datos.put("contratoNumero", cz.getContrato().getNumeroContrato());
                datos.put("planTarifa", cz.getPlanTarifa().getNombre());
                datos.put("coordinadorZona", cz.getNombreCoordinadorZona());
                datos.put("coordinadorOperativo", cz.getNombreCoordinadorOperativo());
                datos.put("estado", cz.getEstado().name());
                datos.put("fechaCreacion", cz.getFechaCreacion());
                datos.put("activo", cz.getActivo());
                return datos;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Buscar zonas por criterios avanzados
     */
    @Transactional(readOnly = true)
    public Page<ContratoZonaDTO> buscarAsignacionesConFiltros(
            UUID contratoUuid, UUID zonaUuid, String estado, Boolean activo, 
            UUID coordinadorUuid, Pageable pageable) {
        
        EstadoContratoZona estadoEnum = null;
        if (estado != null && !estado.isEmpty()) {
            try {
                estadoEnum = EstadoContratoZona.valueOf(estado.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Estado inválido proporcionado: {}", estado);
            }
        }
        
        Page<ContratoZona> asignaciones = contratoZonaRepository.findConFiltros(
            contratoUuid, zonaUuid, estadoEnum, activo, coordinadorUuid, pageable);
        
        return asignaciones.map(this::convertirContratoZonaADTO);
    }
    
    /**
     * Convertir Usuario a DTO básico
     */
    private UsuarioDTO convertirUsuarioADTO(Usuario usuario) {
        return UsuarioDTO.builder()
            .uuid(usuario.getUuid())
            .nombre(usuario.getNombre())
            .email(usuario.getEmail())
            .perfil(usuario.getPerfil())
            .activo(usuario.getActivo())
            .build();
    }
    
    /**
     * Validar integridad de datos antes de operaciones críticas
     */
    private void validarIntegridad(ContratoZona contratoZona) {
        if (contratoZona.getContrato() == null) {
            throw new IllegalStateException("La asignación debe tener un contrato válido");
        }
        
        if (contratoZona.getZona() == null) {
            throw new IllegalStateException("La asignación debe tener una zona válida");
        }
        
        if (contratoZona.getPlanTarifa() == null) {
            throw new IllegalStateException("La asignación debe tener un plan de tarifa válido");
        }
        
        // Validaciones adicionales según reglas de negocio
        if (!contratoZona.getZona().estaActiva()) {
            throw new IllegalStateException("No se puede asignar una zona inactiva");
        }
    }
    
    /**
     * Logging de auditoria para operaciones críticas
     */
    private void auditarOperacion(String operacion, Object entidad, Usuario usuario) {
        log.info("AUDITORIA - Operación: {}, Entidad: {}, Usuario: {}, Timestamp: {}", 
            operacion, entidad.toString(), usuario != null ? usuario.getEmail() : "SISTEMA", 
            java.time.LocalDateTime.now());
    }
}