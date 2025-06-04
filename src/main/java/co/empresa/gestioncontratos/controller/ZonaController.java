package co.empresa.gestioncontratos.controller;

import co.empresa.gestioncontratos.dto.ZonaDTO;
import co.empresa.gestioncontratos.dto.SectorDTO;
import co.empresa.gestioncontratos.entity.Zona;
import co.empresa.gestioncontratos.entity.Usuario;
import co.empresa.gestioncontratos.entity.Contrato;
import co.empresa.gestioncontratos.entity.Sector;
import co.empresa.gestioncontratos.enums.PerfilUsuario;
import co.empresa.gestioncontratos.service.ZonaService;
import co.empresa.gestioncontratos.service.ContratoService;
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
@RequestMapping("/admin/zonas")
@RequiredArgsConstructor
@Slf4j
public class ZonaController {

    private final ZonaService zonaService;
    private final ContratoService contratoService;
    private final SectorService sectorService;

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
            
            // Cargar contratos para el filtro
            model.addAttribute("contratos", contratoService.listarTodos());
            
            // Estadísticas
            Map<String, Long> stats = zonaService.obtenerResumenGeneral();
            model.addAttribute("stats", stats);
            
            return "admin/zonas";
            
        } catch (Exception e) {
            log.error("Error al listar zonas: ", e);
            model.addAttribute("error", "Error al cargar la lista de zonas");
            return "admin/zonas";
        }
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(@RequestParam(required = false) Long contratoId,
                                        Model model) {
        log.info("=== MOSTRANDO FORMULARIO NUEVA ZONA ===");
        
        ZonaDTO zonaDTO = new ZonaDTO();
        
        model.addAttribute("zona", zonaDTO);
        model.addAttribute("esNuevo", true);
        model.addAttribute("contratos", contratoService.listarActivos());
        
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
                log.warn("Errores de validación encontrados");
                model.addAttribute("esNuevo", true);
              //  model.addAttribute("contratos", contratoService.listarActivos());
                return "admin/zona-form";
            }
            
            ZonaDTO zona = zonaService.crear(zonaDTO);
            log.info("✅ Zona creada exitosamente: {}", zona.getNombre());
            
            redirectAttributes.addFlashAttribute("success", 
                "Zona '" + zona.getNombre() + "' creada exitosamente");
            
            return "redirect:/admin/zonas";
            
        } catch (Exception e) {
            log.error("❌ Error al crear zona: ", e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al crear zona: " + e.getMessage());
            return "redirect:/admin/zonas";
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
            
            model.addAttribute("zona", zona);
            model.addAttribute("estadisticas", estadisticas);
            model.addAttribute("sectores", sectores);
            model.addAttribute("puedeEditar", puedeGestionarZonas(usuarioActual));
            
            return "admin/zona-detalle";
            
        } catch (Exception e) {
            log.error("Error al buscar zona: ", e);
            return "redirect:/admin/zonas";
        }
    }

    @GetMapping("/{uuid}/editar")
    public String mostrarFormularioEditar(@PathVariable UUID uuid, 
                                         @AuthenticationPrincipal Usuario usuarioActual,
                                         Model model) {
        log.info("=== EDITANDO ZONA: {} ===", uuid);
        
        if (!puedeGestionarZonas(usuarioActual)) {
            return "redirect:/admin/zonas";
        }
        
        try {
            ZonaDTO zona = zonaService.buscarPorUuid(uuid);
            
            model.addAttribute("zona", zona);
            model.addAttribute("esNuevo", false);
            model.addAttribute("contratos", contratoService.listarActivos());
            
            return "admin/zona-form";
            
        } catch (Exception e) {
            log.error("Error al buscar zona para editar: ", e);
            return "redirect:/admin/zonas";
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
                model.addAttribute("esNuevo", false);
                model.addAttribute("contratos", contratoService.listarActivos());
                return "admin/zona-form";
            }
            
            ZonaDTO zona = zonaService.actualizar(uuid, zonaDTO);
            redirectAttributes.addFlashAttribute("success", 
                "Zona '" + zona.getNombre() + "' actualizada exitosamente");
            
            return "redirect:/admin/zonas";
            
        } catch (Exception e) {
            log.error("Error al actualizar zona: ", e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al actualizar zona: " + e.getMessage());
            return "redirect:/admin/zonas";
        }
    }

    // ==================== API REST ====================

    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<List<ZonaDTO>> listarTodos() {
        log.info("=== API: LISTANDO TODAS LAS ZONAS ===");
        
        try {
            List<ZonaDTO> zonasDTO = zonaService.listarTodasConEstadisticas();
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

    @GetMapping("/api/contrato/{contratoId}")
    @ResponseBody
    public ResponseEntity<List<ZonaDTO>> listarPorContrato(@PathVariable Long contratoId) {
        log.info("=== API: LISTANDO ZONAS DEL CONTRATO {} ===", contratoId);
        
        try {
            List<ZonaDTO> zonas = zonaService.listarPorContrato(contratoId);
            return ResponseEntity.ok(zonas);
        } catch (Exception e) {
            log.error("Error al listar zonas del contrato: ", e);
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
                                          @RequestBody Map<String, Boolean> estado,
                                          @AuthenticationPrincipal Usuario usuarioActual) {
        log.info("=== CAMBIANDO ESTADO ZONA: {} ===", uuid);
        
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
        log.info("=== ELIMINANDO ZONA: {} ===", uuid);
        
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

    // ==================== ENDPOINTS ESPECÍFICOS ====================

    @GetMapping("/api/estadisticas")
    @ResponseBody
    public ResponseEntity<?> obtenerEstadisticasGenerales() {
        log.info("=== API: OBTENIENDO ESTADÍSTICAS GENERALES ===");
        
        try {
            Map<String, Long> estadisticas = zonaService.obtenerResumenGeneral();
            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            log.error("Error al obtener estadísticas: ", e);
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

    @GetMapping("/api/select")
    @ResponseBody
    public ResponseEntity<List<ZonaDTO>> listarZonasSimple() {
        try {
            List<ZonaDTO> zonas = zonaService.listarActivas();
 
            return ResponseEntity.ok(zonas);
        } catch (Exception e) {
            log.error("Error al listar sectores: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
}