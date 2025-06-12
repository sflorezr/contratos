package co.empresa.gestioncontratos.controller;

import co.empresa.gestioncontratos.dto.PlanTarifaDTO;
import co.empresa.gestioncontratos.entity.PlanTarifa;
import co.empresa.gestioncontratos.entity.Usuario;
import co.empresa.gestioncontratos.enums.PerfilUsuario;
import co.empresa.gestioncontratos.service.PlanTarifaService;
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
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/planes-tarifa")
@RequiredArgsConstructor
@Slf4j
public class PlanTarifaController {

    private final PlanTarifaService planTarifaService;

    // ==================== VISTAS WEB ====================

    @GetMapping
    public String listar(@AuthenticationPrincipal Usuario usuarioActual,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String filtro,
                        @RequestParam(required = false) Boolean activo,
                        Model model) {
        
        log.info("=== ACCEDIENDO A LISTA DE PLANES DE TARIFA ===");
        log.info("Usuario: {} ({}), Filtros - texto: {}, activo: {}", 
            usuarioActual.getUsername(), usuarioActual.getPerfil(), filtro, activo);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("fechaCreacion").descending());
            Page<PlanTarifa> planes = planTarifaService.buscarConFiltros(filtro, activo, pageable);
            
            model.addAttribute("planes", planes);
            model.addAttribute("usuarioActual", usuarioActual);
            model.addAttribute("filtroActual", filtro);
            model.addAttribute("activoActual", activo);
            
            // Estadísticas generales
            Map<String, Object> stats = planTarifaService.obtenerResumenGeneral();
            model.addAttribute("stats", stats);
            
            // Permisos según rol
            model.addAttribute("puedeCrear", puedeGestionarPlanes(usuarioActual));
            model.addAttribute("puedeEditar", puedeGestionarPlanes(usuarioActual));
            model.addAttribute("puedeEliminar", usuarioActual.getPerfil() == PerfilUsuario.ADMINISTRADOR);
            
