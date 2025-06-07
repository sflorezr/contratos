package co.empresa.gestioncontratos.controller;

import co.empresa.gestioncontratos.dto.TarifaDTO;
import co.empresa.gestioncontratos.entity.Tarifa;
import co.empresa.gestioncontratos.entity.Usuario;
import co.empresa.gestioncontratos.enums.TipoPredio;
import co.empresa.gestioncontratos.service.PlanTarifaService;
import co.empresa.gestioncontratos.service.ServicioService;
import co.empresa.gestioncontratos.service.TarifaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin/tarifas")
@RequiredArgsConstructor
@Slf4j
public class TarifaController {

    private final TarifaService tarifaService;    
    private final ServicioService servicioService;
    private final PlanTarifaService planTarifaService;

    // ==================== CONSULTAS ====================

    @GetMapping
    public String listar(@AuthenticationPrincipal Usuario usuarioActual,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String filtro,
                        @RequestParam(required = false) Boolean activo,
                        @RequestParam(required = false) UUID planTarifaUuid,
                        @RequestParam(required = false) UUID servicioUuid,
                        Model model) {
        
        log.info("=== ACCEDIENDO A LISTA DE TARIFAS ===");
        log.info("Usuario: {}, Filtros - texto: {}, activo: {}, plan: {}, servicio: {}", 
            usuarioActual.getUsername(), filtro, activo, planTarifaUuid, servicioUuid);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("fechaCreacion").descending());
            Page<Tarifa> tarifas = tarifaService.buscarConFiltros(filtro, activo, planTarifaUuid, servicioUuid, pageable);
            
            model.addAttribute("tarifas", tarifas);
            model.addAttribute("usuarioActual", usuarioActual);
            model.addAttribute("filtroActual", filtro);
            model.addAttribute("activoActual", activo);
            model.addAttribute("planTarifaActual", planTarifaUuid);
            model.addAttribute("servicioActual", servicioUuid);
            
            // Listas para filtros
            model.addAttribute("planesTarifas", planTarifaService.listarActivos());
            model.addAttribute("servicios", servicioService.listarActivos());
            
            // Estadísticas
            Map<String, Object> stats = tarifaService.obtenerResumenGeneral();
            model.addAttribute("stats", stats);
            
