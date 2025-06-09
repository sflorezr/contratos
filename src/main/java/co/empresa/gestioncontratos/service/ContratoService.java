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

    // ==================== CONSULTAS ====================

    @Transactional(readOnly = true)
    public List<Contrato> listarTodos() {
        log.info("Listando todos los contratos");
        return contratoRepository.findAllByOrderByFechaInicioDesc();
    }

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
        return contratoRepository.findByCoordinadoresContainingOrderByFechaInicioDesc(coordinador);
    }
/* 
    @Transactional(readOnly = true)
    public List<Usuario> listarPorOperario(Contrato contrato) {
        log.info("Listando contratos del operario: {}", contrato.getNumeroContrato());
        return contratoRepository.findOperariosPorContrato(contrato);
    }
*/
    @Transactional(readOnly = true)
    public List<Contrato> listarContratoPorOperario(Usuario operario) {
        log.info("Listando contratos del operario: {}", operario.getUsername());
        return contratoRepository.findContratosConOperarioEnPredios(operario);
    }
    @Transactional(readOnly = true)
    public Contrato buscarPorUuid(UUID uuid) {
        return contratoRepository.findByUuid(uuid)
            .orElseThrow(() -> new RuntimeException("Contrato no encontrado: " + uuid));
    }

    @Transactional(readOnly = true)
    public Contrato buscarPorNumeroContrato(String numeroContrato) {
        return contratoRepository.findByNumeroContrato(numeroContrato)
            .orElseThrow(() -> new RuntimeException("Contrato no encontrado con número: " + numeroContrato));
    }

    // ==================== GESTIÓN DE CONTRATOS ====================

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
        
        Contrato contrato = Contrato.builder()
            .numeroContrato(contratoDTO.getCodigo())
            .objetivo(contratoDTO.getObjetivo())
            .zona(zonaRepository.findByUuid(contratoDTO.getZonaUuid())
                .orElseThrow(() -> new RuntimeException("Zona no encontrada")))
            .fechaInicio(contratoDTO.getFechaInicio())
            .fechaFin(contratoDTO.getFechaFin())
            .planTarifa(planTarifaRepository.findByUuid(contratoDTO.getPlanTarifaUuid())
                .orElseThrow(() -> new RuntimeException("Plan de tarifa no encontrado")))
            .estado(EstadoContrato.ACTIVO)
            .build();
        
        return contratoRepository.save(contrato);
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
        
        contrato.setNumeroContrato(contratoDTO.getCodigo());
        contrato.setObjetivo(contratoDTO.getObjetivo());
        contrato.setFechaInicio(contratoDTO.getFechaInicio());
        contrato.setFechaFin(contratoDTO.getFechaFin());
        if (contratoDTO.getSupervisorUuid() != null) {
            contrato.setSupervisor(usuarioRepository.findByUuid(contratoDTO.getSupervisorUuid())
                .orElseThrow(() -> new RuntimeException("Supervisor no encontrado")));
        }
                
        
        if (contratoDTO.getZonaUuid() != null) {
            contrato.setZona(zonaRepository.findByUuid(contratoDTO.getZonaUuid())
                .orElseThrow(() -> new RuntimeException("Zona no encontrada")));
        }
        
        if (contratoDTO.getPlanTarifaUuid() != null) {
            contrato.setPlanTarifa(planTarifaRepository.findByUuid(contratoDTO.getPlanTarifaUuid())
                .orElseThrow(() -> new RuntimeException("Plan de tarifa no encontrado")));
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
        
        if (contrato.tieneCoordinadores() || contrato.tienePredios()) {
            throw new RuntimeException("No se puede eliminar el contrato porque tiene asignaciones activas");
        }
        
        contratoRepository.delete(contrato);
    }

    // ==================== ASIGNACIÓN DE USUARIOS ====================

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

    public Usuario agregarCoordinador(UUID contratoUuid, UUID coordinadorUuid) {
        log.info("Agregando coordinador {} al contrato {}", coordinadorUuid, contratoUuid);
        
        Contrato contrato = buscarPorUuid(contratoUuid);
        Usuario coordinador = usuarioRepository.findByUuid(coordinadorUuid)
            .orElseThrow(() -> new RuntimeException("Coordinador no encontrado"));
        
        if (coordinador.getPerfil() != PerfilUsuario.COORDINADOR) {
            throw new RuntimeException("El usuario no tiene perfil de coordinador");
        }
        
        if (contrato.getCoordinadores().contains(coordinador)) {
            throw new RuntimeException("El coordinador ya está asignado al contrato");
        }
        
        contrato.getCoordinadores().add(coordinador);
        contratoRepository.save(contrato);
        
        return coordinador;
    }

    public void removerCoordinador(UUID contratoUuid, UUID coordinadorUuid) {
        log.info("Removiendo coordinador {} del contrato {}", coordinadorUuid, contratoUuid);
        
        Contrato contrato = buscarPorUuid(contratoUuid);
        Usuario coordinador = usuarioRepository.findByUuid(coordinadorUuid)
            .orElseThrow(() -> new RuntimeException("Coordinador no encontrado"));
        
        if (!contrato.getCoordinadores().contains(coordinador)) {
            throw new RuntimeException("El coordinador no está asignado al contrato");
        }
        
        contrato.getCoordinadores().remove(coordinador);
        contratoRepository.save(contrato);
    }

    // ==================== GESTIÓN DE PREDIOS ====================

    public void agregarPredios(UUID contratoUuid, List<UUID> predioUuids) {
        log.info("Agregando {} predios al contrato {}", predioUuids.size(), contratoUuid);
        
        Contrato contrato = buscarPorUuid(contratoUuid);
        
        for (UUID predioUuid : predioUuids) {
            Predio predio = predioRepository.findByUuid(predioUuid)
                .orElseThrow(() -> new RuntimeException("Predio no encontrado: " + predioUuid));
            
            // Verificar que el predio no esté ya en el contrato
            boolean yaExiste = contratoPredioRepository
                .existsByContratoAndPredio(contrato, predio);
            
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

    // ==================== CONSULTAS Y ESTADÍSTICAS ====================

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas(Contrato contrato) {
        Map<String, Object> stats = new HashMap<>();
        
        long totalPredios = contratoPredioRepository.countByContrato(contrato);
        long prediosAsignados = contratoPredioRepository.countByContratoAndEstado(contrato, EstadoPredio.ASIGNADO);
        long prediosPendientes = contratoPredioRepository.countByContratoAndEstado(contrato, EstadoPredio.PENDIENTE);
        long prediosCompletados = contratoPredioRepository.countByContratoAndEstado(contrato, EstadoPredio.COMPLETADO);
        stats.put("totalPredios", totalPredios);
        stats.put("prediosAsignados", prediosAsignados);
        stats.put("prediosPendientes", prediosPendientes);
        stats.put("prediosCompletados", prediosCompletados);
        stats.put("totalSectores", 1);
        stats.put("sectoresActivos", 1);
        stats.put("porcentajeAsignacion", totalPredios > 0 ? (prediosAsignados * 100.0 / totalPredios) : 0);
        stats.put("porcentajeCompletado", totalPredios > 0 ? (prediosCompletados * 100.0 / totalPredios) : 0);
        
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
        resumen.put("zonaNombre", contrato.getNombreZona());
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
        
        // Coordinadores
        resumen.put("coordinadores", contrato.getCoordinadores().stream()
            .map(coord -> Map.of(
                "uuid", coord.getUuid(),
                "nombre", coord.getNombreCompleto()
            ))
            .collect(Collectors.toList()));
        
        // Estadísticas
        Map<String, Object> stats = obtenerEstadisticas(contrato);
        resumen.putAll(stats);
        
        return resumen;
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
        
        // Crear o actualizar la asignación en PredioOperario
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
        @Transactional(readOnly = true)
    public List<Usuario> obtenerOperariosDisponibles(Contrato contrato) {
        // Obtener todos los operarios activos
        List<Usuario> todosOperarios = usuarioRepository.findByPerfilAndActivoTrue(PerfilUsuario.OPERARIO);
        
        // Obtener operarios ya asignados al contrato
        List<Usuario> operariosAsignados = predioOperarioRepository.findOperariosByContrato(contrato);
        
        // Filtrar operarios que aún pueden ser asignados a más predios
        return todosOperarios.stream()
            .filter(op -> {
                long prediosAsignadosCount = predioOperarioRepository
                    .countByOperarioAndContratoAndActivoTrue(op, contrato);
                long totalPrediosContrato = contratoPredioRepository.countByContrato(contrato);
                // Un operario está disponible si no está asignado a todos los predios
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
    @Transactional(readOnly = true)
    public List<Contrato> listarPorOperario(Usuario operario) {
        log.info("Listando contratos del operario: {}", operario.getUsername());
        
        // Verificar que el usuario sea operario
        if (operario.getPerfil() != PerfilUsuario.OPERARIO) {
            log.warn("El usuario {} no es operario, perfil: {}", operario.getUsername(), operario.getPerfil());
            return new ArrayList<>();
        }
        
        // Obtener contratos donde el operario tiene predios asignados
        List<Contrato> contratos = contratoRepository.findContratosConOperario(operario);
        
        log.info("Operario {} tiene acceso a {} contratos", operario.getUsername(), contratos.size());
        
        return contratos;
    }    
    }