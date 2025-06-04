package co.empresa.gestioncontratos.controller;

import co.empresa.gestioncontratos.dto.SectorDTO;
import co.empresa.gestioncontratos.entity.Sector;
import co.empresa.gestioncontratos.entity.Usuario;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/sectores")
@RequiredArgsConstructor
@Slf4j
public class SectorController {

    private final SectorService sectorService;

    // ==================== VISTAS WEB ====================

    @GetMapping
    public String listar(@AuthenticationPrincipal Usuario usuarioActual,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String filtro,
                        @RequestParam(required = false) Boolean activo,
                        Model model) {
        
        log.info("=== ACCEDIENDO A LISTA DE SECTORES ===");
        log.info("Usuario: {}, Filtros - texto: {}, activo: {}", 
            usuarioActual.getUsername(), filtro, activo);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
            Page<Sector> sectores = sectorService.buscarConFiltros(filtro, activo, pageable);
            
            model.addAttribute("sectores", sectores);
            model.addAttribute("usuarioActual", usuarioActual);
            model.addAttribute("filtroActual", filtro);
            model.addAttribute("activoActual", activo);
            
            // Estadísticas
            Map<String, Long> stats = sectorService.obtenerResumenGeneral();
            model.addAttribute("stats", stats);
            
            return "admin/sectores";
            
        } catch (Exception e) {
            log.error("Error al listar sectores: ", e);
            model.addAttribute("error", "Error al cargar la lista de sectores");
            return "admin/sectores";
        }
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        log.info("=== MOSTRANDO FORMULARIO NUEVO SECTOR ===");
        
        model.addAttribute("sector", new SectorDTO());
        model.addAttribute("esNuevo", true);
        
