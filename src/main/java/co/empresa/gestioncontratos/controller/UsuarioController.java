package co.empresa.gestioncontratos.controller;

import co.empresa.gestioncontratos.dto.UsuarioDTO;
import co.empresa.gestioncontratos.entity.Usuario;
import co.empresa.gestioncontratos.enums.PerfilUsuario;
import co.empresa.gestioncontratos.repository.UsuarioRepository;
import co.empresa.gestioncontratos.service.UsuarioService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor
@Slf4j
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    // ==================== PÁGINAS WEB ====================
    
    @GetMapping
    public String listar(@AuthenticationPrincipal Usuario usuarioActual,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String filtro,
                        @RequestParam(required = false) PerfilUsuario perfil,
                        Model model) {
        
        log.info("=== ACCEDIENDO A LISTA DE USUARIOS ===");
        log.info("Usuario actual: {}", usuarioActual != null ? usuarioActual.getUsername() : "null");
        log.info("Parámetros - page: {}, size: {}, filtro: {}, perfil: {}", page, size, filtro, perfil);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("fechaCreacion").descending());
            Page<Usuario> usuarios;
            
            if (filtro != null && !filtro.trim().isEmpty() || perfil != null) {
                usuarios = usuarioService.buscarConFiltros(perfil, filtro, pageable);
            } else {
                usuarios = usuarioService.listarTodos(pageable);
            }
            
            log.info("Usuarios encontrados: {}", usuarios.getTotalElements());
            
            model.addAttribute("usuarios", usuarios);
            model.addAttribute("usuarioActual", usuarioActual);
            model.addAttribute("perfiles", PerfilUsuario.values());
            model.addAttribute("filtroActual", filtro);
            model.addAttribute("perfilActual", perfil);
            
            // Estadísticas
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", usuarioService.contarTodos());
            stats.put("activos", usuarioService.contarActivos());
            stats.put("administradores", usuarioService.contarPorPerfil(PerfilUsuario.ADMINISTRADOR));
            stats.put("supervisores", usuarioService.contarPorPerfil(PerfilUsuario.SUPERVISOR));
            stats.put("coordinadores", usuarioService.contarPorPerfil(PerfilUsuario.COORDINADOR));
            stats.put("operarios", usuarioService.contarPorPerfil(PerfilUsuario.OPERARIO));
            
            model.addAttribute("stats", stats);
            
            log.info("✅ Cargando template: admin/usuarios");
            return "admin/usuarios";
            
        } catch (Exception e) {
            log.error("❌ Error al listar usuarios: ", e);
            model.addAttribute("error", "Error al cargar la lista de usuarios");
            return "admin/usuarios";
        }
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        log.info("=== MOSTRANDO FORMULARIO NUEVO USUARIO ===");
        
        model.addAttribute("usuario", new UsuarioDTO());
        model.addAttribute("perfiles", PerfilUsuario.values());
        model.addAttribute("esNuevo", true);
        
        return "admin/usuario-form";
    }

    @PostMapping("/nuevo")
    public String crear(@Valid @ModelAttribute UsuarioDTO usuarioDTO,
                       BindingResult result,
                       @RequestParam(value = "foto", required = false) MultipartFile fotoFile,
                       RedirectAttributes redirectAttributes,
                       Model model) {
        
        log.info("=== RECIBIENDO PETICIÓN CREAR USUARIO ===");
        log.info("UsuarioDTO recibido: {}", usuarioDTO);
        log.info("Archivo de foto recibido: {}", fotoFile != null ? fotoFile.getOriginalFilename() : "ninguno");
        log.info("Errores de validación: {}", result.getAllErrors());
        
        try {
            if (result.hasErrors()) {
                log.warn("Errores de validación encontrados:");
                result.getAllErrors().forEach(error -> log.warn("  - {}", error.getDefaultMessage()));
                
                model.addAttribute("perfiles", PerfilUsuario.values());
                model.addAttribute("esNuevo", true);
                return "admin/usuario-form";
            }
            
            // Validar contraseñas
            if (usuarioDTO.getPassword() != null && usuarioDTO.getConfirmPassword() != null &&
                !usuarioDTO.getPassword().equals(usuarioDTO.getConfirmPassword())) {
                log.warn("Las contraseñas no coinciden");
                result.rejectValue("confirmPassword", "error.password", "Las contraseñas no coinciden");
                model.addAttribute("perfiles", PerfilUsuario.values());
                model.addAttribute("esNuevo", true);
                return "admin/usuario-form";
            }
            
            log.info("Validaciones pasadas, creando usuario...");
            Usuario usuario = usuarioService.crear(usuarioDTO, fotoFile);
            
            log.info("✅ Usuario creado exitosamente: {}", usuario.getUsername());
            redirectAttributes.addFlashAttribute("success", 
                "Usuario '" + usuario.getUsername() + "' creado exitosamente");
            
            return "redirect:/admin/usuarios";
            
        } catch (Exception e) {
            log.error("❌ ERROR en controlador al crear usuario: ", e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al crear usuario: " + e.getMessage());
            return "redirect:/admin/usuarios";
        }
    }

    @GetMapping("/{uuid}")
    public String ver(@PathVariable UUID uuid, Model model) {
        log.info("=== VIENDO USUARIO: {} ===", uuid);
        
        try {
            Usuario usuario = usuarioService.buscarPorUuid(uuid);
            model.addAttribute("usuario", usuario);
            return "admin/usuario-detalle";
        } catch (Exception e) {
            log.error("Error al buscar usuario: ", e);
            return "redirect:/admin/usuarios";
        }
    }

    @GetMapping("/{uuid}/editar")
    public String mostrarFormularioEditar(@PathVariable UUID uuid
                                          , Model model) {
        log.info("=== EDITANDO USUARIO: {} ===", uuid);
        try {
            Usuario usuario = usuarioService.buscarPorUuid(uuid);
            UsuarioDTO usuarioDTO = usuarioService.convertirADTO(usuario);
            
            model.addAttribute("usuario", usuarioDTO);
            model.addAttribute("perfiles", PerfilUsuario.values());
            model.addAttribute("esNuevo", false);
            
            return "admin/usuario-form";
        } catch (Exception e) {
            log.error("Error al buscar usuario para editar: ", e);
            return "redirect:/admin/usuarios";
        }
    }

    @PostMapping("/{uuid}/editar")
    public String actualizar(@PathVariable UUID uuid,
                           @Valid @ModelAttribute UsuarioDTO usuarioDTO,
                           BindingResult result,
                           @RequestParam(value = "foto", required = false) MultipartFile fotoFile,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        
        log.info("=== ACTUALIZANDO USUARIO: {} ===", uuid);
        log.info("Archivo de foto recibido: {}", fotoFile != null ? fotoFile.getOriginalFilename() : "ninguno");    
        try {
            if (result.hasErrors()) {
                model.addAttribute("perfiles", PerfilUsuario.values());
                model.addAttribute("esNuevo", false);
                //return "admin/usuario-form";
            }
            
            Usuario usuario = usuarioService.actualizar(uuid, usuarioDTO, fotoFile);
            redirectAttributes.addFlashAttribute("success", 
                "Usuario '" + usuario.getUsername() + "' actualizado exitosamente");
            
            return "redirect:/admin/usuarios";
            
        } catch (Exception e) {
            log.error("Error al actualizar usuario: ", e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al actualizar usuario: " + e.getMessage());
            return "redirect:/admin/usuarios";
        }
    }

    // ==================== API REST ====================

    @PostMapping("/{uuid}/cambiar-estado")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cambiarEstado(@PathVariable UUID uuid) {
        log.info("=== CAMBIANDO ESTADO USUARIO: {} ===", uuid);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Usuario usuario = usuarioService.cambiarEstado(uuid);
            response.put("success", true);
            response.put("message", "Estado del usuario cambiado exitosamente");
            response.put("nuevoEstado", usuario.getActivo());
            
            log.info("✅ Estado cambiado a: {}", usuario.getActivo());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error al cambiar estado del usuario: ", e);
            response.put("success", false);
            response.put("message", "Error al cambiar estado: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{uuid}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminar(@PathVariable UUID uuid) {
        log.info("=== ELIMINANDO USUARIO: {} ===", uuid);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            usuarioService.eliminar(uuid);
            response.put("success", true);
            response.put("message", "Usuario eliminado exitosamente");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al eliminar usuario: ", e);
            response.put("success", false);
            response.put("message", "Error al eliminar usuario: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    // API REST para búsquedas AJAX
    @GetMapping("/api/buscar")
    @ResponseBody
    public ResponseEntity<Page<Usuario>> buscarUsuarios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String filtro,
            @RequestParam(required = false) PerfilUsuario perfil) {
        
        log.info("=== API BUSCAR USUARIOS ===");
        log.info("Parámetros - page: {}, size: {}, filtro: {}, perfil: {}", page, size, filtro, perfil);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("fechaCreacion").descending());
            Page<Usuario> usuarios;
            
            if (filtro != null && !filtro.trim().isEmpty() || perfil != null) {
                usuarios = usuarioService.buscarConFiltros(perfil, filtro, pageable);
            } else {
                usuarios = usuarioService.listarTodos(pageable);
            }
            
            log.info("✅ Usuarios encontrados via API: {}", usuarios.getTotalElements());
            return ResponseEntity.ok(usuarios);
            
        } catch (Exception e) {
            log.error("❌ Error en búsqueda de usuarios: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/validar-username")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validarUsername(
            @RequestParam String username,
            @RequestParam(required = false) UUID uuid) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean existe = usuarioService.existeUsername(username, uuid);
            response.put("disponible", !existe);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al validar username: ", e);
            response.put("error", "Error al validar username");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/validar-email")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validarEmail(
            @RequestParam String email,
            @RequestParam(required = false) UUID uuid) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean existe = usuarioService.existeEmail(email, uuid);
            response.put("disponible", !existe);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al validar email: ", e);
            response.put("error", "Error al validar email");
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== MÉTODOS DE PRUEBA ====================

    @GetMapping("/test")
    @ResponseBody
    public String test() {
        log.info("=== TEST CONTROLLER ===");
        return "UsuarioController funcionando correctamente!";
    }

    @GetMapping("/debug")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> debug() {
        log.info("=== DEBUG USUARIOS ===");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            long totalUsuarios = usuarioService.contarTodos();
            long usuariosActivos = usuarioService.contarActivos();
            
            response.put("totalUsuarios", totalUsuarios);
            response.put("usuariosActivos", usuariosActivos);
            response.put("timestamp", java.time.LocalDateTime.now());
            response.put("controllerStatus", "FUNCIONANDO");
            
            log.info("✅ Debug info: {} usuarios totales, {} activos", totalUsuarios, usuariosActivos);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error en debug: ", e);
            response.put("error", e.getMessage());
            response.put("controllerStatus", "ERROR");
            return ResponseEntity.badRequest().body(response);
        }
    }
    @GetMapping("/api/supervisores")
    @ResponseBody
    public ResponseEntity<List<UsuarioDTO>> listarSupervisores() {
        log.info("=== API: LISTANDO SUPERVISORES ===");
        
        try {
            List<Usuario> supervisores = usuarioRepository.findByPerfilAndActivoTrue(PerfilUsuario.SUPERVISOR);
            List<UsuarioDTO> supervisoresDTO = supervisores.stream()
                .map(usuarioService::convertirADTO)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(supervisoresDTO);
        } catch (Exception e) {
            log.error("Error al listar supervisores: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}