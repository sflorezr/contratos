package co.empresa.gestioncontratos.service;

import co.empresa.gestioncontratos.dto.TarifaDTO;
import co.empresa.gestioncontratos.entity.PlanTarifa;
import co.empresa.gestioncontratos.entity.Servicio;
import co.empresa.gestioncontratos.entity.Tarifa;
import co.empresa.gestioncontratos.enums.TipoPredio;
import co.empresa.gestioncontratos.repository.PlanTarifaRepository;
import co.empresa.gestioncontratos.repository.ServicioRepository;
import co.empresa.gestioncontratos.repository.TarifaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TarifaService {

    private final TarifaRepository tarifaRepository;
    private final PlanTarifaRepository planTarifaRepository;
    private final ServicioRepository servicioRepository;

    // ==================== CONSULTAS ====================

    @Transactional(readOnly = true)
    public List<Tarifa> listarTodas() {
        log.info("Listando todas las tarifas");
        return tarifaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Tarifa> listarTodasPaginado(Pageable pageable) {
        return tarifaRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Tarifa> listarPorPlanTarifa(UUID planTarifaUuid) {
        log.info("Listando tarifas del plan: {}", planTarifaUuid);
        
        PlanTarifa planTarifa = planTarifaRepository.findByUuid(planTarifaUuid)
            .orElseThrow(() -> new RuntimeException("Plan de tarifa no encontrado"));
            
        return planTarifa.getTarifas().stream()
            .filter(Tarifa::getActivo)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Tarifa> listarPorServicio(UUID servicioUuid) {
        log.info("Listando tarifas del servicio: {}", servicioUuid);
        
        Servicio servicio = servicioRepository.findByUuid(servicioUuid)
            .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
            
        return tarifaRepository.findByServicioOrderByPrecioUrbanoAsc(servicio);
    }

    @Transactional(readOnly = true)
    public Tarifa buscarPorUuid(UUID uuid) {
        return tarifaRepository.findByUuid(uuid)
            .orElseThrow(() -> new RuntimeException("Tarifa no encontrada: " + uuid));
    }

    @Transactional(readOnly = true)
    public Optional<Tarifa> buscarPorPlanYServicio(UUID planTarifaUuid, UUID servicioUuid) {
        log.info("Buscando tarifa para plan {} y servicio {}", planTarifaUuid, servicioUuid);
        
        return tarifaRepository.findByPlanTarifaUuidAndServicioUuid(planTarifaUuid, servicioUuid);
    }

    // ==================== GESTIÓN DE TARIFAS ====================

    public Tarifa crear(TarifaDTO tarifaDTO) {
        log.info("Creando nueva tarifa para servicio: {}", tarifaDTO.getServicioUuid());
        
        // Validar que no exista ya una tarifa para el mismo plan y servicio
        Optional<Tarifa> tarifaExistente = buscarPorPlanYServicio(
            tarifaDTO.getPlanTarifaUuid(), 
            tarifaDTO.getServicioUuid()
        );
        
        if (tarifaExistente.isPresent()) {
            throw new RuntimeException("Ya existe una tarifa para este servicio en el plan seleccionado");
        }
        
        // Validar que los precios sean positivos
        if (tarifaDTO.getPrecioUrbano().compareTo(BigDecimal.ZERO) <= 0 ||
            tarifaDTO.getPrecioRural().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Los precios deben ser mayores a cero");
        }
        
        PlanTarifa planTarifa = planTarifaRepository.findByUuid(tarifaDTO.getPlanTarifaUuid())
            .orElseThrow(() -> new RuntimeException("Plan de tarifa no encontrado"));
            
        Servicio servicio = servicioRepository.findByUuid(tarifaDTO.getServicioUuid())
            .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
        
        Tarifa tarifa = Tarifa.builder()
            .planTarifa(planTarifa)
            .servicio(servicio)
            .precioUrbano(tarifaDTO.getPrecioUrbano())
            .precioRural(tarifaDTO.getPrecioRural())
            .activo(true)
            .build();
        
        return tarifaRepository.save(tarifa);
    }

    public Tarifa actualizar(UUID uuid, TarifaDTO tarifaDTO) {
        log.info("Actualizando tarifa: {}", uuid);
        
        Tarifa tarifa = buscarPorUuid(uuid);
        
        // Validar que los precios sean positivos
        if (tarifaDTO.getPrecioUrbano().compareTo(BigDecimal.ZERO) <= 0 ||
            tarifaDTO.getPrecioRural().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Los precios deben ser mayores a cero");
        }
        
        // Si cambia el plan o servicio, validar que no exista duplicado
        if (!tarifa.getPlanTarifa().getUuid().equals(tarifaDTO.getPlanTarifaUuid()) ||
            !tarifa.getServicio().getUuid().equals(tarifaDTO.getServicioUuid())) {
            
            Optional<Tarifa> tarifaExistente = buscarPorPlanYServicio(
                tarifaDTO.getPlanTarifaUuid(), 
                tarifaDTO.getServicioUuid()
            );
            
            if (tarifaExistente.isPresent() && !tarifaExistente.get().getUuid().equals(uuid)) {
                throw new RuntimeException("Ya existe una tarifa para este servicio en el plan seleccionado");
            }
        }
        
        if (!tarifa.getPlanTarifa().getUuid().equals(tarifaDTO.getPlanTarifaUuid())) {
            PlanTarifa planTarifa = planTarifaRepository.findByUuid(tarifaDTO.getPlanTarifaUuid())
                .orElseThrow(() -> new RuntimeException("Plan de tarifa no encontrado"));
            tarifa.setPlanTarifa(planTarifa);
        }
        
        if (!tarifa.getServicio().getUuid().equals(tarifaDTO.getServicioUuid())) {
            Servicio servicio = servicioRepository.findByUuid(tarifaDTO.getServicioUuid())
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
            tarifa.setServicio(servicio);
        }
        
        tarifa.setPrecioUrbano(tarifaDTO.getPrecioUrbano());
        tarifa.setPrecioRural(tarifaDTO.getPrecioRural());
        
        return tarifaRepository.save(tarifa);
    }

    public void cambiarEstado(UUID uuid) {
        log.info("Cambiando estado de la tarifa: {}", uuid);
        
        Tarifa tarifa = buscarPorUuid(uuid);
        tarifa.setActivo(!tarifa.getActivo());
        
        tarifaRepository.save(tarifa);
    }

    public void eliminar(UUID uuid) {
        log.info("Eliminando tarifa: {}", uuid);
        
        Tarifa tarifa = buscarPorUuid(uuid);
        
        // Aquí podrías agregar validaciones adicionales
        // Por ejemplo, verificar que no haya actividades usando esta tarifa
        
        tarifaRepository.delete(tarifa);
    }

    // ==================== CONSULTAS ESPECÍFICAS ====================

    @Transactional(readOnly = true)
    public BigDecimal obtenerPrecioPorServicioYTipo(UUID planTarifaUuid, UUID servicioUuid, TipoPredio tipoPredio) {
        log.info("Obteniendo precio para servicio {} tipo {} en plan {}", 
            servicioUuid, tipoPredio, planTarifaUuid);
        
        Optional<Tarifa> tarifa = buscarPorPlanYServicio(planTarifaUuid, servicioUuid);
        
        if (tarifa.isEmpty()) {
            throw new RuntimeException("No se encontró tarifa para el servicio en el plan especificado");
        }
        
        return tarifa.get().getPrecioPorTipo(tipoPredio.name());
    }

    @Transactional(readOnly = true)
    public List<TarifaDTO> listarTarifasConDetalles(UUID planTarifaUuid) {
        log.info("Listando tarifas con detalles del plan: {}", planTarifaUuid);
        
        List<Tarifa> tarifas = listarPorPlanTarifa(planTarifaUuid);
        
        return tarifas.stream()
            .map(this::convertirADTOConDetalles)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerResumenTarifasPorPlan(UUID planTarifaUuid) {
        log.info("Obteniendo resumen de tarifas del plan: {}", planTarifaUuid);
        
        List<Tarifa> tarifas = listarPorPlanTarifa(planTarifaUuid);
        
        Map<String, Object> resumen = new HashMap<>();
        resumen.put("totalTarifas", tarifas.size());
        resumen.put("tarifasActivas", tarifas.stream().filter(Tarifa::getActivo).count());
        
        if (!tarifas.isEmpty()) {
            BigDecimal promedioUrbano = tarifas.stream()
                .map(Tarifa::getPrecioUrbano)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(tarifas.size()), 2, BigDecimal.ROUND_HALF_UP);
                
            BigDecimal promedioRural = tarifas.stream()
                .map(Tarifa::getPrecioRural)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(tarifas.size()), 2, BigDecimal.ROUND_HALF_UP);
                
            resumen.put("precioPromedioUrbano", promedioUrbano);
            resumen.put("precioPromedioRural", promedioRural);
        }
        
        return resumen;
    }

    // ==================== OPERACIONES MASIVAS ====================

    public List<Tarifa> crearTarifasMasivo(UUID planTarifaUuid, List<TarifaDTO> tarifasDTO) {
        log.info("Creando {} tarifas para el plan {}", tarifasDTO.size(), planTarifaUuid);
        
        List<Tarifa> tarifasCreadas = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        
        for (int i = 0; i < tarifasDTO.size(); i++) {
            TarifaDTO dto = tarifasDTO.get(i);
            dto.setPlanTarifaUuid(planTarifaUuid);
            
            try {
                Tarifa tarifa = crear(dto);
                tarifasCreadas.add(tarifa);
            } catch (Exception e) {
                errores.add(String.format("Fila %d: %s", i + 1, e.getMessage()));
            }
        }
        
        if (!errores.isEmpty()) {
            log.warn("Errores durante la creación masiva: {}", errores);
            throw new RuntimeException("Se crearon " + tarifasCreadas.size() + 
                " tarifas con errores: " + String.join(", ", errores));
        }
        
        return tarifasCreadas;
    }

    public void actualizarPreciosMasivo(UUID planTarifaUuid, BigDecimal porcentajeAumento) {
        log.info("Actualizando precios del plan {} con aumento del {}%", planTarifaUuid, porcentajeAumento);
        
        if (porcentajeAumento.compareTo(new BigDecimal("-100")) < 0 ||
            porcentajeAumento.compareTo(new BigDecimal("1000")) > 0) {
            throw new RuntimeException("El porcentaje de aumento debe estar entre -100% y 1000%");
        }
        
        List<Tarifa> tarifas = listarPorPlanTarifa(planTarifaUuid);
        BigDecimal factor = BigDecimal.ONE.add(porcentajeAumento.divide(new BigDecimal("100")));
        
        for (Tarifa tarifa : tarifas) {
            tarifa.setPrecioUrbano(tarifa.getPrecioUrbano().multiply(factor).setScale(2, BigDecimal.ROUND_HALF_UP));
            tarifa.setPrecioRural(tarifa.getPrecioRural().multiply(factor).setScale(2, BigDecimal.ROUND_HALF_UP));
            tarifaRepository.save(tarifa);
        }
        
        log.info("Actualizados {} precios", tarifas.size());
    }

    @Transactional(readOnly = true)
    public Page<Tarifa> buscarConFiltros(String filtro, Boolean activo, UUID planTarifaUuid, 
                                        UUID servicioUuid, Pageable pageable) {
        log.info("Buscando tarifas con filtros - texto: {}, activo: {}, plan: {}, servicio: {}", 
            filtro, activo, planTarifaUuid, servicioUuid);
        
        Specification<Tarifa> spec = Specification.where(null);
        
        // Filtro por texto (busca en nombre del plan o servicio)
        if (filtro != null && !filtro.trim().isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> {
                String filtroLike = "%" + filtro.toLowerCase() + "%";
                return criteriaBuilder.or(
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("planTarifa").get("nombre")), 
                        filtroLike
                    ),
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("servicio").get("nombre")), 
                        filtroLike
                    )
                );
            });
        }
        
        // Filtro por estado activo
        if (activo != null) {
            spec = spec.and((root, query, criteriaBuilder) -> 
                criteriaBuilder.equal(root.get("activo"), activo)
            );
        }
        
        // Filtro por plan de tarifa
        if (planTarifaUuid != null) {
            spec = spec.and((root, query, criteriaBuilder) -> 
                criteriaBuilder.equal(root.get("planTarifa").get("uuid"), planTarifaUuid)
            );
        }
        
        // Filtro por servicio
        if (servicioUuid != null) {
            spec = spec.and((root, query, criteriaBuilder) -> 
                criteriaBuilder.equal(root.get("servicio").get("uuid"), servicioUuid)
            );
        }
        
        return tarifaRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerResumenGeneral() {
        log.info("Obteniendo resumen general de tarifas");
        
        Map<String, Object> resumen = new HashMap<>();
        
        // Contar totales
        long totalTarifas = tarifaRepository.count();
        long tarifasActivas = tarifaRepository.countByActivo(true);
        long tarifasInactivas = totalTarifas - tarifasActivas;
        
        resumen.put("totalTarifas", totalTarifas);
        resumen.put("tarifasActivas", tarifasActivas);
        resumen.put("tarifasInactivas", tarifasInactivas);
        
        // Estadísticas por plan de tarifa
        List<Object[]> tarifasPorPlan = tarifaRepository.contarTarifasPorPlan();
        Map<String, Long> distribucionPorPlan = tarifasPorPlan.stream()
            .collect(Collectors.toMap(
                obj -> (String) obj[0], // nombre del plan
                obj -> (Long) obj[1]    // cantidad
            ));
        resumen.put("tarifasPorPlan", distribucionPorPlan);
        
        // Promedios de precios
        if (totalTarifas > 0) {
            BigDecimal promedioUrbano = tarifaRepository.obtenerPromedioPrecioUrbano();
            BigDecimal promedioRural = tarifaRepository.obtenerPromedioPrecioRural();
            BigDecimal precioMinimoUrbano = tarifaRepository.obtenerPrecioMinimoUrbano();
            BigDecimal precioMaximoUrbano = tarifaRepository.obtenerPrecioMaximoUrbano();
            
            resumen.put("promedioUrbano", promedioUrbano);
            resumen.put("promedioRural", promedioRural);
            resumen.put("precioMinimoUrbano", precioMinimoUrbano);
            resumen.put("precioMaximoUrbano", precioMaximoUrbano);
        }
        
        return resumen;
    }
    // ==================== UTILIDADES ====================

    public TarifaDTO convertirADTO(Tarifa tarifa) {
        return TarifaDTO.builder()
            .uuid(tarifa.getUuid())
            .planTarifaUuid(tarifa.getPlanTarifa().getUuid())
            .servicioUuid(tarifa.getServicio().getUuid())
            .precioUrbano(tarifa.getPrecioUrbano())
            .precioRural(tarifa.getPrecioRural())
            .activo(tarifa.getActivo())
            .build();
    }

    public TarifaDTO convertirADTOConDetalles(Tarifa tarifa) {
        TarifaDTO dto = convertirADTO(tarifa);
        dto.setPlanTarifaNombre(tarifa.getPlanTarifa().getNombre());
        dto.setServicioNombre(tarifa.getServicio().getNombre());
        return dto;
    }
}