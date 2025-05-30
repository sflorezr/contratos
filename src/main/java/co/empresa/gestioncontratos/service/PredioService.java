package co.empresa.gestioncontratos.service;

import co.empresa.gestioncontratos.dto.PredioDTO;
import co.empresa.gestioncontratos.entity.*;
import co.empresa.gestioncontratos.enums.TipoPredio;
import co.empresa.gestioncontratos.enums.EstadoContrato;
import co.empresa.gestioncontratos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PredioService {

    private final PredioRepository predioRepository;
    private final SectorRepository sectorRepository;
    private final ContratoPredioRepository contratoPredioRepository;

    // ==================== CONSULTAS ====================

    @Transactional(readOnly = true)
    public Page<Predio> listarTodos(Pageable pageable) {
        log.info("Listando todos los predios");
        return predioRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Predio> listarTodos() {
        return predioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Predio> buscarConFiltros(String filtro, UUID sectorUuid, TipoPredio tipo, Pageable pageable) {
        log.info("Buscando predios con filtros - filtro: {}, sector: {}, tipo: {}", filtro, sectorUuid, tipo);
        
        Specification<Predio> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Filtro por texto (busca en dirección)
            if (filtro != null && !filtro.trim().isEmpty()) {
                String filtroLike = "%" + filtro.toLowerCase().trim() + "%";
                Predicate direccionPred = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("direccion")), filtroLike
                );
                predicates.add(direccionPred);
            }
            
            // Filtro por sector
            if (sectorUuid != null) {
                predicates.add(criteriaBuilder.equal(root.get("sector").get("uuid"), sectorUuid));
            }
            
            // Filtro por tipo
            if (tipo != null) {
                predicates.add(criteriaBuilder.equal(root.get("tipo"), tipo));
            }
            
            // Solo predios activos
            predicates.add(criteriaBuilder.equal(root.get("activo"), true));
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        return predioRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Predio buscarPorUuid(UUID uuid) {
        return predioRepository.findByUuid(uuid)
            .orElseThrow(() -> new RuntimeException("Predio no encontrado: " + uuid));
    }

    @Transactional(readOnly = true)
    public List<Predio> buscarPorSector(UUID sectorUuid) {
        Sector sector = sectorRepository.findByUuid(sectorUuid)
            .orElseThrow(() -> new RuntimeException("Sector no encontrado"));
        
        return predioRepository.findBySectorOrderByDireccion(sector);
    }

    @Transactional(readOnly = true)
    public List<Predio> buscarPorTipo(TipoPredio tipo) {
        return predioRepository.findByTipoOrderByDireccion(tipo);
    }

    @Transactional(readOnly = true)
    public List<Predio> listarPrediosPorOperario(Usuario operario) {
        log.info("Listando predios asignados al operario: {}", operario.getUsername());
        return predioRepository.findPrediosPorOperario(operario);
    }

    // ==================== GESTIÓN DE PREDIOS ====================

    public Predio crear(PredioDTO predioDTO) {
        log.info("Creando nuevo predio: {}", predioDTO.getDireccion());
        
        Sector sector = sectorRepository.findByUuid(predioDTO.getSectorUuid())
            .orElseThrow(() -> new RuntimeException("Sector no encontrado"));
        
        Predio predio = Predio.builder()
            .direccion(predioDTO.getDireccion())
            .sector(sector)
            .tipo(predioDTO.getTipo())
            .activo(true)
            .build();
        
        return predioRepository.save(predio);
    }

    public Predio actualizar(UUID uuid, PredioDTO predioDTO) {
        log.info("Actualizando predio: {}", uuid);
        
        Predio predio = buscarPorUuid(uuid);
        
        predio.setDireccion(predioDTO.getDireccion());
        predio.setTipo(predioDTO.getTipo());
        
        if (predioDTO.getSectorUuid() != null) {
            Sector sector = sectorRepository.findByUuid(predioDTO.getSectorUuid())
                .orElseThrow(() -> new RuntimeException("Sector no encontrado"));
            predio.setSector(sector);
        }
        
        return predioRepository.save(predio);
    }

    public void cambiarEstado(UUID uuid) {
        log.info("Cambiando estado del predio: {}", uuid);
        
        Predio predio = buscarPorUuid(uuid);
        predio.setActivo(!predio.getActivo());
        
        predioRepository.save(predio);
    }

    public void eliminar(UUID uuid) {
        log.info("Eliminando predio: {}", uuid);
        
        Predio predio = buscarPorUuid(uuid);
        
        // Verificar que no esté asignado a contratos activos
        if (contratoPredioRepository.existsByPredioAndContratoEstado(predio, EstadoContrato.ACTIVO)) {
            throw new RuntimeException("No se puede eliminar el predio porque está asignado a contratos activos");
        }
        
        // Verificar que no tenga actividades
        if (predio.tieneActividades()) {
            throw new RuntimeException("No se puede eliminar el predio porque tiene actividades registradas");
        }
        
        predioRepository.delete(predio);
    }

    // ==================== CONSULTAS ESPECÍFICAS ====================

    @Transactional(readOnly = true)
    public List<Predio> listarPrediosDisponibles() {
        log.info("Listando predios disponibles");
        return predioRepository.findPrediosDisponibles();
    }

    @Transactional(readOnly = true)
    public List<Predio> listarPrediosDisponiblesParaContrato(UUID contratoUuid) {
        log.info("Listando predios disponibles para contrato: {}", contratoUuid);
        return predioRepository.findPrediosDisponiblesParaContrato(contratoUuid);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticasPredio(UUID predioUuid) {
        log.info("Obteniendo estadísticas del predio: {}", predioUuid);
        
        Predio predio = buscarPorUuid(predioUuid);
        Map<String, Object> stats = new HashMap<>();
        
        // Información básica
        stats.put("predio", convertirADTO(predio));
        
        // Contratos asociados
        List<ContratoPredio> contratos = contratoPredioRepository.findByPredio(predio);
        stats.put("totalContratos", contratos.size());
        stats.put("contratosActivos", contratos.stream()
            .filter(cp -> cp.getContrato().estaActivo())
            .count());
        
        // Actividades
        stats.put("totalActividades", predio.getCantidadActividades());
        stats.put("actividadesPendientes", predio.getCantidadActividadesPendientes());
        
        return stats;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerHistorialAsignaciones(UUID predioUuid) {
        log.info("Obteniendo historial de asignaciones del predio: {}", predioUuid);
        
        Predio predio = buscarPorUuid(predioUuid);
        List<ContratoPredio> historial = contratoPredioRepository.findByPredioOrderByFechaCreacionDesc(predio);
        
        return historial.stream().map(cp -> {
            Map<String, Object> asignacion = new HashMap<>();
            asignacion.put("contrato", Map.of(
                "uuid", cp.getContrato().getUuid(),
                "numeroContrato", cp.getContrato().getNumeroContrato(),
                "objetivo", cp.getContrato().getObjetivo(),
                "fechaInicio", cp.getContrato().getFechaInicio(),
                "fechaFin", cp.getContrato().getFechaFin()
            ));
            
            asignacion.put("estado", cp.getEstado());
            asignacion.put("fechaAsignacion", cp.getFechaCreacion());
            asignacion.put("activo", cp.getActivo());
            
            return asignacion;
        }).collect(Collectors.toList());
    }

    // ==================== IMPORTACIÓN/EXPORTACIÓN ====================

    public List<Predio> importarPredios(List<PredioDTO> prediosDTO) {
        log.info("Importando {} predios", prediosDTO.size());
        
        List<Predio> prediosImportados = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        
        for (int i = 0; i < prediosDTO.size(); i++) {
            PredioDTO dto = prediosDTO.get(i);
            try {
                Predio predio = crear(dto);
                prediosImportados.add(predio);
            } catch (Exception e) {
                errores.add(String.format("Fila %d: %s", i + 1, e.getMessage()));
            }
        }
        
        if (!errores.isEmpty()) {
            log.warn("Errores durante la importación: {}", errores);
            throw new RuntimeException("Se importaron " + prediosImportados.size() + 
                " predios con errores: " + String.join(", ", errores));
        }
        
        return prediosImportados;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> exportarPredios(UUID sectorUuid, TipoPredio tipo) {
        log.info("Exportando predios - sector: {}, tipo: {}", sectorUuid, tipo);
        
        List<Predio> predios;
        
        if (sectorUuid != null && tipo != null) {
            Sector sector = sectorRepository.findByUuid(sectorUuid)
                .orElseThrow(() -> new RuntimeException("Sector no encontrado"));
            predios = predioRepository.findBySectorAndTipo(sector, tipo);
        } else if (sectorUuid != null) {
            predios = buscarPorSector(sectorUuid);
        } else if (tipo != null) {
            predios = buscarPorTipo(tipo);
        } else {
            predios = listarTodos();
        }
        
        return predios.stream().map(predio -> {
            Map<String, Object> data = new HashMap<>();
            data.put("direccion", predio.getDireccion());
            data.put("sector", predio.getSector().getNombre());
            data.put("tipo", predio.getTipo().toString());
            data.put("activo", predio.getActivo());
            
            // Agregar información de asignación actual
            contratoPredioRepository.findActiveByPredio(predio).ifPresent(cp -> {
                data.put("contratoActual", cp.getContrato().getNumeroContrato());
                data.put("estadoAsignacion", cp.getEstado().toString());
            });
            
            return data;
        }).collect(Collectors.toList());
    }

    // ==================== VALIDACIONES ====================

    @Transactional(readOnly = true)
    public boolean predioTieneActividadesPendientes(UUID predioUuid) {
        Predio predio = buscarPorUuid(predioUuid);
        return predio.getCantidadActividadesPendientes() > 0;
    }

    @Transactional(readOnly = true)
    public boolean predioEstaEnContratoActivo(UUID predioUuid) {
        Predio predio = buscarPorUuid(predioUuid);
        return contratoPredioRepository.existsByPredioAndContratoEstado(predio, EstadoContrato.ACTIVO);
    }

    // ==================== UTILIDADES ====================

    public PredioDTO convertirADTO(Predio predio) {
        return PredioDTO.builder()
            .uuid(predio.getUuid())
            .direccion(predio.getDireccion())
            .sectorUuid(predio.getSector().getUuid())
            .sectorNombre(predio.getSector().getNombre())
            .tipo(predio.getTipo())
            .activo(predio.getActivo())
            .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> obtenerResumenPredios() {
        Map<String, Long> resumen = new HashMap<>();
        
        resumen.put("total", predioRepository.count());
        resumen.put("activos", predioRepository.countByActivoTrue());
        resumen.put("urbanos", predioRepository.countByTipo(TipoPredio.URBANO));
        resumen.put("rurales", predioRepository.countByTipo(TipoPredio.RURAL));
        resumen.put("enContratosActivos", predioRepository.countPrediosEnContratosActivos());
        resumen.put("disponibles", predioRepository.countPrediosDisponibles());
        
        return resumen;
    }
    @Transactional(readOnly = true)
    public boolean existeCodigoCatastral(String codigoCatastral) {
        log.debug("Verificando si existe código catastral: {}", codigoCatastral);
        return predioRepository.existsByCodigoCatastral(codigoCatastral);
    }

    @Transactional(readOnly = true)
    public Predio buscarPorCodigoCatastral(String codigoCatastral) {
        log.info("Buscando predio por código catastral: {}", codigoCatastral);
        return predioRepository.findByCodigoCatastral(codigoCatastral)
            .orElseThrow(() -> new RuntimeException("Predio no encontrado con código catastral: " + codigoCatastral));
    }   
}