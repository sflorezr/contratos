// ContratoController.java
package co.empresa.gestioncontratos.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import co.empresa.gestioncontratos.dto.AsignacionMasivaDTO;
import co.empresa.gestioncontratos.entity.Contrato;
import co.empresa.gestioncontratos.entity.ContratoPredio;
import co.empresa.gestioncontratos.entity.Usuario;
import co.empresa.gestioncontratos.enums.PerfilUsuario;
import co.empresa.gestioncontratos.service.ContratoService;
import co.empresa.gestioncontratos.service.PredioService;
import co.empresa.gestioncontratos.service.UsuarioService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/contratos")
@RequiredArgsConstructor
@Slf4j
public class ContratoController {

    private final ContratoService contratoService;
    private final UsuarioService usuarioService;
    private final PredioService predioService;

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
}


