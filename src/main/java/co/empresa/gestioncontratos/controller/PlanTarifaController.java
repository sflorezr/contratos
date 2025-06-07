package co.empresa.gestioncontratos.controller;

import co.empresa.gestioncontratos.dto.PlanTarifaDTO;
import co.empresa.gestioncontratos.dto.SectorDTO;
import co.empresa.gestioncontratos.dto.ZonaDTO;
import co.empresa.gestioncontratos.entity.PlanTarifa;
import co.empresa.gestioncontratos.entity.Sector;
import co.empresa.gestioncontratos.entity.Usuario;
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

    // ==================== CONSULTAS ====================

    @GetMapping
    public String listar(@AuthenticationPrincipal Usuario usuarioActual,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String filtro,
                        @RequestParam(required = false) Boolean activo,
                        Model model) {
        
        log.info("=== ACCEDIENDO A LISTA DE PLANES DE TARIFA ===");
        log.info("Usuario: {}, Filtros - texto: {}, activo: {}", 
            usuarioActual.getUsername(), filtro, activo);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("fechaCreacion").descending());
            Page<PlanTarifa> planes = planTarifaService.buscarConFiltros(filtro, activo, pageable);
            
            model.addAttribute("planes", planes);
            model.addAttribute("usuarioActual", usuarioActual);
            model.addAttribute("filtroActual", filtro);
            model.addAttribute("activoActual", activo);
            
            // Estadísticas
            Map<String, Object> stats = planTarifaService.obtenerResumenGeneral();
            model.addAttribute("stats", stats);
            
            return "admin/planes-tarifa";
            
        } catch (Exception e) {
            log.error("Error al listar planes de tarifa: ", e);
            model.addAttribute("error", "Error al cargar la lista de planes de tarifa");
            return "admin/planes-tarifa";
        }
    }
    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<List<PlanTarifaDTO>> listarPlanesAPI() {
        try {
            List<PlanTarifa> planes = planTarifaService.listarTodos();
            List<PlanTarifaDTO> planesDTO = planes.stream()
                .map(planTarifaService::convertirADTOConEstadisticas) // Con estadísticas completas
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(planesDTO);
        } catch (Exception e) {
            log.error("Error al listar planes de tarifa: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/resumen/general")
    public ResponseEntity<Map<String, Object>> obtenerResumenGeneral() {
        try {
            Map<String, Object> resumen = planTarifaService.obtenerResumenGeneral();
            return ResponseEntity.ok(resumen);
        } catch (Exception e) {
            log.error("Error al obtener resumen general: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/paginado")
    public ResponseEntity<Page<PlanTarifa>> listarTodosPaginado(Pageable pageable) {
        try {
            Page<PlanTarifa> planes = planTarifaService.listarTodosPaginado(pageable);
            return ResponseEntity.ok(planes);
        } catch (Exception e) {
            log.error("Error al listar planes paginado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/listar-simple")
    @ResponseBody
    public ResponseEntity<List<PlanTarifaDTO>> listarPlanesSimple() {
        try {
            List<PlanTarifa> planTarifa = planTarifaService.listarActivos();
            List<PlanTarifaDTO> planTarifaDTOs = planTarifa.stream()
                .map(planTarifaService::convertirADTO) // Sin estadísticas
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(planTarifaDTOs);
        } catch (Exception e) {
            log.error("Error al listar planes tarifa: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }  


    @GetMapping("/{uuid}")
    public ResponseEntity<PlanTarifaDTO> buscarPorUuid(@PathVariable UUID uuid) {
        try {
            PlanTarifa plan = planTarifaService.buscarPorUuid(uuid);
            PlanTarifaDTO planDTO = planTarifaService.convertirADTO(plan);
            return ResponseEntity.ok(planDTO);
        } catch (RuntimeException e) {
            log.error("Plan no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error inesperado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/detalles/{uuid}")
    public ResponseEntity<Map<String, Object>> obtenerDetallePlan(@PathVariable UUID uuid) {
        try {
            Map<String, Object> detalle = null;
            if(!uuid.toString().isEmpty()){
             detalle = planTarifaService.obtenerDetallePlan(uuid);
            return ResponseEntity.ok(detalle);
            }else{
              return ResponseEntity.ok(detalle);
            }                    
        } catch (RuntimeException e) {
            log.error("Error al obtener detalle del plan: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error inesperado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== GESTIÓN DE PLANES ====================

    @PostMapping
    public ResponseEntity<PlanTarifa> crear(@Valid @RequestBody PlanTarifaDTO planTarifaDTO) {
        try {
            PlanTarifa plan = planTarifaService.crear(planTarifaDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(plan);
        } catch (RuntimeException e) {
            log.error("Error al crear plan: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error inesperado al crear plan: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<PlanTarifa> actualizar(
            @PathVariable UUID uuid,
            @Valid @RequestBody PlanTarifaDTO planTarifaDTO) {
        try {
            PlanTarifa plan = planTarifaService.actualizar(uuid, planTarifaDTO);
            return ResponseEntity.ok(plan);
        } catch (RuntimeException e) {
            log.error("Error al actualizar plan: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error inesperado al actualizar plan: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{uuid}/estado")
    public ResponseEntity<Void> cambiarEstado(@PathVariable UUID uuid) {
        try {
            planTarifaService.cambiarEstado(uuid);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Error al cambiar estado del plan: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error inesperado al cambiar estado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID uuid) {
        try {
            planTarifaService.eliminar(uuid);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error al eliminar plan: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error inesperado al eliminar plan: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== CONSULTAS ESPECÍFICAS ====================

    @GetMapping("/con-tarifas")
    public ResponseEntity<List<PlanTarifa>> listarPlanesConTarifas() {
        try {
            List<PlanTarifa> planes = planTarifaService.buscarPlanesConTarifas();
            return ResponseEntity.ok(planes);
        } catch (Exception e) {
            log.error("Error al listar planes con tarifas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/sin-tarifas")
    public ResponseEntity<List<PlanTarifa>> listarPlanesSinTarifas() {
        try {
            List<PlanTarifa> planes = planTarifaService.buscarPlanesSinTarifas();
            return ResponseEntity.ok(planes);
        } catch (Exception e) {
            log.error("Error al listar planes sin tarifas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}