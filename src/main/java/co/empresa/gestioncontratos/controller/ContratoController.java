package co.empresa.gestioncontratos.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import co.empresa.gestioncontratos.dto.*;
import co.empresa.gestioncontratos.service.ZonaService;
import co.empresa.gestioncontratos.service.PlanTarifaService;
import jakarta.validation.Valid;

import co.empresa.gestioncontratos.entity.*;
import co.empresa.gestioncontratos.enums.EstadoContrato;
import co.empresa.gestioncontratos.enums.EstadoContratoZona;
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
    private final PlanTarifaService planTarifaService;
    private final PredioOperarioRepository predioOperarioRepository;

    // ==================== VISTAS WEB ====================

    // Vista principal de contratos
    @GetMapping
    public String listar(@AuthenticationPrincipal Usuario usuarioActual, Model model) {
        log.info("=== LISTANDO CONTRATOS ===");
        log.info("Usuario: {} ({})", usuarioActual.getUsername(), usuarioActual.getPerfil());
        
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
                contratos = contratoService.listarContratoPorOperario(usuarioActual);
                break;
            default:
                contratos = new ArrayList<>();
        }
        
        model.addAttribute("contratos", contratos);
        model.addAttribute("usuarioActual", usuarioActual);
        
        return "admin/contratos";
    }

    // Vista de detalle/asignación de un contrato (ACTUALIZADA)
    @GetMapping("/{uuid}/asignaciones")
    public String verAsignaciones(@PathVariable UUID uuid, 
                                 @AuthenticationPrincipal Usuario usuarioActual,
                                 Model model) {
        log.info("=== VER ASIGNACIONES CONTRATO: {} ===", uuid);
        log.info("Usuario: {} ({})", usuarioActual.getUsername(), usuarioActual.getPerfil());
        
        Contrato contrato = contratoService.buscarPorUuid(uuid);
        
        // Verificar permisos de visualización
        if (!tienePermisoVerContrato(usuarioActual, contrato)) {
            log.warn("Usuario {} sin permisos para ver contrato {}", usuarioActual.getUsername(), uuid);
            return "redirect:/admin/contratos";
        }
        
        // Obtener estadísticas del contrato
        Map<String, Object> estadisticas = contratoService.obtenerEstadisticas(contrato);
        
        // Obtener zonas del contrato
        List<ContratoZona> zonasContrato = contratoService.listarZonasDelContrato(uuid);
        
        model.addAttribute("contrato", contrato);
        model.addAttribute("estadisticas", estadisticas);
        model.addAttribute("zonasContrato", zonasContrato);
        
        // Solo ADMIN puede asignar supervisores
        if (usuarioActual.getPerfil() == PerfilUsuario.ADMINISTRADOR) {
            model.addAttribute("supervisoresDisponibles", usuarioService.findSupervisoresActivos());
        }
        
        // Para gestionar zonas (ADMIN y SUPERVISOR del contrato)
        if (puedeGestionarZonas(usuarioActual, contrato)) {
            model.addAttribute("zonasDisponibles", zonaService.listarActivas());
            model.addAttribute("planesTarifaDisponibles", planTarifaService.listarActivos());
            model.addAttribute("coordinadoresDisponibles", usuarioService.findCoordinadoresActivos());
        }
        
        // Para asignar operarios a predios
        if (puedeAsignarOperario(usuarioActual, contrato)) {
            model.addAttribute("operariosDisponibles", usuarioService.findOperariosActivos());
            model.addAttribute("prediosDelContrato", contrato.getPredios());
        }
        
        model.addAttribute("puedeEditarAsignaciones", tienePermisoParaAsignar(usuarioActual, contrato));
        model.addAttribute("puedeGestionarZonas", puedeGestionarZonas(usuarioActual, contrato));
        
        return "admin/contrato-asignaciones";
    }

    // Vista para crear/editar contrato (ACTUALIZADA)
    @GetMapping("/nuevo")
    public String mostrarFormularioCrear(@AuthenticationPrincipal Usuario usuarioActual, Model model) {
        if (usuarioActual.getPerfil() != PerfilUsuario.ADMINISTRADOR) {
            return "redirect:/admin/contratos";
        }
        
        model.addAttribute("contratoDTO", new ContratoDTO());
        model.addAttribute("supervisoresDisponibles", usuarioService.findSupervisoresActivos());
        model.addAttribute("zonasDisponibles", zonaService.listarActivas());
        model.addAttribute("planesTarifaDisponibles", planTarifaService.listarActivos());
        model.addAttribute("coordinadoresDisponibles", usuarioService.findCoordinadoresActivos());
        model.addAttribute("esNuevo", true);
        
        return "admin/contrato-form";
    }

    @GetMapping("/{uuid}/editar")
    public String mostrarFormularioEditar(@PathVariable UUID uuid,
                                         @AuthenticationPrincipal Usuario usuarioActual,
                                         Model model) {
        Contrato contrato = contratoService.buscarPorUuid(uuid);
        
        if (!tienePermisoEditarContrato(usuarioActual, contrato)) {
            return "redirect:/admin/contratos";
        }
        
        ContratoDTO contratoDTO = contratoService.convertirADTOCompleto(contrato);
        
        model.addAttribute("contratoDTO", contratoDTO);
        model.addAttribute("supervisoresDisponibles", usuarioService.findSupervisoresActivos());
        model.addAttribute("zonasDisponibles", zonaService.listarActivas());
        model.addAttribute("planesTarifaDisponibles", planTarifaService.listarActivos());
        model.addAttribute("coordinadoresDisponibles", usuarioService.findCoordinadoresActivos());
        model.addAttribute("esNuevo", false);
        
        return "admin/contrato-form";
    }

    // ==================== API REST ENDPOINTS ====================

    // Listar contratos via API
    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<List<ContratoDTO>> listarContratosAPI(@AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: LISTANDO CONTRATOS ===");
        log.info("Usuario: {} ({})", usuarioActual.getUsername(), usuarioActual.getPerfil());
        
        try {
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
                    contratos = contratoService.listarPorOperario(usuarioActual);
                    break;
                default:
                    contratos = new ArrayList<>();
            }
            
            List<ContratoDTO> contratosDTO = contratos.stream()
                .map(contratoService::convertirADTOCompleto)
                .collect(Collectors.toList());

            log.info("✅ Retornando {} contratos", contratosDTO.size());
            return ResponseEntity.ok(contratosDTO);
            
        } catch (Exception e) {
            log.error("❌ Error al listar contratos: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Crear contrato via API (ACTUALIZADO)
    @PostMapping("/api/crear")
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
            
            // Validar que tenga al menos una zona
            if (contratoDTO.getZonas() == null || contratoDTO.getZonas().isEmpty()) {
                response.put("success", false);
                response.put("message", "El contrato debe tener al menos una zona");
                return ResponseEntity.badRequest().body(response);
            }
            
            Contrato contrato = contratoService.crear(contratoDTO);
            
            response.put("success", true);
            response.put("message", "Contrato creado exitosamente");
            response.put("contrato", Map.of(
                "uuid", contrato.getUuid(),
                "numeroContrato", contrato.getNumeroContrato(),
                "objetivo", contrato.getObjetivo(),
                "totalZonas", contratoDTO.getZonas().size()
            ));
            
            log.info("✅ Contrato creado: {} con {} zonas", 
                contrato.getNumeroContrato(), contratoDTO.getZonas().size());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("❌ Error al crear contrato: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Actualizar contrato via API (ACTUALIZADO)
    @PutMapping("/api/{uuid}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizar(@PathVariable UUID uuid,
                                                        @Valid @RequestBody ContratoDTO contratoDTO,
                                                        @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== ACTUALIZANDO CONTRATO: {} ===", uuid);
        log.info("Usuario: {} ({})", usuarioActual.getUsername(), usuarioActual.getPerfil());
        log.info("Nuevos datos: {}", contratoDTO);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Contrato contrato = contratoService.buscarPorUuid(uuid);
            
            if (!tienePermisoEditarContrato(usuarioActual, contrato)) {
                response.put("success", false);
                response.put("message", "No tiene permisos para editar este contrato");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
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

    // Obtener contrato por UUID via API
    @GetMapping("/api/{uuid}")
    @ResponseBody
    public ResponseEntity<ContratoDTO> obtenerPorUuid(@PathVariable UUID uuid,
                                                    @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== OBTENIENDO CONTRATO: {} ===", uuid);
        log.info("Usuario: {} ({})", usuarioActual.getUsername(), usuarioActual.getPerfil());
        
        try {
            Contrato contrato = contratoService.buscarPorUuid(uuid);
            
            if (!tienePermisoVerContrato(usuarioActual, contrato)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            ContratoDTO dto = contratoService.convertirADTOCompleto(contrato);
            
            log.info("✅ Contrato encontrado: {} con {} zonas", 
                contrato.getNumeroContrato(), dto.getZonas().size());
            return ResponseEntity.ok(dto);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener contrato: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== GESTIÓN DE SUPERVISOR ====================

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

    // ==================== GESTIÓN DE ZONAS (NUEVO) ====================

    @PostMapping("/{contratoUuid}/zonas")
    @ResponseBody
    public ResponseEntity<?> agregarZona(@PathVariable UUID contratoUuid,
                                        @Valid @RequestBody ContratoZonaDTO zonaDTO,
                                        @AuthenticationPrincipal Usuario usuarioActual) {
        
        Contrato contrato = contratoService.buscarPorUuid(contratoUuid);
        
        if (!puedeGestionarZonas(usuarioActual, contrato)) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "No tiene permisos para gestionar zonas"
            ));
        }
        
        try {
            ContratoZona contratoZona = contratoService.agregarZona(contratoUuid, zonaDTO);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Zona agregada exitosamente",
                "zona", Map.of(
                    "uuid", contratoZona.getUuid(),
                    "zonaNombre", contratoZona.getNombreZona(),
                    "planTarifaNombre", contratoZona.getNombrePlanTarifa()
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{contratoUuid}/zonas/{zonaUuid}")
    @ResponseBody
    public ResponseEntity<?> actualizarZona(@PathVariable UUID contratoUuid,
                                           @PathVariable UUID zonaUuid,
                                           @Valid @RequestBody ContratoZonaDTO zonaDTO,
                                           @AuthenticationPrincipal Usuario usuarioActual) {
        
        Contrato contrato = contratoService.buscarPorUuid(contratoUuid);
        
        if (!puedeGestionarZonas(usuarioActual, contrato)) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "No tiene permisos para gestionar zonas"
            ));
        }
        
        try {
            ContratoZona contratoZona = contratoService.actualizarZona(zonaUuid, zonaDTO);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Zona actualizada exitosamente",
                "zona", Map.of(
                    "uuid", contratoZona.getUuid(),
                    "zonaNombre", contratoZona.getNombreZona(),
                    "planTarifaNombre", contratoZona.getNombrePlanTarifa()
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{contratoUuid}/zonas/{zonaUuid}")
    @ResponseBody
    public ResponseEntity<?> removerZona(@PathVariable UUID contratoUuid,
                                        @PathVariable UUID zonaUuid,
                                        @AuthenticationPrincipal Usuario usuarioActual) {
        
        Contrato contrato = contratoService.buscarPorUuid(contratoUuid);
        
        if (!puedeGestionarZonas(usuarioActual, contrato)) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "No tiene permisos para gestionar zonas"
            ));
        }
        
        try {
            contratoService.removerZona(zonaUuid);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Zona removida exitosamente"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{contratoUuid}/zonas")
    @ResponseBody
    public ResponseEntity<?> listarZonasContrato(@PathVariable UUID contratoUuid,
                                                @AuthenticationPrincipal Usuario usuarioActual) {
        
        Contrato contrato = contratoService.buscarPorUuid(contratoUuid);
        
        if (!tienePermisoVerContrato(usuarioActual, contrato)) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "No tiene permisos para ver este contrato"
            ));
        }
        
        try {
            List<ContratoZona> zonas = contratoService.listarZonasDelContrato(contratoUuid);
            
            List<Map<String, Object>> zonasInfo = zonas.stream().map(zona -> {
                Map<String, Object> info = new HashMap<>();
                info.put("uuid", zona.getUuid());
                info.put("zonaNombre", zona.getNombreZona());
                info.put("zonaUuid", zona.getZona().getUuid());
                info.put("planTarifaNombre", zona.getNombrePlanTarifa());
                info.put("planTarifaUuid", zona.getPlanTarifa().getUuid());
                info.put("coordinadorZonaNombre", zona.getNombreCoordinadorZona());
                info.put("coordinadorZonaUuid", zona.getCoordinadorZona() != null ? zona.getCoordinadorZona().getUuid() : null);
                info.put("coordinadorOperativoNombre", zona.getNombreCoordinadorOperativo());
                info.put("coordinadorOperativoUuid", zona.getCoordinadorOperativo() != null ? zona.getCoordinadorOperativo().getUuid() : null);
                info.put("estado", zona.getEstado());
                info.put("activo", zona.getActivo());
                return info;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "zonas", zonasInfo
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ==================== GESTIÓN DE COORDINADORES POR ZONA (NUEVO) ====================

    @PostMapping("/{contratoUuid}/zonas/{zonaUuid}/coordinador-zona")
    @ResponseBody
    public ResponseEntity<?> asignarCoordinadorZona(@PathVariable UUID contratoUuid,
                                                   @PathVariable UUID zonaUuid,
                                                   @RequestParam UUID coordinadorUuid,
                                                   @AuthenticationPrincipal Usuario usuarioActual) {
        
        Contrato contrato = contratoService.buscarPorUuid(contratoUuid);
        
        if (!puedeAsignarCoordinador(usuarioActual, contrato)) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "No tiene permisos para asignar coordinadores"
            ));
        }
        
        try {
            ContratoZona contratoZona = contratoService.asignarCoordinadorZona(zonaUuid, coordinadorUuid);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Coordinador de zona asignado exitosamente",
                "coordinadorNombre", contratoZona.getNombreCoordinadorZona()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/{contratoUuid}/zonas/{zonaUuid}/coordinador-operativo")
    @ResponseBody
    public ResponseEntity<?> asignarCoordinadorOperativo(@PathVariable UUID contratoUuid,
                                                        @PathVariable UUID zonaUuid,
                                                        @RequestParam UUID coordinadorUuid,
                                                        @AuthenticationPrincipal Usuario usuarioActual) {
        
        Contrato contrato = contratoService.buscarPorUuid(contratoUuid);
        
        if (!puedeAsignarCoordinador(usuarioActual, contrato)) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "No tiene permisos para asignar coordinadores"
            ));
        }
        
        try {
            ContratoZona contratoZona = contratoService.asignarCoordinadorOperativo(zonaUuid, coordinadorUuid);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Coordinador operativo asignado exitosamente",
                "coordinadorNombre", contratoZona.getNombreCoordinadorOperativo()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ==================== GESTIÓN DE PREDIOS Y OPERARIOS (SIN CAMBIOS) ====================

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

    // ==================== CONSULTAS Y ESTADÍSTICAS ====================

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
            Map<String, Object> resumen = contratoService.obtenerResumenAsignaciones(contratoUuid);
            
            return ResponseEntity.ok(resumen);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{contratoUuid}/estadisticas")
    @ResponseBody
    public ResponseEntity<?> obtenerEstadisticas(@PathVariable UUID contratoUuid,
                                                @AuthenticationPrincipal Usuario usuarioActual) {
        
        Contrato contrato = contratoService.buscarPorUuid(contratoUuid);
        
        if (!tienePermisoVerContrato(usuarioActual, contrato)) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "No tiene permisos para ver este contrato"
            ));
        }
        
        try {
            Map<String, Object> estadisticas = contratoService.obtenerEstadisticas(contrato);
            Map<String, Object> estadisticasZonas = contratoService.obtenerEstadisticasZonas(contratoUuid);
            
            // Combinar estadísticas
            Map<String, Object> response = new HashMap<>();
            response.putAll(estadisticas);
            response.putAll(estadisticasZonas);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{uuid}/predios")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerPrediosContrato(@PathVariable UUID uuid,
                                                                            @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== OBTENIENDO PREDIOS DEL CONTRATO: {} ===", uuid);
        
        try {
            Contrato contrato = contratoService.buscarPorUuid(uuid);
            
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
                Optional<PredioOperario> predioOperario = predioOperarioRepository
                    .findByPredioAndContratoAndActivoTrue(cp.getPredio(), cp.getContrato());
                
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

    // ==================== GESTIÓN DE ESTADO ====================

    @PatchMapping("/api/{uuid}/estado")
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
            
            if (!tienePermisoEditarContrato(usuarioActual, contrato)) {
                response.put("success", false);
                response.put("message", "No tiene permisos para cambiar el estado del contrato");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
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

    @DeleteMapping("/api/{uuid}")
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

    // ==================== MÉTODOS DE VALIDACIÓN DE PERMISOS (ACTUALIZADOS) ====================

    private boolean tienePermisoVerContrato(Usuario usuario, Contrato contrato) {
        switch (usuario.getPerfil()) {
            case ADMINISTRADOR:
                return true;
            case SUPERVISOR:
                return contrato.getSupervisor() != null && 
                       contrato.getSupervisor().getId().equals(usuario.getId());
            case COORDINADOR:
                // ACTUALIZADO: Verificar si es coordinador en alguna zona del contrato
                List<ContratoZona> zonasCoordinador = contratoService.buscarZonasPorCoordinador(usuario);
                return zonasCoordinador.stream()
                    .anyMatch(zona -> zona.getContrato().getUuid().equals(contrato.getUuid()));
            case OPERARIO:
                // Los operarios pueden ver contratos donde tienen predios asignados
                return contratoService.listarContratoPorOperario(usuario).stream()
                    .anyMatch(c -> c.getUuid().equals(contrato.getUuid()));
            default:
                return false;
        }
    }

    private boolean tienePermisoEditarContrato(Usuario usuario, Contrato contrato) {
        // Solo ADMIN y SUPERVISOR del contrato pueden editar
        return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR ||
               (usuario.getPerfil() == PerfilUsuario.SUPERVISOR && 
                contrato.getSupervisor() != null && 
                contrato.getSupervisor().getId().equals(usuario.getId()));
    }

    private boolean tienePermisoParaAsignar(Usuario usuario, Contrato contrato) {
        return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR ||
               (usuario.getPerfil() == PerfilUsuario.SUPERVISOR && 
                contrato.getSupervisor() != null && 
                contrato.getSupervisor().getId().equals(usuario.getId())) ||
               (usuario.getPerfil() == PerfilUsuario.COORDINADOR &&
                esCoordinadorDelContrato(usuario, contrato));
    }

    private boolean puedeGestionarZonas(Usuario usuario, Contrato contrato) {
        // Solo ADMIN y SUPERVISOR del contrato pueden gestionar zonas
        return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR ||
               (usuario.getPerfil() == PerfilUsuario.SUPERVISOR && 
                contrato.getSupervisor() != null && 
                contrato.getSupervisor().getId().equals(usuario.getId()));
    }

    private boolean puedeAsignarCoordinador(Usuario usuario, Contrato contrato) {
        // Solo ADMIN y SUPERVISOR del contrato pueden asignar coordinadores
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
            return esCoordinadorDelContrato(usuario, contrato);
        }
        
        return false;
    }

    // NUEVO: Método para verificar si un usuario es coordinador en alguna zona del contrato
    private boolean esCoordinadorDelContrato(Usuario usuario, Contrato contrato) {
        List<ContratoZona> zonasCoordinador = contratoService.buscarZonasPorCoordinador(usuario);
        return zonasCoordinador.stream()
            .anyMatch(zona -> zona.getContrato().getUuid().equals(contrato.getUuid()));
    }

    // ==================== ENDPOINTS ADICIONALES PARA ZONAS ====================

    @GetMapping("/api/zonas-sin-coordinador/{contratoUuid}")
    @ResponseBody
    public ResponseEntity<?> obtenerZonasSinCoordinador(@PathVariable UUID contratoUuid,
                                                       @AuthenticationPrincipal Usuario usuarioActual) {
        
        Contrato contrato = contratoService.buscarPorUuid(contratoUuid);
        
        if (!tienePermisoVerContrato(usuarioActual, contrato)) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "No tiene permisos para ver este contrato"
            ));
        }
        
        try {
            List<ContratoZona> zonasSinCoordinador = contratoService.buscarZonasSinCoordinador(contratoUuid);
            
            List<Map<String, Object>> zonasInfo = zonasSinCoordinador.stream().map(zona -> {
                Map<String, Object> info = new HashMap<>();
                info.put("uuid", zona.getUuid());
                info.put("zonaNombre", zona.getNombreZona());
                info.put("planTarifaNombre", zona.getNombrePlanTarifa());
                info.put("estado", zona.getEstado());
                info.put("tieneCoordinadorZona", zona.tieneCoordinadorZona());
                info.put("tieneCoordinadorOperativo", zona.tieneCoordinadorOperativo());
                return info;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "zonas", zonasInfo
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/api/coordinador/{coordinadorUuid}/disponibilidad")
    @ResponseBody
    public ResponseEntity<?> verificarDisponibilidadCoordinador(@PathVariable UUID coordinadorUuid,
                                                              @RequestParam String fechaInicio,
                                                              @RequestParam String fechaFin,
                                                              @AuthenticationPrincipal Usuario usuarioActual) {
        
        // Solo ADMIN y SUPERVISORES pueden verificar disponibilidad
        if (usuarioActual.getPerfil() != PerfilUsuario.ADMINISTRADOR && 
            usuarioActual.getPerfil() != PerfilUsuario.SUPERVISOR) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "No tiene permisos para esta consulta"
            ));
        }
        
        try {
            java.time.LocalDate fechaInicioLD = java.time.LocalDate.parse(fechaInicio);
            java.time.LocalDate fechaFinLD = java.time.LocalDate.parse(fechaFin);
            
            boolean disponible = contratoService.verificarDisponibilidadCoordinador(
                coordinadorUuid, fechaInicioLD, fechaFinLD);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "disponible", disponible,
                "message", disponible ? "Coordinador disponible" : "Coordinador no disponible en estas fechas"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ==================== ENDPOINTS PARA FORMULARIOS WEB ====================

    @PostMapping("/guardar")
    public String guardarContrato(@Valid @ModelAttribute ContratoDTO contratoDTO,
                                 @AuthenticationPrincipal Usuario usuarioActual,
                                 Model model) {
        
        try {
            if (contratoDTO.getUuid() == null) {
                // Crear nuevo contrato
                if (usuarioActual.getPerfil() != PerfilUsuario.ADMINISTRADOR) {
                    model.addAttribute("error", "No tiene permisos para crear contratos");
                    return "admin/contrato-form";
                }
                contratoService.crear(contratoDTO);
                model.addAttribute("success", "Contrato creado exitosamente");
            } else {
                // Actualizar contrato existente
                Contrato contrato = contratoService.buscarPorUuid(contratoDTO.getUuid());
                if (!tienePermisoEditarContrato(usuarioActual, contrato)) {
                    model.addAttribute("error", "No tiene permisos para editar este contrato");
                    return "admin/contrato-form";
                }
                contratoService.actualizar(contratoDTO.getUuid(), contratoDTO);
                model.addAttribute("success", "Contrato actualizado exitosamente");
            }
            
            return "redirect:/admin/contratos";
            
        } catch (Exception e) {
            log.error("Error al guardar contrato: ", e);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("contratoDTO", contratoDTO);
            
            // Recargar listas para el formulario
            model.addAttribute("supervisoresDisponibles", usuarioService.findSupervisoresActivos());
            model.addAttribute("zonasDisponibles", zonaService.listarActivas());
            model.addAttribute("planesTarifaDisponibles", planTarifaService.listarActivos());
            model.addAttribute("coordinadoresDisponibles", usuarioService.findCoordinadoresActivos());
            
            return "admin/contrato-form";
        }
    }

    // ==================== MÉTODOS DE UTILIDAD ====================

    private Map<String, Object> crearRespuestaError(String mensaje) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", mensaje);
        return response;
    }

    private Map<String, Object> crearRespuestaExito(String mensaje) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", mensaje);
        return response;
    }

    private Map<String, Object> crearRespuestaExito(String mensaje, Object data) {
        Map<String, Object> response = crearRespuestaExito(mensaje);
        response.put("data", data);
        return response;
    }
}