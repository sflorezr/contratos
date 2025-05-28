package co.empresa.gestioncontratos;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@EnableJpaAuditing
@Slf4j
public class GestionContratosApplication {

    private final Environment environment;

    public GestionContratosApplication(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        System.setProperty("spring.devtools.livereload.enabled", "true");
        
        SpringApplication app = new SpringApplication(GestionContratosApplication.class);
        
        // Configurar propiedades adicionales si es necesario
        // app.setDefaultProperties(Collections.singletonMap("server.port", "8080"));
        
        log.info("=============================================================");
        log.info("Iniciando Sistema de Gestión de Contratos...");
        log.info("=============================================================");
        
        app.run(args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            String protocol = environment.getProperty("server.ssl.key-store") != null ? "https" : "http";
            String serverPort = environment.getProperty("server.port", "8080");
            String contextPath = environment.getProperty("server.servlet.context-path", "");
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            
            log.info("");
            log.info("=============================================================");
            log.info("  SISTEMA DE GESTIÓN DE CONTRATOS - INICIADO CORRECTAMENTE");
            log.info("=============================================================");
            log.info("");
            log.info("  🌐 Aplicación disponible en:");
            log.info("     Local:    {}://localhost:{}{}", protocol, serverPort, contextPath);
            log.info("     Externa:  {}://{}:{}{}", protocol, hostAddress, serverPort, contextPath);
            log.info("");
            log.info("  👤 Usuarios por defecto:");
            log.info("     Administrador: admin / admin123");
            log.info("     Supervisor:    supervisor / supervisor123");
            log.info("     Coordinador:   coordinador / coordinador123");
            log.info("     Operario:      operario / operario123");
            log.info("");
            log.info("  📊 Dashboard disponible:");
            log.info("     Admin:       {}/admin/dashboard", contextPath);
            log.info("     Supervisor:  {}/supervisor/dashboard", contextPath);
            log.info("     Coordinador: {}/coordinador/dashboard", contextPath);
            log.info("     Operario:    {}/operario/dashboard", contextPath);
            log.info("");
            log.info("  🔧 Perfiles activos: {}", String.join(", ", environment.getActiveProfiles()));
            log.info("  📁 Base de datos: {}", environment.getProperty("spring.datasource.url"));
            log.info("");
            log.info("=============================================================");
            log.info("  ✅ Sistema listo para usar!");
            log.info("=============================================================");
            log.info("");
            
        } catch (UnknownHostException e) {
            log.warn("No se pudo determinar la dirección IP del host: {}", e.getMessage());
            log.info("");
            log.info("=============================================================");
            log.info("  SISTEMA DE GESTIÓN DE CONTRATOS - INICIADO");
            log.info("=============================================================");
            log.info("  🌐 Aplicación disponible en: http://localhost:{}", 
                    environment.getProperty("server.port", "8080"));
            log.info("=============================================================");
        }
    }
}