            return "admin/planes-tarifa";
            
        } catch (Exception e) {
            log.error("Error al listar planes de tarifa: ", e);
            model.addAttribute("error", "Error al cargar la lista de planes de tarifa");
            return "admin/planes-tarifa";
        }
    }

    @GetMapping("/{uuid}/detalle")
    public String verDetalle(@PathVariable UUID uuid,
                           @AuthenticationPrincipal Usuario usuarioActual,
                           Model model) {
        
        log.info("=== VER DETALLE PLAN TARIFA: {} ===", uuid);
        log.info("Usuario: {} ({})", usuarioActual.getUsername(), usuarioActual.getPerfil());
        
        try {
            Map<String, Object> detalle = planTarifaService.obtenerDetallePlan(uuid);
            model.addAttribute("detalle", detalle);
            model.addAttribute("usuarioActual", usuarioActual);
            model.addAttribute("puedeEditar", puedeGestionarPlanes(usuarioActual));
            
            return "admin/plan-tarifa-detalle";
            
        } catch (Exception e) {
            log.error("Error al obtener detalle del plan: ", e);
            model.addAttribute("error", "Plan de tarifa no encontrado");
            return "redirect:/admin/planes-tarifa";
        }
    }

    // ==================== API REST ENDPOINTS ====================

    /**
     * Listar todos los planes con estadísticas completas
     */
    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<List<PlanTarifaDTO>> listarPlanesAPI(@AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: LISTANDO PLANES DE TARIFA ===");
        log.info("Usuario: {} ({})", usuarioActual.getUsername(), usuarioActual.getPerfil());
        
        try {
            List<PlanTarifa> planes = planTarifaService.listarTodos();
            List<PlanTarifaDTO> planesDTO = planTarifaService.convertirADTOsConEstadisticas(planes);
            
            log.info("✅ Retornando {} planes de tarifa", planesDTO.size());
            return ResponseEntity.ok(planesDTO);
            
        } catch (Exception e) {
            log.error("❌ Error al listar planes de tarifa: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Listar planes activos para selección en formularios
     */
    @GetMapping("/api/listar-simple")
    @ResponseBody
    public ResponseEntity<List<PlanTarifaDTO>> listarPlanesSimple() {
        try {
            List<PlanTarifa> planes = planTarifaService.listarActivos();
            List<PlanTarifaDTO> planesDTO = planTarifaService.convertirADTOs(planes);
            
            return ResponseEntity.ok(planesDTO);
        } catch (Exception e) {
            log.error("Error al listar planes activos: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Listar planes para selección (solo UUID y nombre)
     */
    @GetMapping("/api/select")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> listarParaSelect() {
        try {
            List<PlanTarifa> planes = planTarifaService.listarActivos();
            List<Map<String, Object>> planesSelect = planes.stream()
                .map(plan -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("uuid", plan.getUuid());
                    item.put("nombre", plan.getNombre());
                    item.put("descripcion", plan.getDescripcion());
                    return item;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(planesSelect);
        } catch (Exception e) {
            log.error("Error al listar planes para select: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtener plan por UUID
     */
    @GetMapping("/api/{uuid}")
    @ResponseBody
    public ResponseEntity<PlanTarifaDTO> buscarPorUuid(@PathVariable UUID uuid,
                                                      @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: OBTENIENDO PLAN TARIFA: {} ===", uuid);
        log.info("Usuario: {} ({})", usuarioActual.getUsername(), usuarioActual.getPerfil());
        
        try {
            PlanTarifa plan = planTarifaService.buscarPorUuid(uuid);
            PlanTarifaDTO planDTO = planTarifaService.convertirADTOConEstadisticas(plan);
            
            log.info("✅ Plan encontrado: {}", plan.getNombre());
            return ResponseEntity.ok(planDTO);
            
        } catch (RuntimeException e) {
            log.error("❌ Plan no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("❌ Error inesperado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Crear nuevo plan de tarifa
     */
    @PostMapping("/api/crear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crear(@Valid @RequestBody PlanTarifaDTO planTarifaDTO,
                                                     @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: CREANDO PLAN DE TARIFA ===");
        log.info("Usuario: {} ({})", usuarioActual.getUsername(), usuarioActual.getPerfil());
        log.info("Datos del plan: {}", planTarifaDTO.getNombre());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Solo ADMIN y SUPERVISOR pueden crear planes
            if (!puedeGestionarPlanes(usuarioActual)) {
                response.put("success", false);
                response.put("message", "No tiene permisos para crear planes de tarifa");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            PlanTarifa plan = planTarifaService.crear(planTarifaDTO);
            
            response.put("success", true);
            response.put("message", "Plan de tarifa creado exitosamente");
            response.put("plan", Map.of(
                "uuid", plan.getUuid(),
                "nombre", plan.getNombre(),
                "activo", plan.getActivo()
            ));
            
            log.info("✅ Plan creado: {}", plan.getNombre());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("❌ Error al crear plan: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Actualizar plan de tarifa
     */
    @PutMapping("/api/{uuid}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizar(@PathVariable UUID uuid,
                                                         @Valid @RequestBody PlanTarifaDTO planTarifaDTO,
                                                         @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: ACTUALIZANDO PLAN DE TARIFA: {} ===", uuid);
        log.info("Usuario: {} ({})", usuarioActual.getUsername(), usuarioActual.getPerfil());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!puedeGestionarPlanes(usuarioActual)) {
                response.put("success", false);
                response.put("message", "No tiene permisos para editar planes de tarifa");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            PlanTarifa plan = planTarifaService.actualizar(uuid, planTarifaDTO);
            
            response.put("success", true);
            response.put("message", "Plan de tarifa actualizado exitosamente");
            response.put("plan", Map.of(
                "uuid", plan.getUuid(),
                "nombre", plan.getNombre(),
                "activo", plan.getActivo()
            ));
            
            log.info("✅ Plan actualizado: {}", plan.getNombre());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error al actualizar plan: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Cambiar estado del plan (activar/desactivar)
     */
    @PatchMapping("/api/{uuid}/estado")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cambiarEstado(@PathVariable UUID uuid,
                                                            @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: CAMBIANDO ESTADO PLAN: {} ===", uuid);
        log.info("Usuario: {} ({})", usuarioActual.getUsername(), usuarioActual.getPerfil());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!puedeGestionarPlanes(usuarioActual)) {
                response.put("success", false);
                response.put("message", "No tiene permisos para cambiar estado de planes");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            // Validar antes de cambiar estado
            PlanTarifa plan = planTarifaService.buscarPorUuid(uuid);
            if (plan.getActivo()) {
                planTarifaService.validarParaDesactivacion(uuid);
            }
            
            planTarifaService.cambiarEstado(uuid);
            
            // Obtener estado actualizado
            plan = planTarifaService.buscarPorUuid(uuid);
            
            response.put("success", true);
            response.put("message", plan.getActivo() ? "Plan activado exitosamente" : "Plan desactivado exitosamente");
            response.put("nuevoEstado", plan.getActivo());
            
            log.info("✅ Estado cambiado para plan: {} a {}", plan.getNombre(), plan.getActivo());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error al cambiar estado: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Eliminar plan de tarifa
     */
    @DeleteMapping("/api/{uuid}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminar(@PathVariable UUID uuid,
                                                       @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: ELIMINANDO PLAN: {} ===", uuid);
        log.info("Usuario: {} ({})", usuarioActual.getUsername(), usuarioActual.getPerfil());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Solo ADMINISTRADOR puede eliminar
            if (usuarioActual.getPerfil() != PerfilUsuario.ADMINISTRADOR) {
                response.put("success", false);
                response.put("message", "No tiene permisos para eliminar planes de tarifa");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            // Validar antes de eliminar
            planTarifaService.validarParaEliminacion(uuid);
            
            PlanTarifa plan = planTarifaService.buscarPorUuid(uuid);
            String nombrePlan = plan.getNombre();
            
            planTarifaService.eliminar(uuid);
            
            response.put("success", true);
            response.put("message", "Plan de tarifa eliminado exitosamente");
            
            log.info("✅ Plan eliminado: {}", nombrePlan);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error al eliminar plan: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== CONSULTAS ESPECÍFICAS ====================

    @GetMapping("/api/resumen-general")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerResumenGeneral(@AuthenticationPrincipal Usuario usuarioActual) {
        try {
            Map<String, Object> resumen = planTarifaService.obtenerResumenGeneral();
            return ResponseEntity.ok(resumen);
        } catch (Exception e) {
            log.error("Error al obtener resumen general: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/{uuid}/detalle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerDetallePlan(@PathVariable UUID uuid,
                                                                 @AuthenticationPrincipal Usuario usuarioActual) {
        try {
            Map<String, Object> detalle = planTarifaService.obtenerDetallePlan(uuid);
            return ResponseEntity.ok(detalle);
        } catch (RuntimeException e) {
            log.error("Error al obtener detalle del plan: ", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error inesperado: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/con-tarifas")
    @ResponseBody
    public ResponseEntity<List<PlanTarifaDTO>> listarPlanesConTarifas() {
        try {
            List<PlanTarifa> planes = planTarifaService.buscarPlanesConTarifas();
            List<PlanTarifaDTO> planesDTO = planTarifaService.convertirADTOs(planes);
            return ResponseEntity.ok(planesDTO);
        } catch (Exception e) {
            log.error("Error al listar planes con tarifas: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/sin-tarifas")
    @ResponseBody
    public ResponseEntity<List<PlanTarifaDTO>> listarPlanesSinTarifas() {
        try {
            List<PlanTarifa> planes = planTarifaService.buscarPlanesSinTarifas();
            List<PlanTarifaDTO> planesDTO = planTarifaService.convertirADTOs(planes);
            return ResponseEntity.ok(planesDTO);
        } catch (Exception e) {
            log.error("Error al listar planes sin tarifas: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/con-contratos")
    @ResponseBody
    public ResponseEntity<List<PlanTarifaDTO>> listarPlanesConContratos() {
        try {
            List<PlanTarifa> planes = planTarifaService.buscarPlanesConContratosZonas();
            List<PlanTarifaDTO> planesDTO = planTarifaService.convertirADTOs(planes);
            return ResponseEntity.ok(planesDTO);
        } catch (Exception e) {
            log.error("Error al listar planes con contratos: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/mas-utilizados")
    @ResponseBody
    public ResponseEntity<List<Object[]>> obtenerPlanesMasUtilizados() {
        try {
            List<Object[]> planes = planTarifaService.obtenerPlanesMasUtilizados();
            return ResponseEntity.ok(planes);
        } catch (Exception e) {
            log.error("Error al obtener planes más utilizados: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/reporte-uso")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generarReporteUso(@AuthenticationPrincipal Usuario usuarioActual) {
        try {
            Map<String, Object> reporte = planTarifaService.generarReporteUso();
            return ResponseEntity.ok(reporte);
        } catch (Exception e) {
            log.error("Error al generar reporte de uso: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/metricas-comparativas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerMetricasComparativas(@AuthenticationPrincipal Usuario usuarioActual) {
        try {
            Map<String, Object> metricas = planTarifaService.obtenerMetricasComparativas();
            return ResponseEntity.ok(metricas);
        } catch (Exception e) {
            log.error("Error al obtener métricas comparativas: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== OPERACIONES MASIVAS ====================

    @PatchMapping("/api/activar-masivo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> activarMasivo(@RequestBody List<UUID> uuids,
                                                            @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: ACTIVACIÓN MASIVA ===");
        log.info("Usuario: {} ({}), Planes: {}", usuarioActual.getUsername(), usuarioActual.getPerfil(), uuids.size());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!puedeGestionarPlanes(usuarioActual)) {
                response.put("success", false);
                response.put("message", "No tiene permisos para gestionar planes");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            int activados = planTarifaService.activarPlanesMasivo(uuids);
            
            response.put("success", true);
            response.put("message", String.format("%d planes activados exitosamente", activados));
            response.put("totalActivados", activados);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error en activación masiva: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PatchMapping("/api/desactivar-masivo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> desactivarMasivo(@RequestBody List<UUID> uuids,
                                                               @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== API: DESACTIVACIÓN MASIVA ===");
        log.info("Usuario: {} ({}), Planes: {}", usuarioActual.getUsername(), usuarioActual.getPerfil(), uuids.size());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!puedeGestionarPlanes(usuarioActual)) {
                response.put("success", false);
                response.put("message", "No tiene permisos para gestionar planes");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            int desactivados = planTarifaService.desactivarPlanesMasivo(uuids);
            
            response.put("success", true);
            response.put("message", String.format("%d planes desactivados exitosamente", desactivados));
            response.put("totalDesactivados", desactivados);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error en desactivación masiva: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== VALIDACIONES ====================

    @GetMapping("/api/{uuid}/puede-eliminar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verificarPuedeEliminar(@PathVariable UUID uuid,
                                                                     @AuthenticationPrincipal Usuario usuarioActual) {
        try {
            boolean puedeEliminar = planTarifaService.puedeSerEliminado(uuid);
            
            Map<String, Object> response = new HashMap<>();
            response.put("puedeEliminar", puedeEliminar);
            
            if (!puedeEliminar) {
                PlanTarifa plan = planTarifaService.buscarPorUuid(uuid);
                response.put("razon", "El plan tiene " + plan.getCantidadTarifas() + 
                           " tarifas y " + plan.getCantidadContratosZonas() + " zonas de contratos asociadas");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al verificar eliminación: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/{uuid}/puede-desactivar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verificarPuedeDesactivar(@PathVariable UUID uuid,
                                                                       @AuthenticationPrincipal Usuario usuarioActual) {
        try {
            boolean puedeDesactivar = planTarifaService.puedeSerDesactivado(uuid);
            
            Map<String, Object> response = new HashMap<>();
            response.put("puedeDesactivar", puedeDesactivar);
            
            if (!puedeDesactivar) {
                long zonasActivas = planTarifaService.contarZonasActivas(uuid);
                long contratosActivos = planTarifaService.contarContratosActivos(uuid);
                response.put("razon", "El plan tiene " + zonasActivas + 
                           " zonas activas en " + contratosActivos + " contratos activos");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al verificar desactivación: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== BÚSQUEDA Y FILTRADO ====================

    @GetMapping("/api/buscar")
    @ResponseBody
    public ResponseEntity<List<PlanTarifaDTO>> buscarPlanes(@RequestParam String termino,
                                                           @RequestParam(defaultValue = "true") Boolean soloActivos,
                                                           @AuthenticationPrincipal Usuario usuarioActual) {
        try {
            List<PlanTarifa> planes;
            
            if (termino != null && !termino.trim().isEmpty()) {
                planes = planTarifaService.buscarPorNombreParcial(termino);
                if (soloActivos) {
                    planes = planes.stream()
                        .filter(PlanTarifa::getActivo)
                        .collect(Collectors.toList());
                }
            } else {
                planes = soloActivos ? planTarifaService.listarActivos() : planTarifaService.listarTodos();
            }
            
            List<PlanTarifaDTO> planesDTO = planTarifaService.convertirADTOs(planes);
            return ResponseEntity.ok(planesDTO);
            
        } catch (Exception e) {
            log.error("Error al buscar planes: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/filtrar")
    @ResponseBody
    public ResponseEntity<Page<PlanTarifaDTO>> filtrarPlanes(@RequestParam(required = false) String filtro,
                                                            @RequestParam(required = false) Boolean activo,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size,
                                                            @RequestParam(defaultValue = "nombre") String sortBy,
                                                            @RequestParam(defaultValue = "asc") String sortDir,
                                                            @AuthenticationPrincipal Usuario usuarioActual) {
        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<PlanTarifa> planes = planTarifaService.buscarConFiltros(filtro, activo, pageable);
            Page<PlanTarifaDTO> planesDTO = planes.map(planTarifaService::convertirADTO);
            
            return ResponseEntity.ok(planesDTO);
            
        } catch (Exception e) {
            log.error("Error al filtrar planes: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== FORMULARIOS WEB ====================

    @PostMapping("/guardar")
    public String guardarPlan(@Valid @ModelAttribute PlanTarifaDTO planTarifaDTO,
                             @AuthenticationPrincipal Usuario usuarioActual,
                             Model model) {
        
        try {
            if (!puedeGestionarPlanes(usuarioActual)) {
                model.addAttribute("error", "No tiene permisos para gestionar planes de tarifa");
                return "admin/plan-tarifa-form";
            }
            
            if (planTarifaDTO.getUuid() == null) {
                // Crear nuevo plan
                planTarifaService.crear(planTarifaDTO);
                model.addAttribute("success", "Plan de tarifa creado exitosamente");
            } else {
                // Actualizar plan existente
                planTarifaService.actualizar(planTarifaDTO.getUuid(), planTarifaDTO);
                model.addAttribute("success", "Plan de tarifa actualizado exitosamente");
            }
            
            return "redirect:/admin/planes-tarifa";
            
        } catch (Exception e) {
            log.error("Error al guardar plan: ", e);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("planTarifaDTO", planTarifaDTO);
            return "admin/plan-tarifa-form";
        }
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioCrear(@AuthenticationPrincipal Usuario usuarioActual, Model model) {
        if (!puedeGestionarPlanes(usuarioActual)) {
            return "redirect:/admin/planes-tarifa";
        }
        
        model.addAttribute("planTarifaDTO", new PlanTarifaDTO());
        model.addAttribute("usuarioActual", usuarioActual);
        model.addAttribute("esNuevo", true);
        
        return "admin/plan-tarifa-form";
    }

    @GetMapping("/{uuid}/editar")
    public String mostrarFormularioEditar(@PathVariable UUID uuid,
                                         @AuthenticationPrincipal Usuario usuarioActual,
                                         Model model) {
        if (!puedeGestionarPlanes(usuarioActual)) {
            return "redirect:/admin/planes-tarifa";
        }
        
        try {
            PlanTarifa plan = planTarifaService.buscarPorUuid(uuid);
            PlanTarifaDTO planDTO = planTarifaService.convertirADTO(plan);
            
            model.addAttribute("planTarifaDTO", planDTO);
            model.addAttribute("usuarioActual", usuarioActual);
            model.addAttribute("esNuevo", false);
            
            return "admin/plan-tarifa-form";
            
        } catch (Exception e) {
            log.error("Error al cargar plan para edición: ", e);
            model.addAttribute("error", "Plan de tarifa no encontrado");
            return "redirect:/admin/planes-tarifa";
        }
    }

    // ==================== ENDPOINTS LEGACY (Para compatibilidad) ====================

    /**
     * @deprecated Usar /api/listar en su lugar
     */
    @Deprecated
    @GetMapping("/paginado")
    @ResponseBody
    public ResponseEntity<Page<PlanTarifa>> listarTodosPaginado(Pageable pageable) {
        try {
            Page<PlanTarifa> planes = planTarifaService.listarTodosPaginado(pageable);
            return ResponseEntity.ok(planes);
        } catch (Exception e) {
            log.error("Error al listar planes paginado: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @deprecated Usar /api/{uuid} en su lugar
     */
    @Deprecated
    @GetMapping("/{uuid}")
    @ResponseBody
    public ResponseEntity<PlanTarifaDTO> buscarPorUuidLegacy(@PathVariable UUID uuid) {
        try {
            PlanTarifa plan = planTarifaService.buscarPorUuid(uuid);
            PlanTarifaDTO planDTO = planTarifaService.convertirADTO(plan);
            return ResponseEntity.ok(planDTO);
        } catch (RuntimeException e) {
            log.error("Plan no encontrado: ", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error inesperado: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @deprecated Usar /api/{uuid}/detalle en su lugar
     */
    @Deprecated
    @GetMapping("/detalles/{uuid}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerDetallePlanLegacy(@PathVariable UUID uuid) {
        try {
            Map<String, Object> detalle = planTarifaService.obtenerDetallePlan(uuid);
            return ResponseEntity.ok(detalle);
        } catch (RuntimeException e) {
            log.error("Error al obtener detalle del plan: ", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error inesperado: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== MÉTODOS DE UTILIDAD PRIVADOS ====================

    private boolean puedeGestionarPlanes(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR || 
               usuario.getPerfil() == PerfilUsuario.SUPERVISOR;
    }

    private boolean puedeVerReportes(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR || 
               usuario.getPerfil() == PerfilUsuario.SUPERVISOR ||
               usuario.getPerfil() == PerfilUsuario.COORDINADOR;
    }

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