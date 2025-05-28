package co.empresa.gestioncontratos.service;

import co.empresa.gestioncontratos.entity.*;
import co.empresa.gestioncontratos.enums.PerfilUsuario;
import co.empresa.gestioncontratos.repository.*;
import co.empresa.gestioncontratos.dto.ContratoDTO;
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
    private final ActividadRepository actividadRepository;

    // ==================== CONSULTAS ====================

    @Transactional(readOnly = true)
    public List<Contrato> listarTodos() {
        log.info("Listando todos los contratos");
        return contratoRepository.findAllByOrderByFechaInicioDesc();
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

    @Transactional(readOnly = true)
    public List<Contrato> listarPorOperario(Usuario operario) {
        log.info("Listando contratos del operario: {}", operario.getUsername());
        return contratoRepository.findContratosConOperario(operario);
    }

    @Transactional(readOnly = true)
    public Contrato buscarPorUuid(UUID uuid) {
        return contratoRepository.findByUuid(uuid)
            .orElseThrow(() -> new RuntimeException("Contrato no encontrado: " + uuid));
    }

    @Transactional(readOnly = true)
    public Contrato buscarPorCodigo(String codigo) {
        return contratoRepository.findByCodigo(codigo)
            .orElseThrow(() -> new RuntimeException("Contrato no encontrado con código: " + codigo));
    }

    // ==================== GESTIÓN DE CONTRATOS ====================

    public Contrato crear(ContratoDTO contratoDTO) {
        log.info("Creando nuevo contrato: {}", contratoDTO.getCodigo());
        
        // Validar que no exista el código
        if (contratoRepository.existsByCodigo(contratoDTO.getCodigo())) {
            throw new RuntimeException("Ya existe un contrato con el código: " + contratoDTO.getCodigo());
        }
        
        // Validar fechas
        if (contratoDTO.getFechaFin().isBefore(contratoDTO.getFechaInicio())) {
            throw new RuntimeException("La fecha fin no puede ser anterior a la fecha inicio");
        }
        
        Contrato contrato = Contrato.builder()
            .codigo(contratoDTO.getCodigo())
            .objetivo(contratoDTO.getObjetivo())
            .sector(sectorRepository.findByUuid(contratoDTO.getSectorUuid())
                .orElseThrow(() -> new RuntimeException("Sector no encontrado")))
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
        
        // Validar código único si cambió
        if (!contrato.getCodigo().equals(contratoDTO.getCodigo()) &&
            contratoRepository.existsByCodigo(contratoDTO.getCodigo())) {
            throw new RuntimeException("Ya existe otro contrato con el código: " + contratoDTO.getCodigo());
        }
        
        // Validar fechas
        if (contratoDTO.getFechaFin().isBefore(contratoDTO.getFechaInicio())) {
            throw new RuntimeException("La fecha fin no puede ser anterior a la fecha inicio");
        }
        
        contrato.setCodigo(contratoDTO.getCodigo());
        contrato.setObjetivo(contratoDTO.getObjetivo());
        contrato.setFechaInicio(contratoDTO.getFechaInicio());
        contrato.setFechaFin(contratoDTO.getFechaFin());
        
        if (contratoDTO.getSectorUuid() != null) {
            contrato.setSector(sectorRepository.findByUuid(contratoDTO.getSectorUuid())
                .orElseThrow(() -> new RuntimeException("Sector no encontrado")));
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

    // ==================== ASIGNACIÓN DE OPERARIOS A PREDIOS ====================

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
        
        contratoPredio.setOperario(operario);
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

    public void removerOperarioDePredio(UUID contratoUuid, UUID predioUuid) {
        log.info("Removiendo operario del predio {} del contrato {}", predioUuid, contratoUuid);
        
        ContratoPredio contratoPredio = contratoPredioRepository
            .findByContratoUuidAndPredioUuid(contratoUuid, predioUuid)
            .orElseThrow(() -> new RuntimeException("Predio no encontrado en el contrato"));
        
        contratoPredio.setOperario(null);
        contratoPredio.setEstado(EstadoPredio.PENDIENTE);
        
        contratoPredioRepository.save(contratoPredio);
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
        
        // Verificar que no tenga actividades registradas
        if (actividadRepository.existsByContratoPredio(contratoPredio)) {
            throw new RuntimeException("No se puede remover el predio porque tiene actividades registradas");
        }
        
        contratoPredioRepository.delete(contratoPredio);
    }

    // ==================== CONSULTAS Y ESTADÍSTICAS ====================

    @Transactional(readOnly = true)
    public List<Usuario> obtenerOperariosDelContrato(Contrato contrato) {
        return contratoPredioRepository.findOperariosDelContrato(contrato);
    }

    @Transactional(readOnly = true)
    public List<Usuario> obtenerOperariosDisponibles(Contrato contrato) {
        List<Usuario> todosOperarios = usuarioRepository.findOperariosActivos();
        List<Usuario> operariosAsignados = obtenerOperariosDelContrato(contrato);
        
        // Filtrar operarios que no tienen todos los predios asignados
        return todosOperarios.stream()
            .filter(op -> {
                long prediosAsignados = contratoPredioRepository
                    .countByContratoAndOperario(contrato, op);
                return prediosAsignados < contrato.getPredios().size();
            })
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas(Contrato contrato) {
        Map<String, Object> stats = new HashMap<>();
        
        long totalPredios = contratoPredioRepository.countByContrato(contrato);
        long prediosAsignados = contratoPredioRepository.countByContratoAndOperarioIsNotNull(contrato);
        long prediosPendientes = contratoPredioRepository.countByContratoAndEstado(contrato, EstadoPredio.PENDIENTE);
        long prediosCompletados = contratoPredioRepository.countByContratoAndEstado(contrato, EstadoPredio.COMPLETADO);
        
        stats.put("totalPredios", totalPredios);
        stats.put("prediosAsignados", prediosAsignados);
        stats.put("prediosPendientes", prediosPendientes);
        stats.put("prediosCompletados", prediosCompletados);
        stats.put("porcentajeAsignacion", totalPredios > 0 ? (prediosAsignados * 100.0 / totalPredios) : 0);
        stats.put("porcentajeCompletado", totalPredios > 0 ? (prediosCompletados * 100.0 / totalPredios) : 0);
        
        return stats;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerOperariosConPredios(Contrato contrato) {
        List<Usuario> operarios = obtenerOperariosDelContrato(contrato);
        
        return operarios.stream().map(operario -> {
            Map<String, Object> info = new HashMap<>();
            info.put("uuid", operario.getUuid());
            info.put("nombre", operario.getNombre() + " " + operario.getApellido());
            info.put("username", operario.getUsername());
            
            List<ContratoPredio> prediosAsignados = contratoPredioRepository
                .findByContratoAndOperario(contrato, operario);
            
            info.put("prediosAsignados", prediosAsignados.stream().map(cp -> Map.of(
                "uuid", cp.getPredio().getUuid(),
                "direccion", cp.getPredio().getDireccion(),
                "tipo", cp.getPredio().getTipo(),
                "estado", cp.getEstado()
            )).collect(Collectors.toList()));
            
            info.put("totalPrediosAsignados", prediosAsignados.size());
            
            return info;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long contarPrediosAsignadosAOperario(Contrato contrato, Usuario operario) {
        return contratoPredioRepository.countByContratoAndOperario(contrato, operario);
    }

    @Transactional(readOnly = true)
    public long contarPrediosAsignados(Contrato contrato) {
        return contratoPredioRepository.countByContratoAndOperarioIsNotNull(contrato);
    }

    @Transactional(readOnly = true)
    public long contarPrediosSinAsignar(Contrato contrato) {
        return contratoPredioRepository.countByContratoAndOperarioIsNull(contrato);
    }
}