package co.empresa.gestioncontratos.controller;

import co.empresa.gestioncontratos.dto.PredioDTO;
import co.empresa.gestioncontratos.entity.Predio;
import co.empresa.gestioncontratos.entity.Usuario;
import co.empresa.gestioncontratos.enums.PerfilUsuario;
import co.empresa.gestioncontratos.enums.TipoPredio;
import co.empresa.gestioncontratos.service.PredioService;
import co.empresa.gestioncontratos.service.SectorService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.*;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/predios")
@RequiredArgsConstructor
@Slf4j
public class PredioController {

    private final PredioService predioService;
    private final SectorService sectorService;

    // ==================== VISTAS WEB ====================

    @GetMapping
    public String listar(@AuthenticationPrincipal Usuario usuarioActual,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String filtro,
                        @RequestParam(required = false) UUID sectorUuid,
                        @RequestParam(required = false) TipoPredio tipo,
                        Model model) {
        
        log.info("=== ACCEDIENDO A LISTA DE PREDIOS ===");
        log.info("Usuario: {}, Filtros - texto: {}, sector: {}, tipo: {}", 
            usuarioActual.getUsername(), filtro, sectorUuid, tipo);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("direccion").ascending());
            Page<Predio> predios = predioService.buscarConFiltros(filtro, sectorUuid, tipo, pageable);
            
            model.addAttribute("predios", predios);
            model.addAttribute("sectores", sectorService.listarTodos());
            model.addAttribute("tipos", TipoPredio.values());
            model.addAttribute("usuarioActual", usuarioActual);
            
            // Filtros actuales
            model.addAttribute("filtroActual", filtro);
            model.addAttribute("sectorActual", sectorUuid);
            model.addAttribute("tipoActual", tipo);
            
            // Estadísticas
            Map<String, Long> stats = predioService.obtenerResumenPredios();
            model.addAttribute("stats", stats);
            
