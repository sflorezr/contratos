package co.empresa.gestioncontratos.controller;

import co.empresa.gestioncontratos.dto.ServicioDTO;
import co.empresa.gestioncontratos.entity.Contrato;
import co.empresa.gestioncontratos.entity.Servicio;
import co.empresa.gestioncontratos.entity.Usuario;
import co.empresa.gestioncontratos.service.ServicioService;
import co.empresa.gestioncontratos.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final ServicioService servicioService;


    @GetMapping
    public String listar(@AuthenticationPrincipal Usuario usuarioActual, Model model) {
        
        List<Servicio> servicios;
        
        switch (usuarioActual.getPerfil()) {
            case ADMINISTRADOR:
                servicios = servicioService.listarActivos();
                break;
            case SUPERVISOR:
                servicios = servicioService.listarActivos();
                break;
            case COORDINADOR:
                servicios = servicioService.listarActivos();
                break;
            case OPERARIO:
                // Los operarios ven contratos donde tienen predios asignados
                servicios = servicioService.listarActivos();
                break;
            default:
                servicios = new ArrayList<>();
        }
        
        model.addAttribute("servicios", servicioService.listarActivos());
        model.addAttribute("usuarioActual", usuarioActual);
        
        return "admin/servicios";
    }



    @GetMapping("/listar")
    @ResponseBody
    public List<Servicio> listarTodos() {
        return servicioService.listarTodos();
    }

    @GetMapping("/listar/paginado")
    @ResponseBody
    public Page<Servicio> listarPaginado(Pageable pageable) {
        return servicioService.listarTodosPaginado(pageable);
    }

    @GetMapping("/activos")
    @ResponseBody
    public List<Servicio> listarActivos() {
        return servicioService.listarActivos();
    }

    @GetMapping("/resumen")
    @ResponseBody
    public List<Map<String, Object>> obtenerResumen() {
        return servicioService.listarServiciosResumen();
    }

    @GetMapping("/estadisticas")
    @ResponseBody
    public Map<String, Object> obtenerEstadisticas() {
        return servicioService.obtenerEstadisticas();
    }

    @GetMapping("/{uuid}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerDetalles(@PathVariable UUID uuid) {
        return ResponseEntity.ok(servicioService.obtenerDetallesServicio(uuid));
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<Servicio> crear(@RequestBody ServicioDTO servicioDTO) {
        return ResponseEntity.ok(servicioService.crear(servicioDTO));
    }

    @PutMapping("/{uuid}")
    @ResponseBody
    public ResponseEntity<Servicio> actualizar(@PathVariable UUID uuid, @RequestBody ServicioDTO servicioDTO) {
        return ResponseEntity.ok(servicioService.actualizar(uuid, servicioDTO));
    }

    @PutMapping("/{uuid}/estado")
    @ResponseBody
    public ResponseEntity<Servicio> cambiarEstado(@PathVariable UUID uuid, @RequestParam boolean activo) {
        return ResponseEntity.ok(servicioService.cambiarEstado(uuid, activo));
    }

    @DeleteMapping("/{uuid}")
    @ResponseBody
    public ResponseEntity<Void> eliminar(@PathVariable UUID uuid) {
        servicioService.eliminar(uuid);
        return ResponseEntity.ok().build();
    }
}