        return "admin/sector-form";
    }

    @PostMapping("/nuevo")
    public String crear(@Valid @ModelAttribute SectorDTO sectorDTO,
                       BindingResult result,
                       RedirectAttributes redirectAttributes,
                       Model model) {
        
        log.info("=== CREANDO NUEVO SECTOR ===");
        log.info("Datos recibidos: {}", sectorDTO);
        
        try {
            if (result.hasErrors()) {
                log.warn("Errores de validación encontrados");
                model.addAttribute("esNuevo", true);
                return "admin/sector-form";
            }
            
            Sector sector = sectorService.crear(sectorDTO);
            log.info("✅ Sector creado exitosamente: {}", sector.getNombre());
            
            redirectAttributes.addFlashAttribute("success", 
                "Sector '" + sector.getNombre() + "' creado exitosamente");
            
            return "redirect:/admin/sectores";
            
        } catch (Exception e) {
            log.error("❌ Error al crear sector: ", e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al crear sector: " + e.getMessage());
            return "redirect:/admin/sectores";
        }
    }

    @GetMapping("/{uuid}")
    public String ver(@PathVariable UUID uuid, Model model) {
        log.info("=== VIENDO SECTOR: {} ===", uuid);
        
        try {
            Sector sector = sectorService.buscarPorUuid(uuid);
            Map<String, Object> estadisticas = sectorService.obtenerEstadisticas(uuid);
            
            model.addAttribute("sector", sector);
            model.addAttribute("estadisticas", estadisticas);
            
            return "admin/sector-detalle";
            
        } catch (Exception e) {
            log.error("Error al buscar sector: ", e);
            return "redirect:/admin/sectores";
        }
    }

    @GetMapping("/{uuid}/editar")
    public String mostrarFormularioEditar(@PathVariable UUID uuid, Model model) {
        log.info("=== EDITANDO SECTOR: {} ===", uuid);
        
        try {
            Sector sector = sectorService.buscarPorUuid(uuid);
            SectorDTO sectorDTO = sectorService.convertirADTO(sector);
            
            model.addAttribute("sector", sectorDTO);
            model.addAttribute("esNuevo", false);
            
            return "admin/sector-form";
            
        } catch (Exception e) {
            log.error("Error al buscar sector para editar: ", e);
            return "redirect:/admin/sectores";
        }
    }

    @PostMapping("/{uuid}/editar")
    public String actualizar(@PathVariable UUID uuid,
                           @Valid @ModelAttribute SectorDTO sectorDTO,
                           BindingResult result,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        
        log.info("=== ACTUALIZANDO SECTOR: {} ===", uuid);
        
        try {
            if (result.hasErrors()) {
                model.addAttribute("esNuevo", false);
                return "admin/sector-form";
            }
            
            Sector sector = sectorService.actualizar(uuid, sectorDTO);
            redirectAttributes.addFlashAttribute("success", 
                "Sector '" + sector.getNombre() + "' actualizado exitosamente");
            
            return "redirect:/admin/sectores";
            
        } catch (Exception e) {
            log.error("Error al actualizar sector: ", e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al actualizar sector: " + e.getMessage());
            return "redirect:/admin/sectores";
        }
    }

    // ==================== API REST ====================

    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<List<SectorDTO>> listarTodos() {
        log.info("=== API: LISTANDO TODOS LOS SECTORES ===");
        
        try {
            // Opción 1: Usar el método optimizado del servicio (RECOMENDADO)
            List<SectorDTO> sectoresDTO = sectorService.listarTodosConEstadisticas();
            
            return ResponseEntity.ok(sectoresDTO);
            
        } catch (Exception e) {
            log.error("Error al listar sectores: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/activos")
    @ResponseBody
    public ResponseEntity<List<SectorDTO>> listarActivos() {
        log.info("=== API: LISTANDO SECTORES ACTIVOS ===");
        
        try {
            List<Sector> sectores = sectorService.listarActivos();
            List<SectorDTO> sectoresDTO = sectores.stream()
                .map(sectorService::convertirADTO)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(sectoresDTO);
        } catch (Exception e) {
            log.error("Error al listar sectores activos: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/{uuid}")
    @ResponseBody
    public ResponseEntity<SectorDTO> obtenerSector(@PathVariable UUID uuid) {
        try {
            Sector sector = sectorService.buscarPorUuid(uuid);
            return ResponseEntity.ok(sectorService.convertirADTO(sector));
        } catch (Exception e) {
            log.error("Error al obtener sector: ", e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<?> crearSector(@Valid @RequestBody SectorDTO sectorDTO) {
        log.info("=== API: CREANDO SECTOR ===");
        
        try {
            Sector sector = sectorService.crear(sectorDTO);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sector creado exitosamente",
                "sector", sectorService.convertirADTO(sector)
            ));
        } catch (Exception e) {
            log.error("Error al crear sector: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{uuid}")
    @ResponseBody
    public ResponseEntity<?> actualizarSector(@PathVariable UUID uuid,
                                            @Valid @RequestBody SectorDTO sectorDTO) {
        log.info("=== API: ACTUALIZANDO SECTOR {} ===", uuid);
        
        try {
            Sector sector = sectorService.actualizar(uuid, sectorDTO);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sector actualizado exitosamente",
                "sector", sectorService.convertirADTO(sector)
            ));
        } catch (Exception e) {
            log.error("Error al actualizar sector: ", e);
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
        log.info("=== CAMBIANDO ESTADO SECTOR: {} ===", uuid);
        
        try {
            sectorService.cambiarEstado(uuid);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Estado del sector actualizado"
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
        log.info("=== ELIMINANDO SECTOR: {} ===", uuid);
        
        try {
            sectorService.eliminar(uuid);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sector eliminado exitosamente"
            ));
        } catch (Exception e) {
            log.error("Error al eliminar sector: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ==================== ENDPOINTS ESPECÍFICOS ====================

    @GetMapping("/api/estadisticas")
    @ResponseBody
    public ResponseEntity<?> obtenerEstadisticasGenerales() {
        log.info("=== API: OBTENIENDO ESTADÍSTICAS GENERALES ===");
        
        try {
            Map<String, Long> estadisticas = sectorService.obtenerResumenGeneral();
            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            log.error("Error al obtener estadísticas: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{uuid}/estadisticas")
    @ResponseBody
    public ResponseEntity<?> obtenerEstadisticas(@PathVariable UUID uuid) {
        log.info("=== API: OBTENIENDO ESTADÍSTICAS DEL SECTOR {} ===", uuid);
        
        try {
            Map<String, Object> estadisticas = sectorService.obtenerEstadisticas(uuid);
            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            log.error("Error al obtener estadísticas del sector: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/resumen")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerResumenSectores() {
        log.info("=== API: OBTENIENDO RESUMEN DE SECTORES ===");
        
        try {
            List<Map<String, Object>> resumen = sectorService.obtenerResumenSectores();
            return ResponseEntity.ok(resumen);
        } catch (Exception e) {
            log.error("Error al obtener resumen: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/validar-nombre")
    @ResponseBody
    public ResponseEntity<?> validarNombre(@RequestParam String nombre,
                                         @RequestParam(required = false) UUID uuid) {
        log.debug("Validando nombre de sector: {}", nombre);
        
        try {
            boolean existe = sectorService.existeNombre(nombre);
            
            // Si estamos editando, verificar que no sea el mismo sector
            if (existe && uuid != null) {
                Sector sector = sectorService.buscarPorNombre(nombre);
                existe = !sector.getUuid().equals(uuid);
            }
            
            return ResponseEntity.ok(Map.of(
                "existe", existe,
                "disponible", !existe
            ));
        } catch (Exception e) {
            log.error("Error al validar nombre: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Error al validar nombre"
            ));
        }
    }

    @GetMapping("/api/validar-codigo")
    @ResponseBody
    public ResponseEntity<?> validarCodigo(@RequestParam String codigo,
                                         @RequestParam(required = false) UUID uuid) {
        log.debug("Validando código de sector: {}", codigo);
        
        try {
            boolean existe = sectorService.existeCodigo(codigo);
            
            // Si estamos editando, verificar que no sea el mismo sector
            if (existe && uuid != null) {
                Sector sector = sectorService.buscarPorCodigo(codigo);
                existe = !sector.getUuid().equals(uuid);
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

    // ==================== IMPORTACIÓN/EXPORTACIÓN ====================

    @PostMapping("/importar")
    @ResponseBody
    public ResponseEntity<?> importarSectores(@RequestParam("archivo") MultipartFile archivo) {
        log.info("=== IMPORTANDO SECTORES DESDE ARCHIVO ===");
        
        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "El archivo está vacío"
            ));
        }
        
        try {
            // Aquí implementarías la lógica de importación desde CSV/Excel
            // List<SectorDTO> sectoresDTO = parsearArchivo(archivo);
            // List<Sector> sectoresImportados = sectorService.importarSectores(sectoresDTO);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sectores importados exitosamente",
                "cantidad", 0 // sectoresImportados.size()
            ));
        } catch (Exception e) {
            log.error("Error al importar sectores: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al importar: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/exportar")
    public ResponseEntity<?> exportarSectores(@RequestParam(required = false) Boolean soloActivos,
                                            @RequestParam(defaultValue = "csv") String formato) {
        log.info("=== EXPORTANDO SECTORES ===");
        
        try {
            List<Map<String, Object>> datos = sectorService.exportarSectores(soloActivos);
            
            // Aquí implementarías la lógica de exportación a CSV/Excel
            // byte[] archivo = generarArchivo(datos, formato);
            
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=sectores." + formato)
                .body("Datos de exportación"); // En realidad sería el archivo
                
        } catch (Exception e) {
            log.error("Error al exportar sectores: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al exportar sectores");
        }
    }
    @GetMapping("/api/listar-simple")
    @ResponseBody
    public ResponseEntity<List<SectorDTO>> listarSectoresSimple() {
        try {
            List<Sector> sectores = sectorService.listarActivos();
            List<SectorDTO> sectoresDTO = sectores.stream()
                .map(sectorService::convertirADTO) // Sin estadísticas
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(sectoresDTO);
        } catch (Exception e) {
            log.error("Error al listar sectores: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }    
}