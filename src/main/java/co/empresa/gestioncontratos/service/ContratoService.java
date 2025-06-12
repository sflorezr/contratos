package co.empresa.gestioncontratos.service;

import co.empresa.gestioncontratos.dto.*;
import co.empresa.gestioncontratos.entity.*;
import co.empresa.gestioncontratos.enums.*;
import co.empresa.gestioncontratos.repository.*;
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
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final ContratoPredioRepository contratoPredioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PredioRepository predioRepository;
    private final ZonaRepository zonaRepository;
    private final PlanTarifaRepository planTarifaRepository;
    private final PredioOperarioRepository predioOperarioRepository;
    
    // NUEVO: Repository para la gestión de múltiples zonas
    private final ContratoZonaRepository contratoZonaRepository;

    // ==================== CONSULTAS EXISTENTES (MIGRADAS) ====================

    @Transactional(readOnly = true)
    public List<Contrato> listarActivos() {
        log.info("Listando todos los contratos activos");
        return contratoRepository.findContratosActivos();
    }

    @Transactional(readOnly = true)
    public Page<Contrato> listarTodosPaginado(Pageable pageable) {
        return contratoRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Contrato> listarPorSupervisor(Usuario supervisor) {
        log.info("Listando contratos del supervisor: {}", supervisor.getUsername());
        return contratoRepository.findBySupervisorOrderByFechaInicioDesc(supervisor);
    }

    @Transactional(readOnly = true)
    public List<Contrato> listarPorCoordinador(Usuario coordinador) {
        log.info("Listando contratos del coordinador: {}", coordinador.getUsername());
        
        // MIGRADO: Ahora busca en ContratoZona por coordinadores
        List<ContratoZona> zonasCoordinador = contratoZonaRepository.findZonasByCoordinador(coordinador);
        
        return zonasCoordinador.stream()
                .map(ContratoZona::getContrato)
                .distinct()
                .sorted((c1, c2) -> c2.getFechaInicio().compareTo(c1.getFechaInicio()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Contrato> listarContratoPorOperario(Usuario operario) {
        log.info("Listando contratos del operario: {}", operario.getUsername());
        return contratoRepository.findContratosConOperarioEnPredios(operario);
    }

    @Transactional(readOnly = true)
    public List<Contrato> listarPorOperario(Usuario operario) {
        log.info("Listando contratos del operario: {}", operario.getUsername());
        
        if (operario.getPerfil() != PerfilUsuario.OPERARIO) {
            log.warn("El usuario {} no es operario, perfil: {}", operario.getUsername(), operario.getPerfil());
            return new ArrayList<>();
        }
        
        List<Contrato> contratos = contratoRepository.findContratosConOperario(operario);
        log.info("Operario {} tiene acceso a {} contratos", operario.getUsername(), contratos.size());
        
        return contratos;
    }

    @Transactional(readOnly = true)
    public Contrato buscarPorUuid(UUID uuid) {
        return contratoRepository.findByUuid(uuid)
            .orElseThrow(() -> new RuntimeException("Contrato no encontrado: " + uuid));
    }

    @Transactional(readOnly = true)
    public ContratoDTO buscarDTOPorUuid(UUID uuid) {
        return contratoRepository.findDTOByUuid(uuid)
            .orElseThrow(() -> new RuntimeException("Contrato no encontrado: " + uuid));
    }

    @Transactional(readOnly = true)
    public Contrato buscarPorNumeroContrato(String numeroContrato) {
        return contratoRepository.findByNumeroContrato(numeroContrato)
            .orElseThrow(() -> new RuntimeException("Contrato no encontrado con número: " + numeroContrato));
    }

    // ==================== GESTIÓN DE CONTRATOS (ACTUALIZADA) ====================

    public Contrato crear(ContratoDTO contratoDTO) {
        log.info("Creando nuevo contrato: {}", contratoDTO.getCodigo());
        
        // Validar que no exista el número
        if (contratoRepository.existsByNumeroContrato(contratoDTO.getCodigo())) {
            throw new RuntimeException("Ya existe un contrato con el número: " + contratoDTO.getCodigo());
        }
        
        // Validar fechas
        if (contratoDTO.getFechaFin().isBefore(contratoDTO.getFechaInicio())) {
            throw new RuntimeException("La fecha fin no puede ser anterior a la fecha inicio");
        }
        
        // Validar que tenga al menos una zona
        if (contratoDTO.getZonas() == null || contratoDTO.getZonas().isEmpty()) {
            throw new RuntimeException("El contrato debe tener al menos una zona");
        }
        
        // Crear el contrato base (SIN zona ni plan de tarifa)
        Contrato contrato = Contrato.builder()
            .numeroContrato(contratoDTO.getCodigo())
            .objetivo(contratoDTO.getObjetivo())
            .fechaInicio(contratoDTO.getFechaInicio())
            .fechaFin(contratoDTO.getFechaFin())
            .estado(EstadoContrato.ACTIVO)
            .build();
        
        // Asignar supervisor si se proporciona
        if (contratoDTO.getSupervisorUuid() != null) {
            Usuario supervisor = usuarioRepository.findByUuid(contratoDTO.getSupervisorUuid())
                .orElseThrow(() -> new RuntimeException("Supervisor no encontrado"));
            contrato.setSupervisor(supervisor);
        }
        
        // Guardar el contrato primero
        contrato = contratoRepository.save(contrato);
        
        // Crear las relaciones ContratoZona
        for (ContratoZonaDTO zonaDTO : contratoDTO.getZonas()) {
            crearContratoZona(contrato, zonaDTO);
        }
        
        log.info("✅ Contrato creado con {} zonas", contratoDTO.getZonas().size());
        return contrato;
    }

    public Contrato actualizar(UUID uuid, ContratoDTO contratoDTO) {
        log.info("Actualizando contrato: {}", uuid);
        
        Contrato contrato = buscarPorUuid(uuid);
        
        // Validar número único si cambió
        if (!contrato.getNumeroContrato().equals(contratoDTO.getCodigo()) &&
            contratoRepository.existsByNumeroContrato(contratoDTO.getCodigo())) {
            throw new RuntimeException("Ya existe otro contrato con el número: " + contratoDTO.getCodigo());
        }
        
        // Validar fechas
        if (contratoDTO.getFechaFin().isBefore(contratoDTO.getFechaInicio())) {
            throw new RuntimeException("La fecha fin no puede ser anterior a la fecha inicio");
        }
        
        // Actualizar campos básicos del contrato
        contrato.setNumeroContrato(contratoDTO.getCodigo());
        contrato.setObjetivo(contratoDTO.getObjetivo());
        contrato.setFechaInicio(contratoDTO.getFechaInicio());
        contrato.setFechaFin(contratoDTO.getFechaFin());
        
        // Actualizar supervisor
        if (contratoDTO.getSupervisorUuid() != null) {
            Usuario supervisor = usuarioRepository.findByUuid(contratoDTO.getSupervisorUuid())
                .orElseThrow(() -> new RuntimeException("Supervisor no encontrado"));
            contrato.setSupervisor(supervisor);
        } else {
            contrato.setSupervisor(null);
        }
        
        // Actualizar zonas si se proporcionan
        if (contratoDTO.getZonas() != null && !contratoDTO.getZonas().isEmpty()) {
            actualizarZonasContrato(contrato, contratoDTO.getZonas());
        }
        
        return contratoRepository.save(contrato);
    }

    public void cambiarEstado(UUID uuid, EstadoContrato nuevoEstado) {
        log.info("Cambiando estado del contrato {} a {}", uuid, nuevoEstado);
        
        Contrato contrato = buscarPorUuid(uuid);
        contrato.setEstado(nuevoEstado);
        contratoRepository.save(contrato);
    }

    public void eliminar(UUID uuid) {
        log.info("Eliminando contrato: {}", uuid);
        
        Contrato contrato = buscarPorUuid(uuid);
        
        // Verificar que no tenga zonas activas
        if (contrato.tieneZonas() || contrato.tienePredios()) {
            throw new RuntimeException("No se puede eliminar el contrato porque tiene zonas o predios asignados");
        }
        
        contratoRepository.delete(contrato);
    }

    // ==================== NUEVOS MÉTODOS PARA GESTIÓN DE ZONAS ====================

    public ContratoZona agregarZona(UUID contratoUuid, ContratoZonaDTO zonaDTO) {
        log.info("Agregando zona {} al contrato {}", zonaDTO.getZonaUuid(), contratoUuid);
        
        Contrato contrato = buscarPorUuid(contratoUuid);
        zonaDTO.setContratoUuid(contratoUuid);
        
        return crearContratoZona(contrato, zonaDTO);
    }

    public ContratoZona actualizarZona(UUID contratoZonaUuid, ContratoZonaDTO zonaDTO) {
        log.info("Actualizando zona del contrato: {}", contratoZonaUuid);
        
        ContratoZona contratoZona = contratoZonaRepository.findByUuid(contratoZonaUuid)
            .orElseThrow(() -> new RuntimeException("Zona del contrato no encontrada"));
        
        // Actualizar plan de tarifa
        if (zonaDTO.getPlanTarifaUuid() != null) {
            PlanTarifa planTarifa = planTarifaRepository.findByUuid(zonaDTO.getPlanTarifaUuid())
                .orElseThrow(() -> new RuntimeException("Plan de tarifa no encontrado"));
            contratoZona.setPlanTarifa(planTarifa);
        }
        
        // Actualizar coordinadores
        if (zonaDTO.getCoordinadorZonaUuid() != null) {
            Usuario coordinadorZona = usuarioRepository.findByUuid(zonaDTO.getCoordinadorZonaUuid())
                .orElseThrow(() -> new RuntimeException("Coordinador de zona no encontrado"));
            contratoZona.setCoordinadorZona(coordinadorZona);
        }
        
        if (zonaDTO.getCoordinadorOperativoUuid() != null) {
            Usuario coordinadorOperativo = usuarioRepository.findByUuid(zonaDTO.getCoordinadorOperativoUuid())
                .orElseThrow(() -> new RuntimeException("Coordinador operativo no encontrado"));
            contratoZona.setCoordinadorOperativo(coordinadorOperativo);
        }
        
        // Actualizar estado
        if (zonaDTO.getEstado() != null) {
            contratoZona.setEstado(zonaDTO.getEstado());
        }
        
        return contratoZonaRepository.save(contratoZona);
    }

    public void removerZona(UUID contratoZonaUuid) {
        log.info("Removiendo zona del contrato: {}", contratoZonaUuid);
        
        ContratoZona contratoZona = contratoZonaRepository.findByUuid(contratoZonaUuid)
            .orElseThrow(() -> new RuntimeException("Zona del contrato no encontrada"));
        
        // Verificar que no tenga predios asignados
        // TODO: Implementar verificación de predios en la zona
        
        contratoZona.setActivo(false);
        contratoZonaRepository.save(contratoZona);
    }

    @Transactional(readOnly = true)
    public List<ContratoZona> listarZonasDelContrato(UUID contratoUuid) {
        Contrato contrato = buscarPorUuid(contratoUuid);
        return contratoZonaRepository.findByContratoAndActivoTrueOrderByFechaCreacionAsc(contrato);
    }

    public ContratoZona asignarCoordinadorZona(UUID contratoZonaUuid, UUID coordinadorUuid) {
        log.info("Asignando coordinador de zona {} a la zona {}", coordinadorUuid, contratoZonaUuid);
        
        ContratoZona contratoZona = contratoZonaRepository.findByUuid(contratoZonaUuid)
            .orElseThrow(() -> new RuntimeException("Zona del contrato no encontrada"));
        
        Usuario coordinador = usuarioRepository.findByUuid(coordinadorUuid)
            .orElseThrow(() -> new RuntimeException("Coordinador no encontrado"));
        
        if (coordinador.getPerfil() != PerfilUsuario.COORDINADOR) {
            throw new RuntimeException("El usuario no tiene perfil de coordinador");
        }
        
        contratoZona.setCoordinadorZona(coordinador);
        return contratoZonaRepository.save(contratoZona);
    }

    public ContratoZona asignarCoordinadorOperativo(UUID contratoZonaUuid, UUID coordinadorUuid) {
        log.info("Asignando coordinador operativo {} a la zona {}", coordinadorUuid, contratoZonaUuid);
        
        ContratoZona contratoZona = contratoZonaRepository.findByUuid(contratoZonaUuid)
            .orElseThrow(() -> new RuntimeException("Zona del contrato no encontrada"));
        
        Usuario coordinador = usuarioRepository.findByUuid(coordinadorUuid)
            .orElseThrow(() -> new RuntimeException("Coordinador no encontrado"));
        
        if (coordinador.getPerfil() != PerfilUsuario.COORDINADOR) {
            throw new RuntimeException("El usuario no tiene perfil de coordinador");
        }
        
        contratoZona.setCoordinadorOperativo(coordinador);
        return contratoZonaRepository.save(contratoZona);
    }

    // ==================== MÉTODOS AUXILIARES PRIVADOS ====================

    private ContratoZona crearContratoZona(Contrato contrato, ContratoZonaDTO zonaDTO) {
        // Validar que no exista ya la zona en el contrato
        if (contratoZonaRepository.existsActiveByContratoUuidAndZonaUuid(contrato.getUuid(), zonaDTO.getZonaUuid())) {
            throw new RuntimeException("La zona ya está asignada al contrato");
        }
        
        // Buscar entidades relacionadas
        Zona zona = zonaRepository.findByUuid(zonaDTO.getZonaUuid())
            .orElseThrow(() -> new RuntimeException("Zona no encontrada"));
        
        PlanTarifa planTarifa = planTarifaRepository.findByUuid(zonaDTO.getPlanTarifaUuid())
            .orElseThrow(() -> new RuntimeException("Plan de tarifa no encontrado"));
        
        // Crear ContratoZona
        ContratoZona contratoZona = ContratoZona.builder()
            .contrato(contrato)
            .zona(zona)
            .planTarifa(planTarifa)
            .estado(EstadoContratoZona.ACTIVO)
            .activo(true)
            .build();
        
        // Asignar coordinadores si se proporcionan
        if (zonaDTO.getCoordinadorZonaUuid() != null) {
            Usuario coordinadorZona = usuarioRepository.findByUuid(zonaDTO.getCoordinadorZonaUuid())
                .orElseThrow(() -> new RuntimeException("Coordinador de zona no encontrado"));
            contratoZona.setCoordinadorZona(coordinadorZona);
        }
        
        if (zonaDTO.getCoordinadorOperativoUuid() != null) {
            Usuario coordinadorOperativo = usuarioRepository.findByUuid(zonaDTO.getCoordinadorOperativoUuid())
                .orElseThrow(() -> new RuntimeException("Coordinador operativo no encontrado"));
            contratoZona.setCoordinadorOperativo(coordinadorOperativo);
        }
        
        return contratoZonaRepository.save(contratoZona);
    }

    private void actualizarZonasContrato(Contrato contrato, List<ContratoZonaDTO> nuevasZonas) {
        // Obtener zonas actuales
        List<ContratoZona> zonasActuales = contratoZonaRepository.findByContratoAndActivoTrueOrderByFechaCreacionAsc(contrato);
        
        // Desactivar zonas que ya no están en la lista
        for (ContratoZona zonaActual : zonasActuales) {
            boolean existe = nuevasZonas.stream()
                .anyMatch(nueva -> nueva.getZonaUuid().equals(zonaActual.getZona().getUuid()));
            
            if (!existe) {
                zonaActual.setActivo(false);
                contratoZonaRepository.save(zonaActual);
            }
        }
        
        // Agregar o actualizar zonas nuevas
        for (ContratoZonaDTO nuevaZona : nuevasZonas) {
            Optional<ContratoZona> existente = zonasActuales.stream()
                .filter(z -> z.getZona().getUuid().equals(nuevaZona.getZonaUuid()))
                .findFirst();
            
            if (existente.isPresent()) {
                // Actualizar zona existente
                nuevaZona.setUuid(existente.get().getUuid());
                actualizarZona(existente.get().getUuid(), nuevaZona);
            } else {
                // Crear nueva zona
                crearContratoZona(contrato, nuevaZona);
            }
        }
    }

    // ==================== MÉTODOS EXISTENTES SIN CAMBIOS ====================
    // (Mantengo todos los métodos de asignación de supervisores y predios)

    public Contrato asignarSupervisor(UUID contratoUuid, UUID supervisorUuid) {
        log.info("Asignando supervisor {} al contrato {}", supervisorUuid, contratoUuid);
        
        Contrato contrato = buscarPorUuid(contratoUuid);
        Usuario supervisor = usuarioRepository.findByUuid(supervisorUuid)
            .orElseThrow(() -> new RuntimeException("Supervisor no encontrado"));
        
        if (supervisor.getPerfil() != PerfilUsuario.SUPERVISOR) {
            throw new RuntimeException("El usuario no tiene perfil de supervisor");
        }
        
        contrato.setSupervisor(supervisor);
        return contratoRepository.save(contrato);
    }

    // DEPRECATED: Estos métodos están obsoletos con la nueva estructura
    @Deprecated
    public Usuario agregarCoordinador(UUID contratoUuid, UUID coordinadorUuid) {
        log.warn("DEPRECATED: Use asignarCoordinadorZona o asignarCoordinadorOperativo en su lugar");
        throw new RuntimeException("Método obsoleto. Use asignarCoordinadorZona o asignarCoordinadorOperativo");
    }

    @Deprecated
    public void removerCoordinador(UUID contratoUuid, UUID coordinadorUuid) {
        log.warn("DEPRECATED: Los coordinadores ahora se gestionan por zona");
        throw new RuntimeException("Método obsoleto. Los coordinadores se gestionan por zona");
    }

    // ==================== MÉTODOS DE PREDIOS (SIN CAMBIOS) ====================

    public void agregarPredios(UUID contratoUuid, List<UUID> predioUuids) {
        log.info("Agregando {} predios al contrato {}", predioUuids.size(), contratoUuid);
        
        Contrato contrato = buscarPorUuid(contratoUuid);
        
        for (UUID predioUuid : predioUuids) {
            Predio predio = predioRepository.findByUuid(predioUuid)
                .orElseThrow(() -> new RuntimeException("Predio no encontrado: " + predioUuid));
            
            boolean yaExiste = contratoPredioRepository.existsByContratoAndPredio(contrato, predio);
            
            if (!yaExiste) {
                ContratoPredio contratoPredio = ContratoPredio.builder()
                    .contrato(contrato)
                    .predio(predio)
                    .estado(EstadoPredio.PENDIENTE)
                    .activo(true)
                    .build();
                
                contratoPredioRepository.save(contratoPredio);
            }
        }
    }

    public void removerPredio(UUID contratoUuid, UUID predioUuid) {
        log.info("Removiendo predio {} del contrato {}", predioUuid, contratoUuid);
        
        ContratoPredio contratoPredio = contratoPredioRepository
            .findByContratoUuidAndPredioUuid(contratoUuid, predioUuid)
            .orElseThrow(() -> new RuntimeException("Predio no encontrado en el contrato"));
        
        contratoPredio.setActivo(false);
        contratoPredioRepository.save(contratoPredio);
    }

    public ContratoPredio asignarOperarioAPredio(UUID contratoUuid, UUID predioUuid, UUID operarioUuid) {
        log.info("Asignando operario {} al predio {} del contrato {}", operarioUuid, predioUuid, contratoUuid);
        
        ContratoPredio contratoPredio = contratoPredioRepository
            .findByContratoUuidAndPredioUuid(contratoUuid, predioUuid)
            .orElseThrow(() -> new RuntimeException("Predio no encontrado en el contrato"));
        
        Usuario operario = usuarioRepository.findByUuid(operarioUuid)
            .orElseThrow(() -> new RuntimeException("Operario no encontrado"));
        
        if (operario.getPerfil() != PerfilUsuario.OPERARIO) {
            throw new RuntimeException("El usuario no tiene perfil de operario");
        }
        
        PredioOperario predioOperario = predioOperarioRepository
            .findByPredioAndContratoAndActivoTrue(contratoPredio.getPredio(), contratoPredio.getContrato())
            .orElse(PredioOperario.builder()
                .predio(contratoPredio.getPredio())
                .contrato(contratoPredio.getContrato())
                .build());
        
        predioOperario.setOperario(operario);
        predioOperario.setActivo(true);
        predioOperarioRepository.save(predioOperario);
        
        contratoPredio.setEstado(EstadoPredio.ASIGNADO);
        return contratoPredioRepository.save(contratoPredio);
    }

    public int asignarOperariosMasivo(UUID contratoUuid, List<AsignacionPredioOperario> asignaciones) {
        log.info("Realizando asignación masiva de {} operarios en contrato {}", 
            asignaciones.size(), contratoUuid);
        
        int asignacionesRealizadas = 0;
        
        for (AsignacionPredioOperario asignacion : asignaciones) {
            try {
                asignarOperarioAPredio(contratoUuid, asignacion.getPredioUuid(), asignacion.getOperarioUuid());
                asignacionesRealizadas++;
            } catch (Exception e) {
                log.error("Error asignando operario {} a predio {}: {}", 
                    asignacion.getOperarioUuid(), asignacion.getPredioUuid(), e.getMessage());
            }
        }
        
        return asignacionesRealizadas;
    }

    // ==================== CONSULTAS Y ESTADÍSTICAS (ACTUALIZADAS) ====================

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas(Contrato contrato) {
        Map<String, Object> stats = new HashMap<>();
        
        // Estadísticas de predios (sin cambios)
        long totalPredios = contratoPredioRepository.countByContrato(contrato);
        long prediosAsignados = contratoPredioRepository.countByContratoAndEstado(contrato, EstadoPredio.ASIGNADO);
        long prediosPendientes = contratoPredioRepository.countByContratoAndEstado(contrato, EstadoPredio.PENDIENTE);
        long prediosCompletados = contratoPredioRepository.countByContratoAndEstado(contrato, EstadoPredio.COMPLETADO);
        
        stats.put("totalPredios", totalPredios);
        stats.put("prediosAsignados", prediosAsignados);
        stats.put("prediosPendientes", prediosPendientes);
        stats.put("prediosCompletados", prediosCompletados);
        
        // NUEVAS estadísticas de zonas
        long totalZonas = contratoZonaRepository.countByContratoAndActivoTrue(contrato);
        long zonasCompletas = contratoZonaRepository.countByContratoAndEstado(contrato, EstadoContratoZona.COMPLETADO);
        
        stats.put("totalZonas", totalZonas);
        stats.put("zonasActivas", totalZonas);
        stats.put("zonasCompletas", zonasCompletas);
        
        // Estadísticas de coordinadores por zona
        List<ContratoZona> zonas = contratoZonaRepository.findByContratoAndActivoTrueOrderByFechaCreacionAsc(contrato);
        long coordinadoresZona = zonas.stream().filter(ContratoZona::tieneCoordinadorZona).count();
        long coordinadoresOperativos = zonas.stream().filter(ContratoZona::tieneCoordinadorOperativo).count();
        
        stats.put("totalCoordinadoresZona", coordinadoresZona);
        stats.put("totalCoordinadoresOperativos", coordinadoresOperativos);
        
        // Porcentajes
        stats.put("porcentajeAsignacion", totalPredios > 0 ? (prediosAsignados * 100.0 / totalPredios) : 0);
        stats.put("porcentajeCompletado", totalPredios > 0 ? (prediosCompletados * 100.0 / totalPredios) : 0);
        stats.put("porcentajeZonasCompletas", totalZonas > 0 ? (zonasCompletas * 100.0 / totalZonas) : 0);
        
        return stats;
    }

    @Transactional(readOnly = true)
    public List<ContratoPredio> obtenerPrediosDelContrato(UUID contratoUuid) {
        Contrato contrato = buscarPorUuid(contratoUuid);
        return contratoPredioRepository.findByContratoAndActivoTrue(contrato);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerResumenAsignaciones(UUID contratoUuid) {
        Contrato contrato = buscarPorUuid(contratoUuid);
        Map<String, Object> resumen = new HashMap<>();
        
        // Información básica del contrato
        resumen.put("codigo", contrato.getNumeroContrato());
        resumen.put("objetivo", contrato.getObjetivo());
        resumen.put("fechaInicio", contrato.getFechaInicio());
        resumen.put("fechaFin", contrato.getFechaFin());
        resumen.put("estado", contrato.getEstado());
        
        // Supervisor
        if (contrato.getSupervisor() != null) {
            resumen.put("supervisor", Map.of(
                "uuid", contrato.getSupervisor().getUuid(),
                "nombre", contrato.getSupervisor().getNombreCompleto()
            ));
        }
        
        // ACTUALIZADO: Coordinadores por zona
        List<ContratoZona> zonas = contratoZonaRepository.findByContratoAndActivoTrueOrderByFechaCreacionAsc(contrato);
        List<Map<String, Object>> zonasInfo = zonas.stream().map(zona -> {
            Map<String, Object> zonaInfo = new HashMap<>();
            zonaInfo.put("uuid", zona.getUuid());
            zonaInfo.put("zonaNombre", zona.getNombreZona());
            zonaInfo.put("planTarifa", zona.getNombrePlanTarifa());
            zonaInfo.put("coordinadorZona", zona.getNombreCoordinadorZona());
            zonaInfo.put("coordinadorOperativo", zona.getNombreCoordinadorOperativo());
            zonaInfo.put("estado", zona.getEstado());
            return zonaInfo;
        }).collect(Collectors.toList());
        
        resumen.put("zonas", zonasInfo);
        
        // Estadísticas
        Map<String, Object> stats = obtenerEstadisticas(contrato);
        resumen.putAll(stats);
        
        return resumen;
    }

    // ==================== MÉTODOS DE OPERARIOS (SIN CAMBIOS) ====================

    @Transactional(readOnly = true)
    public List<Usuario> obtenerOperariosDisponibles(Contrato contrato) {
        List<Usuario> todosOperarios = usuarioRepository.findByPerfilAndActivoTrue(PerfilUsuario.OPERARIO);
        List<Usuario> operariosAsignados = predioOperarioRepository.findOperariosByContrato(contrato);
        
        return todosOperarios.stream()
            .filter(op -> {
                long prediosAsignadosCount = predioOperarioRepository
                    .countByOperarioAndContratoAndActivoTrue(op, contrato);
                long totalPrediosContrato = contratoPredioRepository.countByContrato(contrato);
                return prediosAsignadosCount < totalPrediosContrato;
            })
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long contarPrediosAsignadosAOperario(Contrato contrato, Usuario operario) {
        return predioOperarioRepository.countByOperarioAndContratoAndActivoTrue(operario, contrato);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerOperariosConPredios(Contrato contrato) {
        List<Usuario> operarios = predioOperarioRepository.findOperariosByContrato(contrato);
        
        return operarios.stream().map(operario -> {
            Map<String, Object> info = new HashMap<>();
            info.put("uuid", operario.getUuid());
            info.put("nombre", operario.getNombre() + " " + operario.getApellido());
            info.put("username", operario.getUsername());
            
            List<PredioOperario> prediosAsignados = predioOperarioRepository
                .findByOperarioAndContratoAndActivoTrue(operario, contrato);
            
            info.put("prediosAsignados", prediosAsignados.stream().map(po -> Map.of(
                "uuid", po.getPredio().getUuid(),
                "direccion", po.getPredio().getDireccion(),
                "tipo", po.getPredio().getTipo()
            )).collect(Collectors.toList()));
            
            info.put("totalPrediosAsignados", prediosAsignados.size());
            
            return info;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long contarPrediosAsignados(Contrato contrato) {
        return predioOperarioRepository.countByContratoAndActivoTrue(contrato);
    }

    @Transactional(readOnly = true)
    public long contarPrediosSinAsignar(Contrato contrato) {
        long totalPredios = contratoPredioRepository.countByContrato(contrato);
        long prediosAsignados = predioOperarioRepository.countByContratoAndActivoTrue(contrato);
        return totalPredios - prediosAsignados;
    }

    @Transactional(readOnly = true)
    public List<Usuario> obtenerOperariosDelContrato(Contrato contrato) {
        return predioOperarioRepository.findOperariosByContrato(contrato);
    }

    // ==================== NUEVOS MÉTODOS DE CONSULTA POR ZONAS ====================

    @Transactional(readOnly = true)
    public List<ContratoZona> buscarZonasPorCoordinador(Usuario coordinador) {
        return contratoZonaRepository.findZonasByCoordinador(coordinador);
    }

    @Transactional(readOnly = true)
    public List<ContratoZona> buscarZonasSinCoordinador(UUID contratoUuid) {
        Contrato contrato = buscarPorUuid(contratoUuid);
        return contratoZonaRepository.findZonasSinCoordinadorCompleto(contrato);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticasZonas(UUID contratoUuid) {
        Contrato contrato = buscarPorUuid(contratoUuid);
        Map<String, Object> stats = new HashMap<>();
        
        // Estadísticas por estado de zona
        List<Object[]> estadisticasEstados = contratoZonaRepository.findEstadisticasEstadosByContrato(contrato);
        Map<String, Long> estadosPorCantidad = estadisticasEstados.stream()
            .collect(Collectors.toMap(
                obj -> ((EstadoContratoZona) obj[0]).getDescripcion(),
                obj -> (Long) obj[1]
            ));
        
        stats.put("estadosPorCantidad", estadosPorCantidad);
        
        // Zonas que necesitan atención
        List<ContratoZona> zonasQueNecesitanAtencion = contratoZonaRepository.findZonasQueNecesitanAtencion()
            .stream()
            .filter(zona -> zona.getContrato().getUuid().equals(contratoUuid))
            .collect(Collectors.toList());
        
        stats.put("zonasQueNecesitanAtencion", zonasQueNecesitanAtencion.size());
        
        return stats;
    }

    @Transactional(readOnly = true)
    public boolean verificarDisponibilidadCoordinador(UUID coordinadorUuid, 
                                                     java.time.LocalDate fechaInicio, 
                                                     java.time.LocalDate fechaFin) {
        Usuario coordinador = usuarioRepository.findByUuid(coordinadorUuid)
            .orElseThrow(() -> new RuntimeException("Coordinador no encontrado"));
        
        long conflictos = contratoZonaRepository.countConflictosCoordinador(coordinador, fechaInicio, fechaFin);
        return conflictos == 0;
    }

    // ==================== MÉTODOS DE CONVERSIÓN DTO ====================

    public ContratoZonaDTO convertirZonaADTO(ContratoZona contratoZona) {
        return ContratoZonaDTO.builder()
            .uuid(contratoZona.getUuid())
            .contratoUuid(contratoZona.getContrato().getUuid())
            .zonaUuid(contratoZona.getZona().getUuid())
            .planTarifaUuid(contratoZona.getPlanTarifa().getUuid())
            .coordinadorZonaUuid(contratoZona.getCoordinadorZona() != null ? 
                contratoZona.getCoordinadorZona().getUuid() : null)
            .coordinadorOperativoUuid(contratoZona.getCoordinadorOperativo() != null ? 
                contratoZona.getCoordinadorOperativo().getUuid() : null)
            .estado(contratoZona.getEstado())
            .activo(contratoZona.getActivo())
            .fechaCreacion(contratoZona.getFechaCreacion())
            .fechaActualizacion(contratoZona.getFechaActualizacion())
            
            // Campos adicionales para vistas
            .contratoNumero(contratoZona.getContrato().getNumeroContrato())
            .zonaNombre(contratoZona.getNombreZona())
            .planTarifaNombre(contratoZona.getNombrePlanTarifa())
            .coordinadorZonaNombre(contratoZona.getNombreCoordinadorZona())
            .coordinadorOperativoNombre(contratoZona.getNombreCoordinadorOperativo())
            .build();
    }

    public ContratoDTO convertirADTOCompleto(Contrato contrato) {
        ContratoDTO dto = ContratoDTO.builder()
            .uuid(contrato.getUuid())
            .codigo(contrato.getNumeroContrato())
            .objetivo(contrato.getObjetivo())
            .fechaInicio(contrato.getFechaInicio())
            .fechaFin(contrato.getFechaFin())
            .estado(contrato.getEstado())
            .supervisorUuid(contrato.getSupervisor() != null ? contrato.getSupervisor().getUuid() : null)
            .supervisorNombre(contrato.getNombreSupervisor())
            .build();
        
        // Agregar zonas
        List<ContratoZona> zonas = contratoZonaRepository.findByContratoAndActivoTrueOrderByFechaCreacionAsc(contrato);
        List<ContratoZonaDTO> zonasDTO = zonas.stream()
            .map(this::convertirZonaADTO)
            .collect(Collectors.toList());
        
        dto.setZonas(zonasDTO);
        
        // Agregar estadísticas
        Map<String, Object> stats = obtenerEstadisticas(contrato);
        dto.setTotalPredios((Integer) stats.get("totalPredios"));
        dto.setPrediosAsignados((Integer) stats.get("prediosAsignados"));
        dto.setTotalZonas((Integer) stats.get("totalZonas"));
        dto.setTotalCoordinadoresZona((Integer) stats.get("totalCoordinadoresZona"));
        dto.setTotalCoordinadoresOperativos((Integer) stats.get("totalCoordinadoresOperativos"));
        dto.setPorcentajeAvance((Double) stats.get("porcentajeCompletado"));
        
        return dto;
    }

    // ==================== MÉTODOS DE MIGRACIÓN/COMPATIBILIDAD ====================

    /**
     * Método temporal para migrar contratos con zona única a múltiples zonas
     * TODO: Remover después de la migración completa
     */
    @Deprecated
    public void migrarContratoAMultiplesZonas(UUID contratoUuid, UUID planTarifaUuid) {
        log.warn("MIGRACIÓN: Convirtiendo contrato {} a múltiples zonas", contratoUuid);
        
        // Este método sería usado durante la migración de datos
        // No es necesario implementarlo completamente ahora
        throw new RuntimeException("Método de migración - implementar según necesidades específicas");
    }
// ==================== MÉTODOS FALTANTES PARA ZonaController ====================

// Agregar estos métodos al ContratoService.java existente:

/**
 * Verificar si un coordinador está asignado a un contrato específico
 */
    @Transactional(readOnly = true)
    public boolean esCoordinadorDeContrato(UUID contratoUuid, UUID usuarioUuid) {
        log.debug("Verificando si usuario {} es coordinador del contrato {}", usuarioUuid, contratoUuid);
        
        try {
            Contrato contrato = buscarPorUuid(contratoUuid);
            Usuario usuario = usuarioRepository.findByUuid(usuarioUuid)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            return contratoZonaRepository.esCoordinadorDeContrato(contratoUuid, usuario);
        } catch (Exception e) {
            log.error("Error al verificar coordinador de contrato: ", e);
            return false;
        }
    }

    /**
     * Verificar si un usuario puede gestionar un contrato específico
     */
    @Transactional(readOnly = true)
    public boolean puedeGestionarContrato(UUID contratoUuid, UUID usuarioUuid) {
        log.debug("Verificando si usuario {} puede gestionar contrato {}", usuarioUuid, contratoUuid);
        
        try {
            Contrato contrato = buscarPorUuid(contratoUuid);
            Usuario usuario = usuarioRepository.findByUuid(usuarioUuid)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Administradores pueden gestionar cualquier contrato
            if (usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR) {
                return true;
            }
            
            // Supervisores pueden gestionar contratos asignados a ellos
            if (usuario.getPerfil() == PerfilUsuario.SUPERVISOR) {
                return contrato.getSupervisor() != null && 
                    contrato.getSupervisor().getUuid().equals(usuarioUuid);
            }
            
            // Coordinadores pueden gestionar contratos donde están asignados
            if (usuario.getPerfil() == PerfilUsuario.COORDINADOR) {
                return contratoZonaRepository.esCoordinadorDeContrato(contratoUuid, usuario);
            }
            
            return false;
        } catch (Exception e) {
            log.error("Error al verificar gestión de contrato: ", e);
            return false;
        }
    }

    /**
     * Listar contratos accesibles por un usuario específico
     */
    @Transactional(readOnly = true)
    public List<ContratoDTO> listarAccesiblesPorUsuario(UUID usuarioUuid) {
        log.info("Listando contratos accesibles para usuario: {}", usuarioUuid);
        
        try {
            Usuario usuario = usuarioRepository.findByUuid(usuarioUuid)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            List<Contrato> contratos;
            
            switch (usuario.getPerfil()) {
                case ADMINISTRADOR:
                    // Administradores ven todos los contratos
                    contratos = contratoRepository.findAccesiblesPorUsuario(usuarioUuid, PerfilUsuario.ADMINISTRADOR);
                    break;
                    
                case SUPERVISOR:
                    // Supervisores ven contratos asignados a ellos
                    contratos = contratoRepository.findBySupervisorOrderByFechaInicioDesc(usuario);
                    break;
                    
                case COORDINADOR:
                    // Coordinadores ven contratos donde están asignados
                    contratos = contratoRepository.findByCoordinador(usuario);
                    break;
                    
                case OPERARIO:
                    // Operarios ven contratos donde tienen predios asignados
                    contratos = contratoRepository.findContratosConOperario(usuario);
                    break;
                    
                default:
                    contratos = new ArrayList<>();
            }
            
            return contratos.stream()
                .map(this::convertirADTOBasico)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Error al listar contratos accesibles: ", e);
            return new ArrayList<>();
        }
    }

    /**
     * Buscar contrato por UUID y retornar DTO
     */
    @Transactional(readOnly = true)
    public ContratoDTO buscarPorUuidDTO(UUID uuid) {
        log.debug("Buscando contrato por UUID: {}", uuid);
        
        Contrato contrato = contratoRepository.findByUuid(uuid)
            .orElseThrow(() -> new RuntimeException("Contrato no encontrado: " + uuid));
        
        return convertirADTOCompleto(contrato);
    }

    /**
     * Listar todos los contratos como DTO
     */
    @Transactional(readOnly = true)
    public List<Contrato> listarTodos() {
        log.info("Listando todos los contratos como DTO");
        
        List<Contrato> contratos = contratoRepository.findAllByOrderByFechaInicioDesc();
        return contratos;
    }

    /**
     * Verificar si un contrato tiene zonas asignadas
     */
    @Transactional(readOnly = true)
    public boolean tieneZonasAsignadas(UUID contratoUuid) {
        try {
            Contrato contrato = contratoRepository.findByUuid(contratoUuid)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));
            
            long zonasActivas = contratoZonaRepository.countByContratoAndActivoTrue(contrato);
            return zonasActivas > 0;
        } catch (Exception e) {
            log.error("Error al verificar zonas del contrato: ", e);
            return false;
        }
    }

    /**
     * Obtener coordinadores únicos de un contrato
     */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> obtenerCoordinadoresDelContrato(UUID contratoUuid) {
        log.info("Obteniendo coordinadores del contrato: {}", contratoUuid);
        
        try {
            Contrato contrato = contratoRepository.findByUuid(contratoUuid)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));
            
            List<ContratoZona> zonas = contratoZonaRepository.findByContratoAndActivoTrueOrderByFechaCreacionAsc(contrato);
            
            Set<Usuario> coordinadores = new HashSet<>();
            
            for (ContratoZona zona : zonas) {
                if (zona.getCoordinadorZona() != null) {
                    coordinadores.add(zona.getCoordinadorZona());
                }
                if (zona.getCoordinadorOperativo() != null) {
                    coordinadores.add(zona.getCoordinadorOperativo());
                }
            }
            
            return coordinadores.stream()
                .map(this::convertirUsuarioADTO)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Error al obtener coordinadores del contrato: ", e);
            return new ArrayList<>();
        }
    }

    /**
     * Validar si un contrato puede ser eliminado
     */
    @Transactional(readOnly = true)
    public boolean puedeSerEliminado(UUID contratoUuid) {
        try {
            Contrato contrato = contratoRepository.findByUuid(contratoUuid)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));
            
            // No puede eliminarse si tiene zonas activas
            long zonasActivas = contratoZonaRepository.countByContratoAndActivoTrue(contrato);
            if (zonasActivas > 0) {
                return false;
            }
            
            // No puede eliminarse si tiene predios asignados
            long prediosAsignados = contratoPredioRepository.countByContratoAndActivoTrue(contrato);
            return prediosAsignados == 0;
            
        } catch (Exception e) {
            log.error("Error al validar eliminación de contrato: ", e);
            return false;
        }
    }

    /**
     * Buscar contratos por múltiples filtros
     */
    @Transactional(readOnly = true)
    public Page<ContratoDTO> buscarConFiltros(String numeroContrato, String empresa, EstadoContrato estado,
                                            Boolean activo, java.time.LocalDate fechaDesde, 
                                            java.time.LocalDate fechaHasta, Pageable pageable) {
        log.info("Buscando contratos con filtros");
        
        Page<Contrato> contratos = contratoRepository.findConFiltros(
            numeroContrato, empresa, estado, fechaDesde, fechaHasta, pageable);
        
        return contratos.map(this::convertirADTOBasico);
    }

    /**
     * Obtener resumen ejecutivo de contratos
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerResumenEjecutivo() {
        log.info("Obteniendo resumen ejecutivo de contratos");
        
        try {
            List<Object[]> resultados = contratoRepository.findResumenEjecutivo();
            
            return resultados.stream().map(resultado -> {
                Map<String, Object> resumen = new HashMap<>();
                resumen.put("numeroContrato", resultado[0]);
                resumen.put("estado", resultado[1]);
                resumen.put("fechaInicio", resultado[2]);
                resumen.put("fechaFin", resultado[3]);
                resumen.put("totalZonas", resultado[4]);
                resumen.put("zonasCompletas", resultado[5]);
                
                // Calcular porcentaje de completitud
                Long totalZonas = (Long) resultado[5];
                Long zonasCompletas = (Long) resultado[6];
                double porcentaje = totalZonas > 0 ? (zonasCompletas.doubleValue() / totalZonas.doubleValue()) * 100 : 0;
                resumen.put("porcentajeCompletitud", porcentaje);
                
                return resumen;
            }).collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Error al obtener resumen ejecutivo: ", e);
            return new ArrayList<>();
        }
    }

    /**
     * Obtener contratos próximos a vencer
     */
    @Transactional(readOnly = true)
    public List<ContratoDTO> obtenerProximosAVencer(int diasAnticipacion) {
        log.info("Obteniendo contratos próximos a vencer en {} días", diasAnticipacion);
        
        try {
            java.time.LocalDate fechaLimite = java.time.LocalDate.now().plusDays(diasAnticipacion);
            List<Contrato> contratos = contratoRepository.findProximosAVencer(fechaLimite);
            
            return contratos.stream()
                .map(this::convertirADTOBasico)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Error al obtener contratos próximos a vencer: ", e);
            return new ArrayList<>();
        }
    }

    /**
     * Obtener estadísticas generales de contratos
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticasGenerales() {
        log.info("Obteniendo estadísticas generales de contratos");
        
        Map<String, Object> estadisticas = new HashMap<>();
        
        try {
            // Estadísticas básicas
            estadisticas.put("totalContratos", contratoRepository.count());
            estadisticas.put("contratosActivos", contratoRepository.countByEstado(EstadoContrato.ACTIVO));
            estadisticas.put("contratosPausados", contratoRepository.countByEstado(EstadoContrato.SUSPENDIDO));
            estadisticas.put("contratosFinalizados", contratoRepository.countByEstado(EstadoContrato.FINALIZADO));
            estadisticas.put("contratosVigentes", contratoRepository.countVigentes());
            
            // Estadísticas por estado
            List<Object[]> estadisticasPorEstado = contratoRepository.findEstadisticasPorEstado();
            Map<String, Long> estadosPorCantidad = estadisticasPorEstado.stream()
                .collect(Collectors.toMap(
                    obj -> ((EstadoContrato) obj[0]).name(),
                    obj -> (Long) obj[1]
                ));
            estadisticas.put("estadosPorCantidad", estadosPorCantidad);
            
            // Contratos por mes (últimos 12 meses)
            java.time.LocalDate fechaDesde = java.time.LocalDate.now().minusMonths(12);
            List<Object[]> contratosPorMes = contratoRepository.findContratosUltimos12Meses(fechaDesde);
            estadisticas.put("contratosPorMes", contratosPorMes);
            
            // Contratos sin zonas
            List<Contrato> contratosSinZonas = contratoRepository.findSinZonasAsignadas();
            estadisticas.put("contratosSinZonas", contratosSinZonas.size());
            
            // Contratos con zonas incompletas
            List<Contrato> contratosIncompletos = contratoRepository.findConZonasIncompletas();
            estadisticas.put("contratosConZonasIncompletas", contratosIncompletos.size());
            
        } catch (Exception e) {
            log.error("Error al obtener estadísticas generales: ", e);
        }
        
        return estadisticas;
    }

    /**
     * Clonar contrato con sus zonas
     */
    public ContratoDTO clonarContrato(UUID contratoOrigenUuid, ContratoDTO nuevoDatoContrato) {
        log.info("Clonando contrato: {}", contratoOrigenUuid);
        
        try {
            Contrato contratoOrigen = contratoRepository.findByUuid(contratoOrigenUuid)
                .orElseThrow(() -> new RuntimeException("Contrato origen no encontrado"));
            
            // Crear nuevo contrato
            Contrato nuevoContrato = Contrato.builder()
                .numeroContrato(nuevoDatoContrato.getCodigo())
                .objetivo(nuevoDatoContrato.getObjetivo())
                .fechaInicio(nuevoDatoContrato.getFechaInicio())
                .fechaFin(nuevoDatoContrato.getFechaFin())
                .estado(EstadoContrato.ACTIVO)
                .build();
            
            if (nuevoDatoContrato.getSupervisorUuid() != null) {
                Usuario supervisor = usuarioRepository.findByUuid(nuevoDatoContrato.getSupervisorUuid())
                    .orElseThrow(() -> new RuntimeException("Supervisor no encontrado"));
                nuevoContrato.setSupervisor(supervisor);
            }
            
            nuevoContrato = contratoRepository.save(nuevoContrato);
            
            // Clonar zonas del contrato origen
            List<ContratoZona> zonasOrigen = contratoZonaRepository.findByContratoAndActivoTrueOrderByFechaCreacionAsc(contratoOrigen);
            
            for (ContratoZona zonaOrigen : zonasOrigen) {
                ContratoZona nuevaZona = ContratoZona.builder()
                    .contrato(nuevoContrato)
                    .zona(zonaOrigen.getZona())
                    .planTarifa(zonaOrigen.getPlanTarifa())
                    .coordinadorZona(zonaOrigen.getCoordinadorZona())
                    .coordinadorOperativo(zonaOrigen.getCoordinadorOperativo())
                    .estado(EstadoContratoZona.ACTIVO)
                    .activo(true)
                    .build();
                
                contratoZonaRepository.save(nuevaZona);
            }
            
            log.info("✅ Contrato clonado exitosamente con {} zonas", zonasOrigen.size());
            return convertirADTOCompleto(nuevoContrato);
            
        } catch (Exception e) {
            log.error("Error al clonar contrato: ", e);
            throw new RuntimeException("Error al clonar contrato: " + e.getMessage());
        }
    }

    // ==================== MÉTODOS DE CONVERSIÓN DTO ADICIONALES ====================

    /**
     * Convertir contrato a DTO básico (sin zonas ni estadísticas completas)
     */
    private ContratoDTO convertirADTOBasico(Contrato contrato) {
        return ContratoDTO.builder()
            .uuid(contrato.getUuid())
            .codigo(contrato.getNumeroContrato())
            .objetivo(contrato.getObjetivo())
            .fechaInicio(contrato.getFechaInicio())
            .fechaFin(contrato.getFechaFin())
            .estado(contrato.getEstado())
            .supervisorUuid(contrato.getSupervisor() != null ? contrato.getSupervisor().getUuid() : null)
            .supervisorNombre(contrato.getNombreSupervisor())
            .build();
    }

    /**
     * Convertir Usuario a DTO básico
     */
    private UsuarioDTO convertirUsuarioADTO(Usuario usuario) {
        return UsuarioDTO.builder()
            .uuid(usuario.getUuid())
            .nombre(usuario.getNombre())
            .email(usuario.getEmail())
            .username(usuario.getUsername())
            .perfil(usuario.getPerfil())
            .activo(usuario.getActivo())
            .build();
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Validar que las fechas del contrato sean válidas
     */
    private void validarFechasContrato(java.time.LocalDate fechaInicio, java.time.LocalDate fechaFin) {
        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha fin no puede ser anterior a la fecha inicio");
        }
        
        if (fechaInicio.isBefore(java.time.LocalDate.now().minusYears(1))) {
            throw new IllegalArgumentException("La fecha inicio no puede ser anterior a un año");
        }
        
        if (fechaFin.isAfter(java.time.LocalDate.now().plusYears(10))) {
            throw new IllegalArgumentException("La fecha fin no puede ser superior a 10 años");
        }
    }

    /**
     * Validar que el contrato tenga los datos mínimos requeridos
     */
    private void validarDatosMinimosContrato(ContratoDTO contratoDTO) {
        if (contratoDTO.getCodigo() == null || contratoDTO.getCodigo().trim().isEmpty()) {
            throw new IllegalArgumentException("El código del contrato es obligatorio");
        }
        
        if (contratoDTO.getObjetivo() == null || contratoDTO.getObjetivo().trim().isEmpty()) {
            throw new IllegalArgumentException("El objetivo del contrato es obligatorio");
        }
        
        if (contratoDTO.getFechaInicio() == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria");
        }
        
        if (contratoDTO.getFechaFin() == null) {
            throw new IllegalArgumentException("La fecha de fin es obligatoria");
        }
    }

    /**
     * Logging de auditoría para operaciones críticas
     */
    private void auditarOperacion(String operacion, Object entidad, UUID usuarioUuid) {
        log.info("AUDITORIA - Operación: {}, Entidad: {}, Usuario: {}, Timestamp: {}", 
            operacion, entidad.toString(), usuarioUuid, java.time.LocalDateTime.now());
    }    
}