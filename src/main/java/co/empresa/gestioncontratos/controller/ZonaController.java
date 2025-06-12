package co.empresa.gestioncontratos.controller;

import co.empresa.gestioncontratos.dto.*;
import co.empresa.gestioncontratos.entity.*;
import co.empresa.gestioncontratos.enums.PerfilUsuario;
import co.empresa.gestioncontratos.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/admin/zonas")
@RequiredArgsConstructor
@Slf4j
public class ZonaController {

    private final ZonaService zonaService;
    private final ContratoService contratoService;
    private final SectorService sectorService;
    private final PlanTarifaService planTarifaService;
    private final UsuarioService usuarioService;

    // ==================== VISTAS WEB ====================

    @GetMapping
    public String listar(@AuthenticationPrincipal Usuario usuarioActual,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String filtro,
                        @RequestParam(required = false) Boolean activo,
                        @RequestParam(required = false) Long contratoId,
                        Model model) {
        
        log.info("=== ACCEDIENDO A LISTA DE ZONAS ===");
        log.info("Usuario: {}, Filtros - texto: {}, activo: {}, contratoId: {}", 
            usuarioActual.getUsername(), filtro, activo, contratoId);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
            Page<Zona> zonas = zonaService.buscarConFiltros(filtro, activo, contratoId, pageable);
            
            model.addAttribute("zonas", zonas);
            model.addAttribute("usuarioActual", usuarioActual);
            model.addAttribute("filtroActual", filtro);
            model.addAttribute("activoActual", activo);
            model.addAttribute("contratoIdActual", contratoId);
            
            // Cargar contratos para el filtro si el usuario puede verlos
            if (puedeVerTodosLosContratos(usuarioActual)) {
                model.addAttribute("contratos", contratoService.listarTodos());
            } else {
                model.addAttribute("contratos", contratoService.listarAccesiblesPorUsuario(usuarioActual.getUuid()));
            }
            
            // Estadísticas
            Map<String, Long> stats = zonaService.obtenerResumenGeneral();
            model.addAttribute("stats", stats);
            
            // Dashboard si es administrador
            if (usuarioActual.getPerfil() == PerfilUsuario.ADMINISTRADOR) {
                Map<String, Object> dashboard = zonaService.obtenerDashboard();
                model.addAttribute("dashboard", dashboard);
            }
            
            return "admin/zonas";
            
        } catch (Exception e) {
            log.error("Error al listar zonas: ", e);
            model.addAttribute("error", "Error al cargar la lista de zonas: " + e.getMessage());
            return "admin/zonas";
        }
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(@AuthenticationPrincipal Usuario usuarioActual,
                                        Model model) {
        log.info("=== MOSTRANDO FORMULARIO NUEVA ZONA ===");
        
        if (!puedeGestionarZonas(usuarioActual)) {
            return "redirect:/admin/zonas?error=Sin permisos para crear zonas";
        }
        
        ZonaDTO zonaDTO = new ZonaDTO();
        
        model.addAttribute("zona", zonaDTO);
        model.addAttribute("esNuevo", true);
        model.addAttribute("usuarioActual", usuarioActual);
        
        return "admin/zona-form";
    }

    @PostMapping("/nuevo")
    public String crear(@Valid @ModelAttribute ZonaDTO zonaDTO,
                       BindingResult result,
                       @AuthenticationPrincipal Usuario usuarioActual,
                       RedirectAttributes redirectAttributes,
                       Model model) {
        
        log.info("=== CREANDO NUEVA ZONA ===");
        log.info("Usuario: {}, Datos: {}", usuarioActual.getUsername(), zonaDTO);
        
        // Verificar permisos
        if (!puedeGestionarZonas(usuarioActual)) {
            redirectAttributes.addFlashAttribute("error", "No tiene permisos para crear zonas");
            return "redirect:/admin/zonas";
        }
        
        try {
            if (result.hasErrors()) {
                log.warn("Errores de validación encontrados: {}", result.getAllErrors());
                model.addAttribute("esNuevo", true);
                model.addAttribute("usuarioActual", usuarioActual);
                return "admin/zona-form";
            }
            
            ZonaDTO zona = zonaService.crear(zonaDTO);
            log.info("✅ Zona creada exitosamente: {}", zona.getNombre());
            
            redirectAttributes.addFlashAttribute("success", 
                "Zona '" + zona.getNombre() + "' creada exitosamente");
            
            return "redirect:/admin/zonas";
            
        } catch (Exception e) {
            log.error("❌ Error al crear zona: ", e);
            model.addAttribute("error", "Error al crear zona: " + e.getMessage());
            model.addAttribute("esNuevo", true);
            model.addAttribute("usuarioActual", usuarioActual);
            return "admin/zona-form";
        }
    }

