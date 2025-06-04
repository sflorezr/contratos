// ContratoController.java
package co.empresa.gestioncontratos.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import co.empresa.gestioncontratos.dto.AsignacionMasivaDTO;
import co.empresa.gestioncontratos.service.ZonaService;
import jakarta.validation.Valid;
import co.empresa.gestioncontratos.dto.ContratoDTO;
import co.empresa.gestioncontratos.dto.UsuarioResumenDTO;

import co.empresa.gestioncontratos.entity.Contrato;
import co.empresa.gestioncontratos.entity.ContratoPredio;
import co.empresa.gestioncontratos.entity.PredioOperario;
import co.empresa.gestioncontratos.entity.Usuario;
import co.empresa.gestioncontratos.enums.EstadoContrato;
import co.empresa.gestioncontratos.enums.PerfilUsuario;
import co.empresa.gestioncontratos.repository.PredioOperarioRepository;
import co.empresa.gestioncontratos.service.ContratoService;

import co.empresa.gestioncontratos.service.UsuarioService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/contratos")
@RequiredArgsConstructor
@Slf4j
public class ContratoController {

    private final ContratoService contratoService;
    private final ZonaService zonaService;
    private final UsuarioService usuarioService;
    private final PredioOperarioRepository predioOperarioRepository;
    

    // Vista principal de contratos
    @GetMapping
    public String listar(@AuthenticationPrincipal Usuario usuarioActual, Model model) {
        
        List<Contrato> contratos;
        
        switch (usuarioActual.getPerfil()) {
            case ADMINISTRADOR:
                contratos = contratoService.listarTodos();
                break;
            case SUPERVISOR:
                contratos = contratoService.listarPorSupervisor(usuarioActual);
                break;
            case COORDINADOR:
                contratos = contratoService.listarPorCoordinador(usuarioActual);
                break;
            case OPERARIO:
                // Los operarios ven contratos donde tienen predios asignados
                contratos = contratoService.listarContratoPorOperario(usuarioActual);
                break;
            default:
                contratos = new ArrayList<>();
        }
        
        model.addAttribute("contratos", contratos);
        model.addAttribute("usuarioActual", usuarioActual);
        
        return "admin/contratos";
    }

    // Vista de detalle/asignación de un contrato
    @GetMapping("/{uuid}/asignaciones")
    public String verAsignaciones(@PathVariable UUID uuid, 
                                 @AuthenticationPrincipal Usuario usuarioActual,
                                 Model model) {
        
        Contrato contrato = contratoService.buscarPorUuid(uuid);
        
        // Verificar permisos de visualización
        if (!tienePermisoVerContrato(usuarioActual, contrato)) {
            return "redirect:/admin/contratos";
        }
        
        // Obtener estadísticas del contrato
        Map<String, Object> estadisticas = contratoService.obtenerEstadisticas(contrato);
        
        // Obtener todos los operarios asignados al contrato (a través de los predios)
     //   List<Usuario> operariosAsignados = contratoService.listarPorOperario(contrato);
        
        // Cargar listas para asignación según permisos
        model.addAttribute("contrato", contrato);
        model.addAttribute("estadisticas", estadisticas);
     //   model.addAttribute("operariosAsignados", operariosAsignados);
        
        // Solo ADMIN puede asignar supervisores
        if (usuarioActual.getPerfil() == PerfilUsuario.ADMINISTRADOR) {
            model.addAttribute("supervisoresDisponibles", usuarioService.findSupervisoresActivos());
        }
        
        // ADMIN y SUPERVISOR del contrato pueden asignar coordinadores
        if (puedeAsignarCoordinador(usuarioActual, contrato)) {
            model.addAttribute("coordinadoresDisponibles", usuarioService.findCoordinadoresActivos());
        }
        
        // Para asignar operarios a predios
        if (puedeAsignarOperario(usuarioActual, contrato)) {
            model.addAttribute("operariosDisponibles", usuarioService.findOperariosActivos());
            model.addAttribute("zonasDelContrato", zonaService.listarPorContrato(contrato.getId()));
            model.addAttribute("prediosDelContrato", contrato.getPredios());
        }
        
        model.addAttribute("puedeEditarAsignaciones", tienePermisoParaAsignar(usuarioActual, contrato));
        
        return "admin/contrato-asignaciones";
    }