            return "admin/tarifas";
            
        } catch (Exception e) {
            log.error("Error al listar tarifas: ", e);
            model.addAttribute("error", "Error al cargar la lista de tarifas");
            return "admin/tarifas";
        }
    }
    @GetMapping("/resumen/general")
    public ResponseEntity<Map<String, Object>> obtenerResumenGeneral() {
        try {
            Map<String, Object> resumen = tarifaService.obtenerResumenGeneral();
            return ResponseEntity.ok(resumen);
        } catch (Exception e) {
            log.error("Error al obtener resumen general: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/detalles")
    public ResponseEntity<List<TarifaDTO>> listarTarifasConDetalles(
            @RequestParam(required = false) String filtro,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) UUID planTarifaUuid,
            @RequestParam(required = false) UUID servicioUuid) {
        try {
            List<TarifaDTO> tarifas = tarifaService.listarTarifasConFiltros(
                filtro, 
                activo, 
                planTarifaUuid, 
                servicioUuid
            );
            return ResponseEntity.ok(tarifas);
        } catch (Exception e) {
            log.error("Error al listar tarifas con detalles: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/paginado")
    public ResponseEntity<Page<Tarifa>> listarTodasPaginado(Pageable pageable) {
        try {
            Page<Tarifa> tarifas = tarifaService.listarTodasPaginado(pageable);
            return ResponseEntity.ok(tarifas);
        } catch (Exception e) {
            log.error("Error al listar tarifas paginado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/plan/{planTarifaUuid}")
    public ResponseEntity<List<Tarifa>> listarPorPlanTarifa(@PathVariable UUID planTarifaUuid) {
        try {
            List<Tarifa> tarifas = tarifaService.listarPorPlanTarifa(planTarifaUuid);
            return ResponseEntity.ok(tarifas);
        } catch (RuntimeException e) {
            log.error("Error al listar tarifas por plan: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error inesperado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/servicio/{servicioUuid}")
    public ResponseEntity<List<Tarifa>> listarPorServicio(@PathVariable UUID servicioUuid) {
        try {
            List<Tarifa> tarifas = tarifaService.listarPorServicio(servicioUuid);
            return ResponseEntity.ok(tarifas);
        } catch (RuntimeException e) {
            log.error("Error al listar tarifas por servicio: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error inesperado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<Tarifa> buscarPorUuid(@PathVariable UUID uuid) {
        try {
            Tarifa tarifa = tarifaService.buscarPorUuid(uuid);
            return ResponseEntity.ok(tarifa);
        } catch (RuntimeException e) {
            log.error("Tarifa no encontrada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error inesperado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/plan/{planTarifaUuid}/servicio/{servicioUuid}")
    public ResponseEntity<Tarifa> buscarPorPlanYServicio(
            @PathVariable UUID planTarifaUuid,
            @PathVariable UUID servicioUuid) {
        try {
            return tarifaService.buscarPorPlanYServicio(planTarifaUuid, servicioUuid)
                    .map(tarifa -> ResponseEntity.ok(tarifa))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error al buscar tarifa por plan y servicio: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== GESTIÓN DE TARIFAS ====================

    @PostMapping
    public ResponseEntity<Tarifa> crear(@Valid @RequestBody TarifaDTO tarifaDTO) {
        try {
            Tarifa tarifa = tarifaService.crear(tarifaDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(tarifa);
        } catch (RuntimeException e) {
            log.error("Error al crear tarifa: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error inesperado al crear tarifa: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<Tarifa> actualizar(
            @PathVariable UUID uuid,
            @Valid @RequestBody TarifaDTO tarifaDTO) {
        try {
            Tarifa tarifa = tarifaService.actualizar(uuid, tarifaDTO);
            return ResponseEntity.ok(tarifa);
        } catch (RuntimeException e) {
            log.error("Error al actualizar tarifa: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error inesperado al actualizar tarifa: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{uuid}/estado")
    public ResponseEntity<Void> cambiarEstado(@PathVariable UUID uuid) {
        try {
            tarifaService.cambiarEstado(uuid);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Error al cambiar estado de tarifa: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error inesperado al cambiar estado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID uuid) {
        try {
            tarifaService.eliminar(uuid);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error al eliminar tarifa: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error inesperado al eliminar tarifa: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== CONSULTAS ESPECÍFICAS ====================

    @GetMapping("/precio/plan/{planTarifaUuid}/servicio/{servicioUuid}")
    public ResponseEntity<BigDecimal> obtenerPrecioPorServicioYTipo(
            @PathVariable UUID planTarifaUuid,
            @PathVariable UUID servicioUuid,
            @RequestParam TipoPredio tipoPredio) {
        try {
            BigDecimal precio = tarifaService.obtenerPrecioPorServicioYTipo(
                    planTarifaUuid, servicioUuid, tipoPredio);
            return ResponseEntity.ok(precio);
        } catch (RuntimeException e) {
            log.error("Error al obtener precio: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error inesperado al obtener precio: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/plan/{planTarifaUuid}/detalles")
    public ResponseEntity<List<TarifaDTO>> listarTarifasConDetalles(@PathVariable UUID planTarifaUuid) {
        try {
            List<TarifaDTO> tarifas = tarifaService.listarTarifasConDetalles(planTarifaUuid);
            return ResponseEntity.ok(tarifas);
        } catch (RuntimeException e) {
            log.error("Error al listar tarifas con detalles: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error inesperado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/plan/{planTarifaUuid}/resumen")
    public ResponseEntity<Map<String, Object>> obtenerResumenTarifasPorPlan(@PathVariable UUID planTarifaUuid) {
        try {
            Map<String, Object> resumen = tarifaService.obtenerResumenTarifasPorPlan(planTarifaUuid);
            return ResponseEntity.ok(resumen);
        } catch (RuntimeException e) {
            log.error("Error al obtener resumen: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error inesperado al obtener resumen: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== OPERACIONES MASIVAS ====================

    @PostMapping("/plan/{planTarifaUuid}/masivo")
    public ResponseEntity<List<Tarifa>> crearTarifasMasivo(
            @PathVariable UUID planTarifaUuid,
            @Valid @RequestBody List<TarifaDTO> tarifasDTO) {
        try {
            List<Tarifa> tarifas = tarifaService.crearTarifasMasivo(planTarifaUuid, tarifasDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(tarifas);
        } catch (RuntimeException e) {
            log.error("Error en creación masiva: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error inesperado en creación masiva: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/plan/{planTarifaUuid}/actualizar-precios")
    public ResponseEntity<Void> actualizarPreciosMasivo(
            @PathVariable UUID planTarifaUuid,
            @RequestParam BigDecimal porcentajeAumento) {
        try {
            tarifaService.actualizarPreciosMasivo(planTarifaUuid, porcentajeAumento);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Error al actualizar precios masivo: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error inesperado en actualización masiva: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @PostMapping("/cargar-excel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cargarExcel(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("planTarifaUuid") UUID planTarifaUuid) {
        
        Map<String, Object> response = new java.util.HashMap<>();
        
        try {
            if (archivo.isEmpty()) {
                response.put("success", false);
                response.put("message", "Debe seleccionar un archivo");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> resultado = tarifaService.cargarTarifasDesdeExcel(archivo, planTarifaUuid);
            
            response.put("success", true);
            response.put("message", "Archivo procesado exitosamente");
            response.putAll(resultado);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al procesar archivo Excel: ", e);
            response.put("success", false);
            response.put("message", "Error al procesar archivo: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    @GetMapping("/plantilla-excel")
    public ResponseEntity<byte[]> descargarPlantilla() {
        try {
            byte[] plantilla = tarifaService.generarPlantillaExcel();
            
            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.ms-excel")
                .header("Content-Disposition", "attachment; filename=plantilla_tarifas.xls")
                .body(plantilla);
                
        } catch (Exception e) {
            log.error("Error al generar plantilla: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


}