    @GetMapping("/{uuid}")
    public String ver(@PathVariable UUID uuid, 
                     @AuthenticationPrincipal Usuario usuarioActual,
                     Model model) {
        log.info("=== VIENDO ZONA: {} ===", uuid);
        
        try {
            ZonaDTO zona = zonaService.buscarPorUuid(uuid);
            Map<String, Object> estadisticas = zonaService.obtenerEstadisticas(zona.getId());
            List<SectorDTO> sectores = sectorService.listarPorZona(zona.getId());
            List<ContratoZonaDTO> contratos = zonaService.listarContratosPorZona(uuid);
            
            // Filtrar contratos según permisos del usuario
            if (!puedeVerTodosLosContratos(usuarioActual)) {
                contratos = contratos.stream()
                    .filter(contrato -> puedeVerContrato(usuarioActual, contrato.getContratoUuid()))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            model.addAttribute("zona", zona);
            model.addAttribute("estadisticas", estadisticas);
            model.addAttribute("sectores", sectores);
            model.addAttribute("contratos", contratos);
            model.addAttribute("puedeEditar", puedeGestionarZonas(usuarioActual));
            model.addAttribute("puedeAsignar", puedeAsignarZonas(usuarioActual));
            model.addAttribute("usuarioActual", usuarioActual);
            
            return "admin/zona-detalle";
            
        } catch (Exception e) {
            log.error("Error al buscar zona: ", e);
            return "redirect:/admin/zonas?error=Zona no encontrada";
        }
    }

    @GetMapping("/{uuid}/editar")
    public String mostrarFormularioEditar(@PathVariable UUID uuid, 
                                         @AuthenticationPrincipal Usuario usuarioActual,
                                         Model model) {
        log.info("=== EDITANDO ZONA: {} ===", uuid);
        
        if (!puedeGestionarZonas(usuarioActual)) {
            return "redirect:/admin/zonas?error=Sin permisos para editar zonas";
        }
        
        try {
            ZonaDTO zona = zonaService.buscarPorUuid(uuid);
            
            model.addAttribute("zona", zona);
            model.addAttribute("esNuevo", false);
            model.addAttribute("usuarioActual", usuarioActual);
            
            return "admin/zona-form";
            
        } catch (Exception e) {
            log.error("Error al buscar zona para editar: ", e);
            return "redirect:/admin/zonas?error=Zona no encontrada";
        }
    }

    @PostMapping("/{uuid}/editar")
    public String actualizar(@PathVariable UUID uuid,
                           @Valid @ModelAttribute ZonaDTO zonaDTO,
                           BindingResult result,
                           @AuthenticationPrincipal Usuario usuarioActual,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        
        log.info("=== ACTUALIZANDO ZONA: {} ===", uuid);
        
        if (!puedeGestionarZonas(usuarioActual)) {
            redirectAttributes.addFlashAttribute("error", "No tiene permisos para editar zonas");
            return "redirect:/admin/zonas";
        }
        
        try {
            if (result.hasErrors()) {
                log.warn("Errores de validación: {}", result.getAllErrors());
                model.addAttribute("esNuevo", false);
                model.addAttribute("usuarioActual", usuarioActual);
                return "admin/zona-form";
            }
            
            ZonaDTO zona = zonaService.actualizar(uuid, zonaDTO);
            redirectAttributes.addFlashAttribute("success", 
                "Zona '" + zona.getNombre() + "' actualizada exitosamente");
            
            return "redirect:/admin/zonas/" + uuid;
            
        } catch (Exception e) {
            log.error("Error al actualizar zona: ", e);
            model.addAttribute("error", "Error al actualizar zona: " + e.getMessage());
            model.addAttribute("esNuevo", false);
            model.addAttribute("usuarioActual", usuarioActual);
            return "admin/zona-form";
        }
    }

    @PostMapping("/{uuid}/eliminar")
    public String eliminar(@PathVariable UUID uuid,
                          @AuthenticationPrincipal Usuario usuarioActual,
                          RedirectAttributes redirectAttributes) {
        
        log.info("=== ELIMINANDO ZONA: {} ===", uuid);
        
        if (!puedeGestionarZonas(usuarioActual)) {
            redirectAttributes.addFlashAttribute("error", "No tiene permisos para eliminar zonas");
            return "redirect:/admin/zonas";
        }
        
        try {
            ZonaDTO zona = zonaService.buscarPorUuid(uuid);
            zonaService.eliminar(uuid);
            
            redirectAttributes.addFlashAttribute("success", 
                "Zona '" + zona.getNombre() + "' eliminada exitosamente");
            
        } catch (Exception e) {
            log.error("Error al eliminar zona: ", e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al eliminar zona: " + e.getMessage());
        }
        
        return "redirect:/admin/zonas";
    }

    @PostMapping("/{uuid}/cambiar-estado")
    public String cambiarEstado(@PathVariable UUID uuid,
                               @AuthenticationPrincipal Usuario usuarioActual,
                               RedirectAttributes redirectAttributes) {
        
        log.info("=== CAMBIANDO ESTADO ZONA: {} ===", uuid);
        
        if (!puedeGestionarZonas(usuarioActual)) {
            redirectAttributes.addFlashAttribute("error", "No tiene permisos para cambiar estado de zonas");
            return "redirect:/admin/zonas";
        }
        
        try {
            ZonaDTO zona = zonaService.buscarPorUuid(uuid);
            zonaService.cambiarEstado(uuid);
            
            String nuevoEstado = zona.getActivo() ? "desactivada" : "activada";
            redirectAttributes.addFlashAttribute("success", 
                "Zona '" + zona.getNombre() + "' " + nuevoEstado + " exitosamente");
            
        } catch (Exception e) {
            log.error("Error al cambiar estado: ", e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al cambiar estado: " + e.getMessage());
        }
        
        return "redirect:/admin/zonas";
    }

    // ==================== GESTIÓN DE ASIGNACIONES CONTRATO-ZONA ====================

    @GetMapping("/contrato/{contratoUuid}/asignar")
    public String mostrarFormularioAsignar(@PathVariable UUID contratoUuid,
                                          @AuthenticationPrincipal Usuario usuarioActual,
                                          Model model) {
        log.info("=== ASIGNANDO ZONA A CONTRATO: {} ===", contratoUuid);
        
        if (!puedeAsignarZonas(usuarioActual)) {
            return "redirect:/admin/contratos/" + contratoUuid + "?error=Sin permisos para asignar zonas";
        }
        
        try {
            ContratoDTO contrato = contratoService.buscarDTOPorUuid(contratoUuid);
            
            // Verificar si el usuario puede gestionar este contrato
            if (!puedeGestionarContrato(usuarioActual, contratoUuid)) {
                return "redirect:/admin/contratos?error=Sin permisos para gestionar este contrato";
            }
            
            List<ZonaDTO> zonasDisponibles = zonaService.listarDisponiblesParaContrato(contratoUuid);
            List<PlanTarifaDTO> planesTarifa = planTarifaService.listarActivosDTO();
            List<UsuarioDTO> coordinadores = usuarioService.listarPorPerfil(PerfilUsuario.COORDINADOR);
            
            ContratoZonaDTO contratoZonaDTO = new ContratoZonaDTO();
            contratoZonaDTO.setContratoUuid(contratoUuid);
            
            model.addAttribute("contrato", contrato);
            model.addAttribute("contratoZona", contratoZonaDTO);
            model.addAttribute("zonasDisponibles", zonasDisponibles);
            model.addAttribute("planesTarifa", planesTarifa);
            model.addAttribute("coordinadores", coordinadores);
            model.addAttribute("usuarioActual", usuarioActual);
            
            return "admin/contrato-zona-form";
            
        } catch (Exception e) {
            log.error("Error al mostrar formulario de asignación: ", e);
            return "redirect:/admin/contratos/" + contratoUuid + "?error=Error al cargar formulario";
        }
    }

    @PostMapping("/contrato/{contratoUuid}/asignar")
    public String asignarZona(@PathVariable UUID contratoUuid,
                             @Valid @ModelAttribute ContratoZonaDTO contratoZonaDTO,
                             BindingResult result,
                             @AuthenticationPrincipal Usuario usuarioActual,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        
        log.info("=== ASIGNANDO ZONA AL CONTRATO: {} ===", contratoUuid);
        
        if (!puedeAsignarZonas(usuarioActual)) {
            redirectAttributes.addFlashAttribute("error", "No tiene permisos para asignar zonas");
            return "redirect:/admin/contratos/" + contratoUuid;
        }
        
        try {
            if (result.hasErrors()) {
                log.warn("Errores de validación en asignación: {}", result.getAllErrors());
                
                ContratoDTO contrato = contratoService.buscarDTOPorUuid(contratoUuid);
                List<ZonaDTO> zonasDisponibles = zonaService.listarDisponiblesParaContrato(contratoUuid);
                List<PlanTarifaDTO> planesTarifa = planTarifaService.listarActivosDTO();
                List<UsuarioDTO> coordinadores = usuarioService.listarPorPerfil(PerfilUsuario.COORDINADOR);
                
                model.addAttribute("contrato", contrato);
                model.addAttribute("zonasDisponibles", zonasDisponibles);
                model.addAttribute("planesTarifa", planesTarifa);
                model.addAttribute("coordinadores", coordinadores);
                model.addAttribute("usuarioActual", usuarioActual);
                return "admin/contrato-zona-form";
            }
            
            contratoZonaDTO.setContratoUuid(contratoUuid);
            ContratoZonaDTO asignacion = zonaService.asignarZonaAContrato(contratoZonaDTO);
            
            redirectAttributes.addFlashAttribute("success", 
                "Zona '" + asignacion.getZonaNombre() + "' asignada al contrato exitosamente");
            
            return "redirect:/admin/contratos/" + contratoUuid;
            
        } catch (Exception e) {
            log.error("Error al asignar zona: ", e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al asignar zona: " + e.getMessage());
            return "redirect:/admin/contratos/" + contratoUuid;
        }
    }

    @GetMapping("/contrato-zona/{contratoZonaUuid}/editar")
    public String editarAsignacion(@PathVariable UUID contratoZonaUuid,
                                  @AuthenticationPrincipal Usuario usuarioActual,
                                  Model model) {
        log.info("=== EDITANDO ASIGNACIÓN CONTRATO-ZONA: {} ===", contratoZonaUuid);
        
        if (!puedeGestionarZonas(usuarioActual)) {
            return "redirect:/admin/zonas?error=Sin permisos para editar asignaciones";
        }
        
        try {
            // Obtener datos de la asignación actual
            // Aquí necesitarías un método en el service para obtener la asignación por UUID
            // ContratoZonaDTO contratoZona = zonaService.buscarAsignacionPorUuid(contratoZonaUuid);
            
            List<PlanTarifaDTO> planesTarifa = planTarifaService.listarActivosDTO();
            List<UsuarioDTO> coordinadores = usuarioService.listarPorPerfil(PerfilUsuario.COORDINADOR);
            
            // model.addAttribute("contratoZona", contratoZona);
            model.addAttribute("planesTarifa", planesTarifa);
            model.addAttribute("coordinadores", coordinadores);
            model.addAttribute("usuarioActual", usuarioActual);
            model.addAttribute("esEdicion", true);
            
            return "admin/contrato-zona-edit";
            
        } catch (Exception e) {
            log.error("Error al mostrar formulario de edición: ", e);
            return "redirect:/admin/zonas?error=Error al cargar asignación";
        }
    }

    @PostMapping("/contrato-zona/{contratoZonaUuid}/editar")
    public String actualizarAsignacion(@PathVariable UUID contratoZonaUuid,
                                      @Valid @ModelAttribute ContratoZonaDTO contratoZonaDTO,
                                      BindingResult result,
                                      @AuthenticationPrincipal Usuario usuarioActual,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        
        log.info("=== ACTUALIZANDO ASIGNACIÓN: {} ===", contratoZonaUuid);
        
        if (!puedeGestionarZonas(usuarioActual)) {
            redirectAttributes.addFlashAttribute("error", "No tiene permisos para editar asignaciones");
            return "redirect:/admin/zonas";
        }
        
        try {
            if (result.hasErrors()) {
                List<PlanTarifaDTO> planesTarifa = planTarifaService.listarActivosDTO();
                List<UsuarioDTO> coordinadores = usuarioService.listarPorPerfil(PerfilUsuario.COORDINADOR);
                
                model.addAttribute("planesTarifa", planesTarifa);
                model.addAttribute("coordinadores", coordinadores);
                model.addAttribute("usuarioActual", usuarioActual);
                model.addAttribute("esEdicion", true);
                return "admin/contrato-zona-edit";
            }
            
            ContratoZonaDTO asignacion = zonaService.actualizarAsignacionZona(contratoZonaUuid, contratoZonaDTO);
            
            redirectAttributes.addFlashAttribute("success", 
                "Asignación actualizada exitosamente");
            
            return "redirect:/admin/contratos/" + asignacion.getContratoUuid();
            
        } catch (Exception e) {
            log.error("Error al actualizar asignación: ", e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al actualizar asignación: " + e.getMessage());
            return "redirect:/admin/zonas";
        }
    }

    @PostMapping("/contrato-zona/{contratoZonaUuid}/remover")
    public String removerAsignacion(@PathVariable UUID contratoZonaUuid,
                                   @AuthenticationPrincipal Usuario usuarioActual,
                                   RedirectAttributes redirectAttributes) {
        
        log.info("=== REMOVIENDO ASIGNACIÓN: {} ===", contratoZonaUuid);
        
        if (!puedeGestionarZonas(usuarioActual)) {
            redirectAttributes.addFlashAttribute("error", "No tiene permisos para remover asignaciones");
            return "redirect:/admin/zonas";
        }
        
        try {
            zonaService.removerZonaDeContrato(contratoZonaUuid);
            
            redirectAttributes.addFlashAttribute("success", 
                "Zona removida del contrato exitosamente");
            
        } catch (Exception e) {
            log.error("Error al remover zona: ", e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al remover zona: " + e.getMessage());
        }
        
        return "redirect:/admin/zonas";
    }

    // ==================== API REST ====================

    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<List<ZonaDTO>> listarTodos(@AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: LISTANDO TODAS LAS ZONAS ===");
        
        try {
            List<ZonaDTO> zonasDTO;
            
            if (puedeVerTodasLasZonas(usuarioActual)) {
                zonasDTO = zonaService.listarTodasConEstadisticas();
            } else {
                zonasDTO = zonaService.listarActivas();
            }
            
            return ResponseEntity.ok(zonasDTO);
            
        } catch (Exception e) {
            log.error("Error al listar zonas: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/activas")
    @ResponseBody
    public ResponseEntity<List<ZonaDTO>> listarActivas() {
        log.info("=== API: LISTANDO ZONAS ACTIVAS ===");
        
        try {
            List<ZonaDTO> zonas = zonaService.listarActivas();
            return ResponseEntity.ok(zonas);
        } catch (Exception e) {
            log.error("Error al listar zonas activas: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/contrato/{contratoUuid}")
    @ResponseBody
    public ResponseEntity<List<ZonaDTO>> listarPorContrato(@PathVariable UUID contratoUuid,
                                                          @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: LISTANDO ZONAS DEL CONTRATO {} ===", contratoUuid);
        
        try {
            // Verificar permisos para el contrato
            if (!puedeVerContrato(usuarioActual, contratoUuid)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            List<ZonaDTO> zonas = zonaService.listarPorContrato(contratoUuid);
            return ResponseEntity.ok(zonas);
        } catch (Exception e) {
            log.error("Error al listar zonas del contrato: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/contrato/{contratoUuid}/disponibles")
    @ResponseBody
    public ResponseEntity<List<ZonaDTO>> listarDisponiblesParaContrato(@PathVariable UUID contratoUuid,
                                                                      @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: LISTANDO ZONAS DISPONIBLES PARA CONTRATO {} ===", contratoUuid);
        
        try {
            if (!puedeVerContrato(usuarioActual, contratoUuid)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            List<ZonaDTO> zonas = zonaService.listarDisponiblesParaContrato(contratoUuid);
            return ResponseEntity.ok(zonas);
        } catch (Exception e) {
            log.error("Error al listar zonas disponibles: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/{uuid}")
    @ResponseBody
    public ResponseEntity<ZonaDTO> obtenerZona(@PathVariable UUID uuid) {
        try {
            ZonaDTO zona = zonaService.buscarPorUuid(uuid);
            return ResponseEntity.ok(zona);
        } catch (Exception e) {
            log.error("Error al obtener zona: ", e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/api/crear")
    @ResponseBody
    public ResponseEntity<?> crearZona(@Valid @RequestBody ZonaDTO zonaDTO,
                                      @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: CREANDO ZONA ===");
        
        if (!puedeGestionarZonas(usuarioActual)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "No tiene permisos para crear zonas"
            ));
        }
        
        try {
            ZonaDTO zona = zonaService.crear(zonaDTO);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Zona creada exitosamente",
                "zona", zona
            ));
        } catch (Exception e) {
            log.error("Error al crear zona: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/api/{uuid}")
    @ResponseBody
    public ResponseEntity<?> actualizarZona(@PathVariable UUID uuid,
                                           @Valid @RequestBody ZonaDTO zonaDTO,
                                           @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: ACTUALIZANDO ZONA {} ===", uuid);
        
        if (!puedeGestionarZonas(usuarioActual)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "No tiene permisos para editar zonas"
            ));
        }
        
        try {
            ZonaDTO zona = zonaService.actualizar(uuid, zonaDTO);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Zona actualizada exitosamente",
                "zona", zona
            ));
        } catch (Exception e) {
            log.error("Error al actualizar zona: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PatchMapping("/api/{uuid}/estado")
    @ResponseBody
    public ResponseEntity<?> cambiarEstado(@PathVariable UUID uuid,
                                          @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: CAMBIANDO ESTADO ZONA: {} ===", uuid);
        
        if (!puedeGestionarZonas(usuarioActual)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "No tiene permisos para cambiar el estado de zonas"
            ));
        }
        
        try {
            zonaService.cambiarEstado(uuid);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Estado de la zona actualizado"
            ));
        } catch (Exception e) {
            log.error("Error al cambiar estado: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/api/{uuid}")
    @ResponseBody
    public ResponseEntity<?> eliminar(@PathVariable UUID uuid,
                                     @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: ELIMINANDO ZONA: {} ===", uuid);
        
        if (!puedeGestionarZonas(usuarioActual)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "No tiene permisos para eliminar zonas"
            ));
        }
        
        try {
            zonaService.eliminar(uuid);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Zona eliminada exitosamente"
            ));
        } catch (Exception e) {
            log.error("Error al eliminar zona: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ==================== API PARA ASIGNACIONES CONTRATO-ZONA ====================

    @PostMapping("/api/contrato/{contratoUuid}/asignar")
    @ResponseBody
    public ResponseEntity<?> asignarZonaAContrato(@PathVariable UUID contratoUuid,
                                                 @Valid @RequestBody ContratoZonaDTO contratoZonaDTO,
                                                 @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: ASIGNANDO ZONA A CONTRATO {} ===", contratoUuid);
        
        if (!puedeAsignarZonas(usuarioActual)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "No tiene permisos para asignar zonas"
            ));
        }
        
        if (!puedeGestionarContrato(usuarioActual, contratoUuid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "No tiene permisos para gestionar este contrato"
            ));
        }
        
        try {
            contratoZonaDTO.setContratoUuid(contratoUuid);
            ContratoZonaDTO asignacion = zonaService.asignarZonaAContrato(contratoZonaDTO);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Zona asignada al contrato exitosamente",
                "asignacion", asignacion
            ));
        } catch (Exception e) {
            log.error("Error al asignar zona: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/api/contrato-zona/{contratoZonaUuid}")
    @ResponseBody
    public ResponseEntity<?> actualizarAsignacion(@PathVariable UUID contratoZonaUuid,
                                                 @Valid @RequestBody ContratoZonaDTO contratoZonaDTO,
                                                 @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: ACTUALIZANDO ASIGNACIÓN {} ===", contratoZonaUuid);
        
        if (!puedeGestionarZonas(usuarioActual)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "No tiene permisos para editar asignaciones"
            ));
        }
        
        try {
            ContratoZonaDTO asignacion = zonaService.actualizarAsignacionZona(contratoZonaUuid, contratoZonaDTO);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Asignación actualizada exitosamente",
                "asignacion", asignacion
            ));
        } catch (Exception e) {
            log.error("Error al actualizar asignación: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/api/contrato-zona/{contratoZonaUuid}")
    @ResponseBody
    public ResponseEntity<?> removerZonaDeContrato(@PathVariable UUID contratoZonaUuid,
                                                  @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: REMOVIENDO ZONA DE CONTRATO {} ===", contratoZonaUuid);
        
        if (!puedeGestionarZonas(usuarioActual)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "No tiene permisos para remover asignaciones"
            ));
        }
        
        try {
            zonaService.removerZonaDeContrato(contratoZonaUuid);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Zona removida del contrato exitosamente"
            ));
        } catch (Exception e) {
            log.error("Error al remover zona: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ==================== ENDPOINTS ESPECÍFICOS ====================

    @GetMapping("/api/estadisticas")
    @ResponseBody
    public ResponseEntity<?> obtenerEstadisticasGenerales(@AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: OBTENIENDO ESTADÍSTICAS GENERALES ===");
        
        if (!puedeVerEstadisticas(usuarioActual)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            Map<String, Long> estadisticas = zonaService.obtenerResumenGeneral();
            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            log.error("Error al obtener estadísticas: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/dashboard")
    @ResponseBody
    public ResponseEntity<?> obtenerDashboard(@AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: OBTENIENDO DASHBOARD ===");
        
        if (!puedeVerDashboard(usuarioActual)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            Map<String, Object> dashboard = zonaService.obtenerDashboard();
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("Error al obtener dashboard: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/{uuid}/estadisticas")
    @ResponseBody
    public ResponseEntity<?> obtenerEstadisticas(@PathVariable UUID uuid) {
        log.info("=== API: OBTENIENDO ESTADÍSTICAS DE LA ZONA {} ===", uuid);
        
        try {
            ZonaDTO zona = zonaService.buscarPorUuid(uuid);
            Map<String, Object> estadisticas = zonaService.obtenerEstadisticas(zona.getId());
            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            log.error("Error al obtener estadísticas de la zona: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/{zonaUuid}/contrato/{contratoUuid}/estadisticas")
    @ResponseBody
    public ResponseEntity<?> obtenerEstadisticasEnContrato(@PathVariable UUID zonaUuid,
                                                          @PathVariable UUID contratoUuid,
                                                          @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: OBTENIENDO ESTADÍSTICAS DE ZONA {} EN CONTRATO {} ===", zonaUuid, contratoUuid);
        
        if (!puedeVerContrato(usuarioActual, contratoUuid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            Map<String, Object> estadisticas = zonaService.obtenerEstadisticasEnContrato(zonaUuid, contratoUuid);
            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            log.error("Error al obtener estadísticas: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/{uuid}/sectores")
    @ResponseBody
    public ResponseEntity<List<SectorDTO>> obtenerSectoresDeZona(@PathVariable UUID uuid) {
        log.info("=== API: OBTENIENDO SECTORES DE LA ZONA {} ===", uuid);
        
        try {
            ZonaDTO zona = zonaService.buscarPorUuid(uuid);
            List<SectorDTO> sectores = sectorService.listarPorZona(zona.getId());
            return ResponseEntity.ok(sectores);
        } catch (Exception e) {
            log.error("Error al obtener sectores de la zona: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/{uuid}/contratos")
    @ResponseBody
    public ResponseEntity<List<ContratoZonaDTO>> obtenerContratosDeZona(@PathVariable UUID uuid,
                                                                       @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: OBTENIENDO CONTRATOS DE LA ZONA {} ===", uuid);
        
        try {
            List<ContratoZonaDTO> contratos = zonaService.listarContratosPorZona(uuid);
            
            // Filtrar contratos según permisos del usuario
            if (!puedeVerTodosLosContratos(usuarioActual)) {
                contratos = contratos.stream()
                    .filter(contrato -> puedeVerContrato(usuarioActual, contrato.getContratoUuid()))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            return ResponseEntity.ok(contratos);
        } catch (Exception e) {
            log.error("Error al obtener contratos de la zona: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/atencion")
    @ResponseBody
    public ResponseEntity<List<ContratoZonaDTO>> obtenerZonasQueNecesitanAtencion(@AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: OBTENIENDO ZONAS QUE NECESITAN ATENCIÓN ===");
        
        if (!puedeVerAlertas(usuarioActual)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            List<ContratoZonaDTO> zonas = zonaService.obtenerZonasQueNecesitanAtencion();
            
            // Filtrar según permisos del usuario
            if (!puedeVerTodosLosContratos(usuarioActual)) {
                zonas = zonas.stream()
                    .filter(zona -> puedeVerContrato(usuarioActual, zona.getContratoUuid()))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            return ResponseEntity.ok(zonas);
        } catch (Exception e) {
            log.error("Error al obtener zonas que necesitan atención: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/coordinadores-disponibles")
    @ResponseBody
    public ResponseEntity<List<UsuarioDTO>> obtenerCoordinadoresDisponibles(
            @RequestParam UUID contratoUuid,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin,
            @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: OBTENIENDO COORDINADORES DISPONIBLES ===");
        
        if (!puedeVerContrato(usuarioActual, contratoUuid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            LocalDate inicio = LocalDate.parse(fechaInicio);
            LocalDate fin = LocalDate.parse(fechaFin);
            
            List<UsuarioDTO> coordinadores = zonaService.buscarCoordinadoresDisponibles(contratoUuid, inicio, fin);
            return ResponseEntity.ok(coordinadores);
        } catch (Exception e) {
            log.error("Error al obtener coordinadores disponibles: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/coordinador/{coordinadorUuid}/resumen")
    @ResponseBody
    public ResponseEntity<?> obtenerResumenCoordinador(@PathVariable UUID coordinadorUuid,
                                                      @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: OBTENIENDO RESUMEN DE COORDINADOR {} ===", coordinadorUuid);
        
        if (!puedeVerResumenCoordinadores(usuarioActual)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            Map<String, Object> resumen = zonaService.obtenerResumenPorCoordinador(coordinadorUuid);
            return ResponseEntity.ok(resumen);
        } catch (Exception e) {
            log.error("Error al obtener resumen de coordinador: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/rendimiento")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerEstadisticasRendimiento(@AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: OBTENIENDO ESTADÍSTICAS DE RENDIMIENTO ===");
        
        if (!puedeVerEstadisticas(usuarioActual)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            List<Map<String, Object>> estadisticas = zonaService.obtenerEstadisticasRendimiento();
            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            log.error("Error al obtener estadísticas de rendimiento: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/buscar-asignaciones")
    @ResponseBody
    public ResponseEntity<Page<ContratoZonaDTO>> buscarAsignaciones(
            @RequestParam(required = false) UUID contratoUuid,
            @RequestParam(required = false) UUID zonaUuid,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) UUID coordinadorUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Usuario usuarioActual) {
        
        log.info("=== API: BUSCANDO ASIGNACIONES CON FILTROS ===");
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("fechaCreacion").descending());
            Page<ContratoZonaDTO> asignaciones = zonaService.buscarAsignacionesConFiltros(
                contratoUuid, zonaUuid, estado, activo, coordinadorUuid, pageable);
            
            // Filtrar según permisos del usuario si no puede ver todos los contratos
            if (!puedeVerTodosLosContratos(usuarioActual)) {
                List<ContratoZonaDTO> asignacionesFiltradas = asignaciones.getContent().stream()
                    .filter(asignacion -> puedeVerContrato(usuarioActual, asignacion.getContratoUuid()))
                    .collect(java.util.stream.Collectors.toList());
                
                asignaciones = new org.springframework.data.domain.PageImpl<>(
                    asignacionesFiltradas, pageable, asignacionesFiltradas.size());
            }
            
            return ResponseEntity.ok(asignaciones);
        } catch (Exception e) {
            log.error("Error al buscar asignaciones: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/validar-codigo")
    @ResponseBody
    public ResponseEntity<?> validarCodigo(@RequestParam String codigo,
                                          @RequestParam(required = false) UUID uuid) {
        log.debug("Validando código de zona: {}", codigo);
        
        try {
            boolean existe = zonaService.existeCodigo(codigo);
            
            // Si estamos editando, verificar que no sea la misma zona
            if (existe && uuid != null) {
                ZonaDTO zona = zonaService.buscarPorCodigo(codigo);
                existe = !zona.getUuid().equals(uuid);
            }
            
            return ResponseEntity.ok(Map.of(
                "existe", existe,
                "disponible", !existe
            ));
        } catch (Exception e) {
            log.error("Error al validar código: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Error al validar código"
            ));
        }
    }

    @GetMapping("/api/validar-asignacion")
    @ResponseBody
    public ResponseEntity<?> validarAsignacion(@RequestParam UUID contratoUuid,
                                              @RequestParam UUID zonaUuid) {
        log.debug("Validando asignación zona {} a contrato {}", zonaUuid, contratoUuid);
        
        try {
            boolean yaAsignada = zonaService.perteneceAContrato(zonaUuid, contratoUuid);
            
            return ResponseEntity.ok(Map.of(
                "yaAsignada", yaAsignada,
                "puedeAsignar", !yaAsignada
            ));
        } catch (Exception e) {
            log.error("Error al validar asignación: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Error al validar asignación"
            ));
        }
    }

    @GetMapping("/api/validar-coordinador")
    @ResponseBody
    public ResponseEntity<?> validarCoordinador(@RequestParam UUID coordinadorUuid,
                                               @RequestParam String fechaInicio,
                                               @RequestParam String fechaFin,
                                               @RequestParam(required = false) UUID contratoExcluir) {
        log.debug("Validando disponibilidad de coordinador: {}", coordinadorUuid);
        
        try {
            LocalDate inicio = LocalDate.parse(fechaInicio);
            LocalDate fin = LocalDate.parse(fechaFin);
            
            boolean disponible = zonaService.coordinadorDisponible(coordinadorUuid, inicio, fin, contratoExcluir);
            
            return ResponseEntity.ok(Map.of(
                "disponible", disponible,
                "puedeAsignar", disponible
            ));
        } catch (Exception e) {
            log.error("Error al validar coordinador: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Error al validar coordinador"
            ));
        }
    }

    @GetMapping("/api/select")
    @ResponseBody
    public ResponseEntity<List<ZonaDTO>> listarZonasSimple() {
        try {
            List<ZonaDTO> zonas = zonaService.listarActivas();
            return ResponseEntity.ok(zonas);
        } catch (Exception e) {
            log.error("Error al listar zonas: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/api/exportar")
    @ResponseBody
    public ResponseEntity<?> exportarDatos(@RequestParam UUID zonaUuid,
                                          @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: EXPORTANDO DATOS DE ZONA {} ===", zonaUuid);
        
        if (!puedeExportarDatos(usuarioActual)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "No tiene permisos para exportar datos"
            ));
        }
        
        try {
            List<Map<String, Object>> datos = zonaService.exportarDatosZona(zonaUuid);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "datos", datos,
                "total", datos.size()
            ));
        } catch (Exception e) {
            log.error("Error al exportar datos: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al exportar datos: " + e.getMessage()
            ));
        }
    }

    // ==================== MÉTODOS DE PERMISOS ====================
    
    private boolean puedeGestionarZonas(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR ||
               usuario.getPerfil() == PerfilUsuario.SUPERVISOR;
    }
    
    private boolean puedeVerZonas(Usuario usuario) {
        // Todos los perfiles pueden ver zonas
        return true;
    }
    
    private boolean puedeAsignarZonas(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR ||
               usuario.getPerfil() == PerfilUsuario.SUPERVISOR;
    }
    
    private boolean puedeVerTodosLosContratos(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR ||
               usuario.getPerfil() == PerfilUsuario.SUPERVISOR;
    }
    
    private boolean puedeVerTodasLasZonas(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR ||
               usuario.getPerfil() == PerfilUsuario.SUPERVISOR;
    }
    
    private boolean puedeVerContrato(Usuario usuario, UUID contratoUuid) {
        if (puedeVerTodosLosContratos(usuario)) {
            return true;
        }
        
        // Para coordinadores, verificar si están asignados al contrato
        if (usuario.getPerfil() == PerfilUsuario.COORDINADOR) {
            try {
                return contratoService.esCoordinadorDeContrato(contratoUuid, usuario.getUuid());
            } catch (Exception e) {
                log.warn("Error al verificar acceso a contrato: ", e);
                return false;
            }
        }
        
        return false;
    }
    
    private boolean puedeGestionarContrato(Usuario usuario, UUID contratoUuid) {
        if (usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR) {
            return true;
        }
        
        if (usuario.getPerfil() == PerfilUsuario.SUPERVISOR) {
            try {
                return contratoService.puedeGestionarContrato(contratoUuid, usuario.getUuid());
            } catch (Exception e) {
                log.warn("Error al verificar gestión de contrato: ", e);
                return false;
            }
        }
        
        return false;
    }
    
    private boolean puedeVerEstadisticas(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR ||
               usuario.getPerfil() == PerfilUsuario.SUPERVISOR;
    }
    
    private boolean puedeVerDashboard(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR ||
               usuario.getPerfil() == PerfilUsuario.SUPERVISOR;
    }
    
    private boolean puedeVerAlertas(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR ||
               usuario.getPerfil() == PerfilUsuario.SUPERVISOR ||
               usuario.getPerfil() == PerfilUsuario.COORDINADOR;
    }
    
    private boolean puedeVerResumenCoordinadores(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR ||
               usuario.getPerfil() == PerfilUsuario.SUPERVISOR;
    }
    
    private boolean puedeExportarDatos(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR ||
               usuario.getPerfil() == PerfilUsuario.SUPERVISOR;
    }
}