    // Asignar supervisor a contrato (solo ADMIN)
    @PostMapping("/{uuid}/asignar-supervisor")
    @ResponseBody
    public ResponseEntity<?> asignarSupervisor(@PathVariable UUID uuid,
                                              @RequestParam UUID supervisorUuid,
                                              @AuthenticationPrincipal Usuario usuarioActual) {
        
        if (usuarioActual.getPerfil() != PerfilUsuario.ADMINISTRADOR) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "No tiene permisos para esta acción"
            ));
        }
        
        try {
            Contrato contrato = contratoService.asignarSupervisor(uuid, supervisorUuid);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Supervisor asignado exitosamente",
                "supervisorNombre", contrato.getSupervisor().getNombre() + " " + contrato.getSupervisor().getApellido()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // Asignar coordinador a contrato (ADMIN y SUPERVISOR del contrato)
    @PostMapping("/{uuid}/asignar-coordinador")
    @ResponseBody
    public ResponseEntity<?> asignarCoordinador(@PathVariable UUID uuid,
                                               @RequestParam UUID coordinadorUuid,
                                               @AuthenticationPrincipal Usuario usuarioActual) {
        
        Contrato contrato = contratoService.buscarPorUuid(uuid);
        
        if (!puedeAsignarCoordinador(usuarioActual, contrato)) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "No tiene permisos para esta acción"
            ));
        }
        
        try {
            Usuario coordinador = contratoService.agregarCoordinador(uuid, coordinadorUuid);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Coordinador asignado exitosamente",
                "coordinadorNombre", coordinador.getNombre() + " " + coordinador.getApellido()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // Remover coordinador del contrato
    @DeleteMapping("/{uuid}/coordinador/{coordinadorUuid}")
    @ResponseBody
    public ResponseEntity<?> removerCoordinador(@PathVariable UUID uuid,
                                               @PathVariable UUID coordinadorUuid,
                                               @AuthenticationPrincipal Usuario usuarioActual) {
        
        Contrato contrato = contratoService.buscarPorUuid(uuid);
        
        if (!puedeAsignarCoordinador(usuarioActual, contrato)) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "No tiene permisos para esta acción"
            ));
        }
        
        try {
            contratoService.removerCoordinador(uuid, coordinadorUuid);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Coordinador removido exitosamente"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // Asignar operario a predio específico
    @PostMapping("/{contratoUuid}/predios/{predioUuid}/asignar-operario")
    @ResponseBody
    public ResponseEntity<?> asignarOperarioAPredio(@PathVariable UUID contratoUuid,
                                                   @PathVariable UUID predioUuid,
                                                   @RequestParam UUID operarioUuid,
                                                   @AuthenticationPrincipal Usuario usuarioActual) {
        
        Contrato contrato = contratoService.buscarPorUuid(contratoUuid);
        
        if (!puedeAsignarOperario(usuarioActual, contrato)) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "No tiene permisos para esta acción"
            ));
        }
        
        try {
            ContratoPredio contratoPredio = contratoService.asignarOperarioAPredio(contratoUuid, predioUuid, operarioUuid);
            Usuario operario = contratoPredio.getOperario();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Operario asignado al predio exitosamente",
                "operarioNombre", operario.getNombre() + " " + operario.getApellido(),
                "predioDireccion", contratoPredio.getPredio().getDireccion()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // Asignar múltiples operarios a múltiples predios
    @PostMapping("/{contratoUuid}/asignar-operarios-masivo")
    @ResponseBody
    public ResponseEntity<?> asignarOperariosMasivo(@PathVariable UUID contratoUuid,
                                                   @RequestBody AsignacionMasivaDTO asignaciones,
                                                   @AuthenticationPrincipal Usuario usuarioActual) {
        
        Contrato contrato = contratoService.buscarPorUuid(contratoUuid);
        
        if (!puedeAsignarOperario(usuarioActual, contrato)) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "No tiene permisos para esta acción"
            ));
        }
        
        try {
            int asignacionesRealizadas = contratoService.asignarOperariosMasivo(
                contratoUuid, 
                asignaciones.getAsignaciones()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", String.format("%d asignaciones realizadas exitosamente", asignacionesRealizadas),
                "totalAsignaciones", asignacionesRealizadas
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // Obtener operarios disponibles para un contrato
    @GetMapping("/{contratoUuid}/operarios-disponibles")
    @ResponseBody
    public ResponseEntity<?> obtenerOperariosDisponibles(@PathVariable UUID contratoUuid,
                                                        @AuthenticationPrincipal Usuario usuarioActual) {
        
        Contrato contrato = contratoService.buscarPorUuid(contratoUuid);
        
        if (!tienePermisoVerContrato(usuarioActual, contrato)) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "No tiene permisos para ver este contrato"
            ));
        }
        
        try {
            // Obtener operarios que no están asignados a todos los predios del contrato
            List<Usuario> operariosDisponibles = contratoService.obtenerOperariosDisponibles(contrato);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "operarios", operariosDisponibles.stream().map(op -> Map.of(
                    "uuid", op.getUuid(),
                    "nombre", op.getNombre() + " " + op.getApellido(),
                    "username", op.getUsername(),
                    "prediosAsignados", contratoService.contarPrediosAsignadosAOperario(contrato, op)
                )).collect(Collectors.toList())
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // Obtener resumen de asignaciones del contrato
    @GetMapping("/{contratoUuid}/resumen-asignaciones")
    @ResponseBody
    public ResponseEntity<?> obtenerResumenAsignaciones(@PathVariable UUID contratoUuid,
                                                       @AuthenticationPrincipal Usuario usuarioActual) {
        
        Contrato contrato = contratoService.buscarPorUuid(contratoUuid);
        
        if (!tienePermisoVerContrato(usuarioActual, contrato)) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "No tiene permisos para ver este contrato"
            ));
        }
        
        try {
            Map<String, Object> resumen = new HashMap<>();
            
            // Supervisor
            resumen.put("supervisor", contrato.getSupervisor() != null ? Map.of(
                "uuid", contrato.getSupervisor().getUuid(),
                "nombre", contrato.getSupervisor().getNombre() + " " + contrato.getSupervisor().getApellido()
            ) : null);
            
            // Coordinadores
            resumen.put("coordinadores", contrato.getCoordinadores().stream().map(coord -> Map.of(
                "uuid", coord.getUuid(),
                "nombre", coord.getNombre() + " " + coord.getApellido()
            )).collect(Collectors.toList()));
            
            // Operarios con sus predios asignados
            List<Map<String, Object>> operariosInfo = contratoService.obtenerOperariosConPredios(contrato);
            resumen.put("operarios", operariosInfo);
            
            // Estadísticas
            resumen.put("totalPredios", contrato.getPredios().size());
            resumen.put("prediosAsignados", contratoService.contarPrediosAsignados(contrato));
            resumen.put("prediosSinAsignar", contratoService.contarPrediosSinAsignar(contrato));
            resumen.put("totalOperarios", contratoService.obtenerOperariosDelContrato(contrato).size());
            
            return ResponseEntity.ok(resumen);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // Métodos auxiliares de permisos
    private boolean tienePermisoVerContrato(Usuario usuario, Contrato contrato) {
        switch (usuario.getPerfil()) {
            case ADMINISTRADOR:
                return true;
            case SUPERVISOR:
                return contrato.getSupervisor() != null && 
                       contrato.getSupervisor().getId().equals(usuario.getId());
            case COORDINADOR:
                return contrato.getCoordinadores().stream()
                    .anyMatch(coord -> coord.getId().equals(usuario.getId()));
       /*      case OPERARIO:
                // Operario puede ver si tiene al menos un predio asignado
                return contrato.getPredios().stream()
                    .anyMatch(cp -> cp.getOperario() != null &&  cp.getOperario().getId().equals(usuario.getId()));*/
            default:
                return false;
        }
    }

    private boolean tienePermisoParaAsignar(Usuario usuario, Contrato contrato) {
        return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR ||
               (usuario.getPerfil() == PerfilUsuario.SUPERVISOR && 
                contrato.getSupervisor() != null && 
                contrato.getSupervisor().getId().equals(usuario.getId())) ||
               (usuario.getPerfil() == PerfilUsuario.COORDINADOR &&
                contrato.getCoordinadores().stream()
                    .anyMatch(coord -> coord.getId().equals(usuario.getId())));
    }

    private boolean puedeAsignarCoordinador(Usuario usuario, Contrato contrato) {
        return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR ||
               (usuario.getPerfil() == PerfilUsuario.SUPERVISOR && 
                contrato.getSupervisor() != null && 
                contrato.getSupervisor().getId().equals(usuario.getId()));
    }

    private boolean puedeAsignarOperario(Usuario usuario, Contrato contrato) {
        if (usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR) return true;
        
        if (usuario.getPerfil() == PerfilUsuario.SUPERVISOR && 
            contrato.getSupervisor() != null && 
            contrato.getSupervisor().getId().equals(usuario.getId())) {
            return true;
        }
        
        if (usuario.getPerfil() == PerfilUsuario.COORDINADOR) {
            return contrato.getCoordinadores().stream()
                .anyMatch(coord -> coord.getId().equals(usuario.getId()));
        }
        
        return false;
    }
    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<List<ContratoDTO>> listarContratosAPI(@AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: LISTANDO CONTRATOS ===");
        log.info("Usuario: {} ({})", usuarioActual.getUsername(), usuarioActual.getPerfil());
        
        try {
            List<Contrato> contratos;
            
            // Filtrar contratos según el perfil del usuario
            switch (usuarioActual.getPerfil()) {
                case ADMINISTRADOR:
                    contratos = contratoService.listarTodos();
                    break;
                case SUPERVISOR:
                    contratos = contratoService.listarPorSupervisor(usuarioActual);
                    break;
                case COORDINADOR:
                    contratos = contratoService.listarPorCoordinador(usuarioActual);
                    break;
                case OPERARIO:
                    contratos = contratoService.listarPorOperario(usuarioActual);
                    break;
                default:
                    contratos = new ArrayList<>();
            }
            

           List<ContratoDTO> contratosDTO = contratos.stream()
                .map(contrato -> {
                    ContratoDTO dto = ContratoDTO.builder()
                        .uuid(contrato.getUuid())
                        .codigo(contrato.getNumeroContrato())
                        .objetivo(contrato.getObjetivo())
                        .fechaInicio(contrato.getFechaInicio())
                        .fechaFin(contrato.getFechaFin())
                        .planTarifaUuid(contrato.getPlanTarifa() != null ? contrato.getPlanTarifa().getUuid() : null)
                        .planTarifaNombre(contrato.getNombrePlanTarifa())
                        .supervisorId(contrato.getSupervisor() != null ? contrato.getSupervisor().getUuid() : null)
                        .supervisorNombre(contrato.getNombreSupervisor())
                        .estado(contrato.getEstado())
                        .build();
                    
                    // Agregar coordinadores
                    if (contrato.getCoordinadores() != null && !contrato.getCoordinadores().isEmpty()) {
                        // Agregar UUIDs de coordinadores
                        dto.setCoordinadorUuids(
                            contrato.getCoordinadores().stream()
                                .map(Usuario::getUuid)
                                .collect(Collectors.toList())
                        );
                        
                        // Agregar información resumida de coordinadores
                        dto.setCoordinadores(
                            contrato.getCoordinadores().stream()
                                .map(coord -> UsuarioResumenDTO.builder()
                                    .uuid(coord.getUuid())
                                    .nombre(coord.getNombre())
                                    .apellido(coord.getApellido())
                                    .username(coord.getUsername())
                                    .build())
                                .collect(Collectors.toList())
                        );
                        
                        dto.setCantidadCoordinadores(contrato.getCoordinadores().size());
                    } else {
                        dto.setCantidadCoordinadores(0);
                    }
                    
                    // Agregar estadísticas del contrato
                    Map<String, Object> estadisticas = contratoService.obtenerEstadisticas(contrato);
                    dto.setTotalPredios(((Number) estadisticas.get("totalPredios")).intValue());
                    dto.setPrediosAsignados(((Number) estadisticas.get("prediosAsignados")).intValue());
                    dto.setTotalOperarios(contratoService.obtenerOperariosDelContrato(contrato).size());
                    dto.setPorcentajeAvance(((Number) estadisticas.get("porcentajeCompletado")).doubleValue());
                    
                    // Agregar información de zonas
                    dto.setTotalZonas(1);                    
                    dto.setTotalSectores(((Number) estadisticas.get("totalSectores")).intValue());
                    dto.setSectoresActivos(((Number) estadisticas.get("sectoresActivos")).intValue());
                    
                    // Opcionalmente, cargar las zonas resumidas                   
                    // Verificar si puede ser eliminado
                    
                    return dto;
                })
                .collect(Collectors.toList());

            log.info("✅ Retornando {} contratos", contratosDTO.size());
            return ResponseEntity.ok(contratosDTO);
            
        } catch (Exception e) {
            log.error("❌ Error al listar contratos: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @PostMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crear(@Valid @RequestBody ContratoDTO contratoDTO,
                                                    @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== CREANDO NUEVO CONTRATO ===");
        log.info("Usuario: {} ({})", usuarioActual.getUsername(), usuarioActual.getPerfil());
        log.info("Datos del contrato: {}", contratoDTO);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Solo ADMINISTRADOR puede crear contratos
            if (usuarioActual.getPerfil() != PerfilUsuario.ADMINISTRADOR) {
                response.put("success", false);
                response.put("message", "No tiene permisos para crear contratos");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            Contrato contrato = contratoService.crear(contratoDTO);
            
            response.put("success", true);
            response.put("message", "Contrato creado exitosamente");
            response.put("contrato", Map.of(
                "uuid", contrato.getUuid(),
                "numeroContrato", contrato.getNumeroContrato(),
                "objetivo", contrato.getObjetivo()
            ));
            
            log.info("✅ Contrato creado: {}", contrato.getNumeroContrato());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("❌ Error al crear contrato: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{uuid}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizar(@PathVariable UUID uuid,
                                                        @Valid @RequestBody ContratoDTO contratoDTO,
                                                        @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== ACTUALIZANDO CONTRATO: {} ===", uuid);
        log.info("Usuario: {} ({})", usuarioActual.getUsername(), usuarioActual.getPerfil());
        log.info("Nuevos datos: {}", contratoDTO);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
                       
            Contrato contratoActualizado = contratoService.actualizar(uuid, contratoDTO);
            
            response.put("success", true);
            response.put("message", "Contrato actualizado exitosamente");
            response.put("contrato", Map.of(
                "uuid", contratoActualizado.getUuid(),
                "numeroContrato", contratoActualizado.getNumeroContrato(),
                "objetivo", contratoActualizado.getObjetivo()
            ));
            
            log.info("✅ Contrato actualizado: {}", contratoActualizado.getNumeroContrato());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error al actualizar contrato: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PatchMapping("/{uuid}/estado")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cambiarEstado(@PathVariable UUID uuid,
                                                            @RequestBody Map<String, String> payload,
                                                            @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== CAMBIANDO ESTADO CONTRATO: {} ===", uuid);
        log.info("Usuario: {} ({})", usuarioActual.getUsername(), usuarioActual.getPerfil());
        log.info("Nuevo estado: {}", payload.get("estado"));
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Contrato contrato = contratoService.buscarPorUuid(uuid);
            
            
            String estadoStr = payload.get("estado");
            EstadoContrato nuevoEstado = EstadoContrato.valueOf(estadoStr);
            
            contratoService.cambiarEstado(uuid, nuevoEstado);
            
            response.put("success", true);
            response.put("message", "Estado del contrato actualizado exitosamente");
            response.put("nuevoEstado", nuevoEstado.toString());
            
            log.info("✅ Estado del contrato {} cambiado a: {}", contrato.getNumeroContrato(), nuevoEstado);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error al cambiar estado del contrato: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{uuid}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminar(@PathVariable UUID uuid,
                                                    @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== ELIMINANDO CONTRATO: {} ===", uuid);
        log.info("Usuario: {} ({})", usuarioActual.getUsername(), usuarioActual.getPerfil());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Contrato contrato = contratoService.buscarPorUuid(uuid);
            
            // Solo ADMINISTRADOR puede eliminar contratos
            if (usuarioActual.getPerfil() != PerfilUsuario.ADMINISTRADOR) {
                response.put("success", false);
                response.put("message", "No tiene permisos para eliminar contratos");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            contratoService.eliminar(uuid);
            
            response.put("success", true);
            response.put("message", "Contrato eliminado exitosamente");
            
            log.info("✅ Contrato eliminado: {}", contrato.getNumeroContrato());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error al eliminar contrato: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{uuid}/predios")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerPrediosContrato(@PathVariable UUID uuid,
                                                                            @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== OBTENIENDO PREDIOS DEL CONTRATO: {} ===", uuid);
        
        try {
            Contrato contrato = contratoService.buscarPorUuid(uuid);
            
            // Verificar permisos
            if (!tienePermisoVerContrato(usuarioActual, contrato)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            List<ContratoPredio> prediosContrato = contratoService.obtenerPrediosDelContrato(uuid);
            
            List<Map<String, Object>> prediosInfo = prediosContrato.stream().map(cp -> {
                Map<String, Object> info = new HashMap<>();
                info.put("uuid", cp.getPredio().getUuid());
                info.put("direccion", cp.getPredio().getDireccion());
                info.put("codigoCatastral", cp.getPredio().getCodigoCatastral());
                info.put("tipo", cp.getPredio().getTipo());
                info.put("estado", cp.getEstado());
                
                // Verificar si tiene operario asignado
                Optional<PredioOperario> predioOperario = predioOperarioRepository.findByPredioAndContratoAndActivoTrue(cp.getPredio(), cp.getContrato());
                
                if (predioOperario.isPresent()) {
                    Usuario operario = predioOperario.get().getOperario();
                    info.put("operarioAsignado", operario.getNombre() + " " + operario.getApellido());
                    info.put("operarioUuid", operario.getUuid());
                } else {
                    info.put("operarioAsignado", null);
                    info.put("operarioUuid", null);
                }
                
                return info;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(prediosInfo);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener predios del contrato: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }    
}  