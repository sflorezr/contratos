package co.empresa.gestioncontratos.controller;


import co.empresa.gestioncontratos.entity.Usuario;
import co.empresa.gestioncontratos.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin/dashboard")
    public String adminDashboard(@AuthenticationPrincipal Usuario usuario, Model model) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("stats", dashboardService.getAdminStats());
        return "dashboard/admin";
    }

    @GetMapping("/supervisor/dashboard")
    public String supervisorDashboard(@AuthenticationPrincipal Usuario usuario, Model model) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("stats", dashboardService.getSupervisorStats(usuario.getId()));
        return "dashboard/supervisor";
    }

    @GetMapping("/coordinador/dashboard")
    public String coordinadorDashboard(@AuthenticationPrincipal Usuario usuario, Model model) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("stats", dashboardService.getCoordinadorStats(usuario.getId()));
        return "dashboard/coordinador";
    }

    @GetMapping("/operario/dashboard")
    public String operarioDashboard(@AuthenticationPrincipal Usuario usuario, Model model) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("stats", dashboardService.getOperarioStats(usuario.getId()));
        return "dashboard/operario";
    }
}