            return "admin/predios";
            
        } catch (Exception e) {
            log.error("Error al listar predios: ", e);
            model.addAttribute("error", "Error al cargar la lista de predios");
            return "admin/predios";
        }
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        log.info("=== MOSTRANDO FORMULARIO NUEVO PREDIO ===");
        
        model.addAttribute("predio", new PredioDTO());
        model.addAttribute("sectores", sectorService.listarTodos());
        model.addAttribute("tipos", TipoPredio.values());
        model.addAttribute("esNuevo", true);
        
        return "admin/predio-form";
    }

    @PostMapping("/nuevo")
    public String crear(@Valid @ModelAttribute PredioDTO predioDTO,
                       BindingResult result,
                       RedirectAttributes redirectAttributes,
                       Model model) {
        
        log.info("=== CREANDO NUEVO PREDIO ===");
        log.info("Datos recibidos: {}", predioDTO);
        
        try {
            if (result.hasErrors()) {
                log.warn("Errores de validación encontrados");
                model.addAttribute("sectores", sectorService.listarTodos());
                model.addAttribute("tipos", TipoPredio.values());
                model.addAttribute("esNuevo", true);
                return "admin/predio-form";
            }
            
            Predio predio = predioService.crear(predioDTO);
            log.info("✅ Predio creado exitosamente: {}", predio.getCodigoCatastral());
            
            redirectAttributes.addFlashAttribute("success", 
                "Predio creado exitosamente en " + predio.getDireccion());
            
            return "redirect:/admin/predios";
            
        } catch (Exception e) {
            log.error("❌ Error al crear predio: ", e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al crear predio: " + e.getMessage());
            return "redirect:/admin/predios";
        }
    }

    @GetMapping("/{uuid}")
    public String ver(@PathVariable UUID uuid, Model model) {
        log.info("=== VIENDO PREDIO: {} ===", uuid);
        
        try {
            Predio predio = predioService.buscarPorUuid(uuid);
            Map<String, Object> estadisticas = predioService.obtenerEstadisticasPredio(uuid);
            List<Map<String, Object>> historial = predioService.obtenerHistorialAsignaciones(uuid);
            
            model.addAttribute("predio", predio);
            model.addAttribute("estadisticas", estadisticas);
            model.addAttribute("historial", historial);
            
            return "admin/predio-detalle";
            
        } catch (Exception e) {
            log.error("Error al buscar predio: ", e);
            return "redirect:/admin/predios";
        }
    }

    @GetMapping("/{uuid}/editar")
    public String mostrarFormularioEditar(@PathVariable UUID uuid, Model model) {
        log.info("=== EDITANDO PREDIO: {} ===", uuid);
        
        try {
            Predio predio = predioService.buscarPorUuid(uuid);
            PredioDTO predioDTO = predioService.convertirADTO(predio);
            
            model.addAttribute("predio", predioDTO);
            model.addAttribute("sectores", sectorService.listarTodos());
            model.addAttribute("tipos", TipoPredio.values());
            model.addAttribute("esNuevo", false);
            
            return "admin/predio-form";
            
        } catch (Exception e) {
            log.error("Error al buscar predio para editar: ", e);
            return "redirect:/admin/predios";
        }
    }

    @PostMapping("/{uuid}/editar")
    public String actualizar(@PathVariable UUID uuid,
                           @Valid @ModelAttribute PredioDTO predioDTO,
                           BindingResult result,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        
        log.info("=== ACTUALIZANDO PREDIO: {} ===", uuid);
        
        try {
            if (result.hasErrors()) {
                model.addAttribute("sectores", sectorService.listarTodos());
                model.addAttribute("tipos", TipoPredio.values());
                model.addAttribute("esNuevo", false);
                return "admin/predio-form";
            }
            
            Predio predio = predioService.actualizar(uuid, predioDTO);
            redirectAttributes.addFlashAttribute("success", 
                "Predio actualizado exitosamente");
            
            return "redirect:/admin/predios";
            
        } catch (Exception e) {
            log.error("Error al actualizar predio: ", e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al actualizar predio: " + e.getMessage());
            return "redirect:/admin/predios";
        }
    }

    // ==================== API REST ====================

    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<List<PredioDTO>> listarTodos() {
        log.info("=== API: LISTANDO TODOS LOS PREDIOS ===");
        
        try {
            List<Predio> predios = predioService.listarTodos();
            List<PredioDTO> prediosDTO = predios.stream()
                .map(predioService::convertirADTO)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(prediosDTO);
        } catch (Exception e) {
            log.error("Error al listar predios: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/buscar")
    @ResponseBody
    public ResponseEntity<Page<PredioDTO>> buscarPredios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String filtro,
            @RequestParam(required = false) UUID sectorUuid,
            @RequestParam(required = false) TipoPredio tipo) {
        
        log.info("=== API: BUSCANDO PREDIOS ===");
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("direccion").ascending());
            Page<Predio> predios = predioService.buscarConFiltros(filtro, sectorUuid, tipo, pageable);
            
            Page<PredioDTO> prediosDTO = predios.map(predioService::convertirADTO);
            
            return ResponseEntity.ok(prediosDTO);
        } catch (Exception e) {
            log.error("Error en búsqueda de predios: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/{uuid}")
    @ResponseBody
    public ResponseEntity<PredioDTO> obtenerPredio(@PathVariable UUID uuid) {
        try {
            Predio predio = predioService.buscarPorUuid(uuid);
            return ResponseEntity.ok(predioService.convertirADTO(predio));
        } catch (Exception e) {
            log.error("Error al obtener predio: ", e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<?> crearPredio(@Valid @RequestBody PredioDTO predioDTO) {
        log.info("=== API: CREANDO PREDIO ===");
        
        try {
            Predio predio = predioService.crear(predioDTO);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Predio creado exitosamente",
                "predio", predioService.convertirADTO(predio)
            ));
        } catch (Exception e) {
            log.error("Error al crear predio: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{uuid}")
    @ResponseBody
    public ResponseEntity<?> actualizarPredio(@PathVariable UUID uuid,
                                            @Valid @RequestBody PredioDTO predioDTO) {
        log.info("=== API: ACTUALIZANDO PREDIO {} ===", uuid);
        
        try {
            Predio predio = predioService.actualizar(uuid, predioDTO);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Predio actualizado exitosamente",
                "predio", predioService.convertirADTO(predio)
            ));
        } catch (Exception e) {
            log.error("Error al actualizar predio: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PatchMapping("/{uuid}/estado")
    @ResponseBody
    public ResponseEntity<?> cambiarEstado(@PathVariable UUID uuid,
                                         @RequestBody Map<String, Boolean> estado) {
        log.info("=== CAMBIANDO ESTADO PREDIO: {} ===", uuid);
        
        try {
            predioService.cambiarEstado(uuid);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Estado del predio actualizado"
            ));
        } catch (Exception e) {
            log.error("Error al cambiar estado: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{uuid}")
    @ResponseBody
    public ResponseEntity<?> eliminar(@PathVariable UUID uuid) {
        log.info("=== ELIMINANDO PREDIO: {} ===", uuid);
        
        try {
            predioService.eliminar(uuid);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Predio eliminado exitosamente"
            ));
        } catch (Exception e) {
            log.error("Error al eliminar predio: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ==================== ENDPOINTS ESPECÍFICOS ====================

    @GetMapping("/api/disponibles")
    @ResponseBody
    public ResponseEntity<List<PredioDTO>> listarDisponibles() {
        log.info("=== API: LISTANDO PREDIOS DISPONIBLES ===");
        
        try {
            List<Predio> predios = predioService.listarPrediosDisponibles();
            List<PredioDTO> prediosDTO = predios.stream()
                .map(predioService::convertirADTO)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(prediosDTO);
        } catch (Exception e) {
            log.error("Error al listar predios disponibles: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/contrato/{contratoUuid}/disponibles")
    @ResponseBody
    public ResponseEntity<List<PredioDTO>> listarDisponiblesParaContrato(@PathVariable UUID contratoUuid) {
        log.info("=== API: LISTANDO PREDIOS DISPONIBLES PARA CONTRATO {} ===", contratoUuid);
        
        try {
            List<Predio> predios = predioService.listarPrediosDisponiblesParaContrato(contratoUuid);
            List<PredioDTO> prediosDTO = predios.stream()
                .map(predioService::convertirADTO)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(prediosDTO);
        } catch (Exception e) {
            log.error("Error al listar predios disponibles para contrato: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/validar-codigo")
    @ResponseBody
    public ResponseEntity<?> validarCodigoCatastral(@RequestParam String codigo,
                                                   @RequestParam(required = false) UUID uuid) {
        log.debug("Validando código catastral: {}", codigo);
        
        try {
            boolean existe = predioService.existeCodigoCatastral(codigo);
            
            // Si estamos editando, verificar que no sea el mismo predio
            if (existe && uuid != null) {
                Predio predio = predioService.buscarPorCodigoCatastral(codigo);
                existe = !predio.getUuid().equals(uuid);
            }
            
            return ResponseEntity.ok(Map.of(
                "existe", existe,
                "disponible", !existe
            ));
        } catch (Exception e) {
            log.error("Error al validar código catastral: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Error al validar código"
            ));
        }
    }

    // ==================== IMPORTACIÓN/EXPORTACIÓN ====================

    @PostMapping("/importar")
    @ResponseBody
    public ResponseEntity<?> importarPredios(@RequestParam("archivo") MultipartFile archivo) {
        log.info("=== IMPORTANDO PREDIOS DESDE ARCHIVO ===");
        
        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "El archivo está vacío"
            ));
        }
        
        try {
            // Aquí implementarías la lógica de importación desde CSV/Excel
            // List<PredioDTO> prediosDTO = parsearArchivo(archivo);
            // List<Predio> prediosImportados = predioService.importarPredios(prediosDTO);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Predios importados exitosamente",
                "cantidad", 0 // prediosImportados.size()
            ));
        } catch (Exception e) {
            log.error("Error al importar predios: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al importar: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/exportar")
    public ResponseEntity<?> exportarPredios(@RequestParam(required = false) UUID sectorUuid,
                                           @RequestParam(required = false) TipoPredio tipo,
                                           @RequestParam(defaultValue = "csv") String formato) {
        log.info("=== EXPORTANDO PREDIOS ===");
        
        try {
            List<Map<String, Object>> datos = predioService.exportarPredios(sectorUuid, tipo);
            
            // Aquí implementarías la lógica de exportación a CSV/Excel
            // byte[] archivo = generarArchivo(datos, formato);
            
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=predios." + formato)
                .body("Datos de exportación"); // En realidad sería el archivo
                
        } catch (Exception e) {
            log.error("Error al exportar predios: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al exportar predios");
        }
    }

    // ==================== MÉTODOS DE UTILIDAD ====================

    @GetMapping("/api/estadisticas")
    @ResponseBody
    public ResponseEntity<?> obtenerEstadisticas() {
        log.info("=== API: OBTENIENDO ESTADÍSTICAS DE PREDIOS ===");
        
        try {
            Map<String, Long> resumen = predioService.obtenerResumenPredios();
            return ResponseEntity.ok(resumen);
        } catch (Exception e) {
            log.error("Error al obtener estadísticas: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/{uuid}/historial")
    @ResponseBody
    public ResponseEntity<?> obtenerHistorial(@PathVariable UUID uuid) {
        log.info("=== API: OBTENIENDO HISTORIAL DEL PREDIO {} ===", uuid);
        
        try {
            List<Map<String, Object>> historial = predioService.obtenerHistorialAsignaciones(uuid);
            return ResponseEntity.ok(historial);
        } catch (Exception e) {
            log.error("Error al obtener historial: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}