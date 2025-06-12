package co.empresa.gestioncontratos.service;

import co.empresa.gestioncontratos.dto.SectorDTO;
import co.empresa.gestioncontratos.entity.Sector;
import co.empresa.gestioncontratos.enums.EstadoContrato;
import co.empresa.gestioncontratos.enums.TipoPredio;
import co.empresa.gestioncontratos.repository.SectorRepository;
import co.empresa.gestioncontratos.repository.PredioRepository;
import co.empresa.gestioncontratos.repository.ContratoRepository;
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
public class SectorService {

    private final SectorRepository sectorRepository;
    private final PredioRepository predioRepository;
    private final ContratoRepository contratoRepository;

    // ==================== CONSULTAS ====================

    @Transactional(readOnly = true)
    public List<Sector> listarTodos() {
        log.info("Listando todos los sectores");
        return sectorRepository.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Page<Sector> listarTodosPaginado(Pageable pageable) {
        log.info("Listando sectores paginados");
        return sectorRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Sector> listarActivos() {
        log.info("Listando sectores activos");
        return sectorRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Page<Sector> buscarConFiltros(String filtro, Boolean activo, Pageable pageable) {
        log.info("Buscando sectores con filtros - filtro: {}, activo: {}", filtro, activo);
        
        Specification<Sector> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Filtro por texto (busca en nombre, código y descripción)
            if (filtro != null && !filtro.trim().isEmpty()) {
                String filtroLike = "%" + filtro.toLowerCase().trim() + "%";
                
                Predicate nombrePred = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("nombre")), filtroLike
                );
                Predicate codigoPred = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("codigo")), filtroLike
                );
                Predicate descripcionPred = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("descripcion")), filtroLike
                );
                
                predicates.add(criteriaBuilder.or(nombrePred, codigoPred, descripcionPred));
            }
            
            // Filtro por estado activo
            if (activo != null) {
                predicates.add(criteriaBuilder.equal(root.get("activo"), activo));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        return sectorRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Sector buscarPorUuid(UUID uuid) {
        return sectorRepository.findByUuid(uuid)
            .orElseThrow(() -> new RuntimeException("Sector no encontrado: " + uuid));
    }

    @Transactional(readOnly = true)
    public Sector buscarPorNombre(String nombre) {
        return sectorRepository.findByNombre(nombre)
            .orElseThrow(() -> new RuntimeException("Sector no encontrado con nombre: " + nombre));
    }

    @Transactional(readOnly = true)
    public Sector buscarPorCodigo(String codigo) {
        return sectorRepository.findByCodigo(codigo)
            .orElseThrow(() -> new RuntimeException("Sector no encontrado con código: " + codigo));
    }

    // ==================== GESTIÓN DE SECTORES ====================

    public Sector crear(SectorDTO sectorDTO) {
        log.info("Creando nuevo sector: {}", sectorDTO.getNombre());
        
        // Validar que no exista el nombre
        if (sectorRepository.existsByNombre(sectorDTO.getNombre())) {
            throw new RuntimeException("Ya existe un sector con el nombre: " + sectorDTO.getNombre());
        }
        
        // Validar que no exista el código si se proporciona
        if (sectorDTO.getCodigo() != null && sectorRepository.existsByCodigo(sectorDTO.getCodigo())) {
            throw new RuntimeException("Ya existe un sector con el código: " + sectorDTO.getCodigo());
        }
        
        Sector sector = Sector.builder()
            .nombre(sectorDTO.getNombre())
            .codigo(sectorDTO.getCodigo() != null ? sectorDTO.getCodigo() : generarCodigo(sectorDTO.getNombre()))
            .descripcion(sectorDTO.getDescripcion())
            .limiteNorte(sectorDTO.getLimiteNorte())
            .limiteSur(sectorDTO.getLimiteSur())
            .limiteEste(sectorDTO.getLimiteEste())
            .limiteOeste(sectorDTO.getLimiteOeste())
            .area(sectorDTO.getArea())
            .poblacion(sectorDTO.getPoblacion())
            .activo(true)
            .build();
        
        return sectorRepository.save(sector);
    }

    public Sector actualizar(UUID uuid, SectorDTO sectorDTO) {
        log.info("Actualizando sector: {}", uuid);
        
        Sector sector = buscarPorUuid(uuid);
        
        // Validar nombre único si cambió
        if (!sector.getNombre().equals(sectorDTO.getNombre()) &&
            sectorRepository.existsByNombre(sectorDTO.getNombre())) {
            throw new RuntimeException("Ya existe otro sector con el nombre: " + sectorDTO.getNombre());
        }
        
        // Validar código único si cambió
        if (sectorDTO.getCodigo() != null && 
            !sector.getCodigo().equals(sectorDTO.getCodigo()) &&
            sectorRepository.existsByCodigo(sectorDTO.getCodigo())) {
            throw new RuntimeException("Ya existe otro sector con el código: " + sectorDTO.getCodigo());
        }
        
        sector.setNombre(sectorDTO.getNombre());
        if (sectorDTO.getCodigo() != null) {
            sector.setCodigo(sectorDTO.getCodigo());
        }
        sector.setDescripcion(sectorDTO.getDescripcion());
        sector.setLimiteNorte(sectorDTO.getLimiteNorte());
        sector.setLimiteSur(sectorDTO.getLimiteSur());
        sector.setLimiteEste(sectorDTO.getLimiteEste());
        sector.setLimiteOeste(sectorDTO.getLimiteOeste());
        sector.setArea(sectorDTO.getArea());
        sector.setPoblacion(sectorDTO.getPoblacion());
        
        return sectorRepository.save(sector);
    }

    public void cambiarEstado(UUID uuid) {
        log.info("Cambiando estado del sector: {}", uuid);
        
        Sector sector = buscarPorUuid(uuid);
        
        // Verificar si se puede desactivar
        if (sector.getActivo() && tieneDependencias(sector)) {
            throw new RuntimeException("No se puede desactivar el sector porque tiene predios o contratos activos");
        }
        
        sector.setActivo(!sector.getActivo());
        sectorRepository.save(sector);
    }

    public void eliminar(UUID uuid) {
        log.info("Eliminando sector: {}", uuid);
        
        Sector sector = buscarPorUuid(uuid);
        
        // Verificar que no tenga dependencias
        if (tieneDependencias(sector)) {
            throw new RuntimeException("No se puede eliminar el sector porque tiene predios o contratos asociados");
        }
        
        sectorRepository.delete(sector);
    }

    // ==================== CONSULTAS ESPECÍFICAS ====================

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas(UUID sectorUuid) {
        log.info("Obteniendo estadísticas del sector: {}", sectorUuid);
        
        Sector sector = buscarPorUuid(sectorUuid);
        Map<String, Object> stats = new HashMap<>();
        
        // Información básica
        stats.put("sector", sector);
        
        // Cantidad de predios
        long totalPredios = predioRepository.countBySector(sector);
        long prediosActivos = predioRepository.countBySectorAndActivoTrue(sector);
        stats.put("totalPredios", totalPredios);
        stats.put("prediosActivos", prediosActivos);
        
        // Tipos de predios
        Map<String, Long> prediosPorTipo = new HashMap<>();
        prediosPorTipo.put("urbanos", predioRepository.countBySectorAndTipo(sector, TipoPredio.URBANO));
        prediosPorTipo.put("rurales", predioRepository.countBySectorAndTipo(sector, TipoPredio.RURAL));
        stats.put("prediosPorTipo", prediosPorTipo);
        
        
        // Área total de predios
        Double areaTotalPredios = predioRepository.sumAreaBySector(sector);
        stats.put("areaTotalPredios", areaTotalPredios != null ? areaTotalPredios : 0.0);
        
        return stats;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerResumenSectores() {
        log.info("Obteniendo resumen de todos los sectores");
        
        List<Sector> sectores = listarActivos();
        
        return sectores.stream().map(sector -> {
            Map<String, Object> resumen = new HashMap<>();
            resumen.put("uuid", sector.getUuid());
            resumen.put("nombre", sector.getNombre());
            resumen.put("codigo", sector.getCodigo());
            resumen.put("totalPredios", predioRepository.countBySector(sector));
            resumen.put("area", sector.getArea());
            resumen.put("poblacion", sector.getPoblacion());
            
            return resumen;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean existeNombre(String nombre) {
        return sectorRepository.existsByNombre(nombre);
    }

    @Transactional(readOnly = true)
    public boolean existeCodigo(String codigo) {
        return sectorRepository.existsByCodigo(codigo);
    }

    @Transactional(readOnly = true)
    public boolean tieneDependencias(Sector sector) {
        // Verificar si tiene predios
        if (predioRepository.existsBySector(sector)) {
            return true;
        }
        
        
        return false;
    }

    // ==================== UTILIDADES ====================

    public SectorDTO convertirADTO(Sector sector) {
        return SectorDTO.builder()
            .uuid(sector.getUuid())
            .codigo(sector.getCodigo())
            .nombre(sector.getNombre())
            .descripcion(sector.getDescripcion())
            .activo(sector.getActivo())
            .zonaId(sector.getZona().getId())
            .zonaNombre(sector.getZona().getNombre())
            .build();
    }


    private String generarCodigo(String nombre) {
        // Generar código basado en las primeras 3 letras del nombre
        String base = nombre.replaceAll("[^A-Za-z0-9]", "")
            .toUpperCase()
            .substring(0, Math.min(nombre.length(), 3));
        
        // Agregar número secuencial si es necesario
        int contador = 1;
        String codigo = base;
        
        while (sectorRepository.existsByCodigo(codigo)) {
            codigo = base + String.format("%03d", contador);
            contador++;
        }
        
        return codigo;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> obtenerResumenGeneral() {
        Map<String, Long> resumen = new HashMap<>();
        
        resumen.put("total", sectorRepository.count());
        resumen.put("activos", sectorRepository.countByActivoTrue());
        resumen.put("inactivos", sectorRepository.countByActivoFalse());
        resumen.put("conPredios", sectorRepository.countSectoresConPredios());
        resumen.put("conContratos", sectorRepository.countSectoresConContratos());
        resumen.put("sinActividad", sectorRepository.countSectoresSinActividad());  
        
        return resumen;
    }

    // ==================== IMPORTACIÓN/EXPORTACIÓN ====================

    public List<Sector> importarSectores(List<SectorDTO> sectoresDTO) {
        log.info("Importando {} sectores", sectoresDTO.size());
        
        List<Sector> sectoresImportados = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        
        for (int i = 0; i < sectoresDTO.size(); i++) {
            SectorDTO dto = sectoresDTO.get(i);
            try {
                // Limpiar espacios
                dto.trim();
                
                Sector sector = crear(dto);
                sectoresImportados.add(sector);
            } catch (Exception e) {
                errores.add(String.format("Fila %d: %s", i + 1, e.getMessage()));
            }
        }
        
        if (!errores.isEmpty()) {
            log.warn("Errores durante la importación: {}", errores);
            throw new RuntimeException("Se importaron " + sectoresImportados.size() + 
                " sectores con errores: " + String.join(", ", errores));
        }
        
        return sectoresImportados;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> exportarSectores(Boolean soloActivos) {
        log.info("Exportando sectores - solo activos: {}", soloActivos);
        
        List<Sector> sectores = soloActivos != null && soloActivos ? 
            listarActivos() : listarTodos();
        
        return sectores.stream().map(sector -> {
            Map<String, Object> data = new HashMap<>();
            data.put("codigo", sector.getCodigo());
            data.put("nombre", sector.getNombre());
            data.put("descripcion", sector.getDescripcion());
            data.put("limiteNorte", sector.getLimiteNorte());
            data.put("limiteSur", sector.getLimiteSur());
            data.put("limiteEste", sector.getLimiteEste());
            data.put("limiteOeste", sector.getLimiteOeste());
            data.put("area", sector.getArea());
            data.put("poblacion", sector.getPoblacion());
            data.put("activo", sector.getActivo());
            data.put("totalPredios", predioRepository.countBySector(sector));
            
            return data;
        }).collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public List<SectorDTO> listarTodosConEstadisticas() {
        log.info("Listando todos los sectores con estadísticas optimizado");
        
        // Una sola consulta para todos los sectores
        List<Sector> sectores = sectorRepository.findAllByOrderByNombreAsc();
        
        // Una consulta para contar predios por sector
        Map<Long, Long> prediosPorSector = predioRepository.countPrediosPorSector();
        
        // Una consulta para contar contratos activos por sector
      
        
        // Mapear todo en memoria
        return sectores.stream().map(sector -> {
            SectorDTO dto = convertirADTO(sector);
            dto.setTotalPredios(prediosPorSector.getOrDefault(sector.getId(), 0L));
            return dto;
        }).collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public List<SectorDTO> listarPorZona(Long zonaId) {
        log.info("Listando sectores de la zona: {}", zonaId);
        
        List<Sector> sectores = sectorRepository.findByZonaIdOrderByNombreAsc(zonaId);
        
        return sectores.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
    }
    }
