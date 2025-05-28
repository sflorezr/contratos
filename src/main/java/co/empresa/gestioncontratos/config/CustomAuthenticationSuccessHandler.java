package co.empresa.gestioncontratos.config;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import co.empresa.gestioncontratos.entity.Usuario;
import co.empresa.gestioncontratos.enums.PerfilUsuario;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        Usuario usuario = (Usuario) authentication.getPrincipal();
        String redirectUrl = determineTargetUrl(usuario.getPerfil());
        
        // Almacenar información del usuario en la sesión
        request.getSession().setAttribute("usuarioLogueado", usuario);
        request.getSession().setAttribute("nombreCompleto", usuario.getNombreCompleto());
        request.getSession().setAttribute("perfil", usuario.getPerfil().getDescripcion());
        
        response.sendRedirect(redirectUrl);
    }

    private String determineTargetUrl(PerfilUsuario perfil) {
        return switch (perfil) {
            case ADMINISTRADOR -> "/admin/dashboard";
            case SUPERVISOR -> "/supervisor/dashboard";
            case COORDINADOR -> "/coordinador/dashboard";
            case OPERARIO -> "/operario/dashboard";
        };
    }
}