package co.empresa.gestioncontratos.config;

import co.empresa.gestioncontratos.entity.Usuario;
import co.empresa.gestioncontratos.enums.PerfilUsuario;
import co.empresa.gestioncontratos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultUsers();
    }

    private void initializeDefaultUsers() {
        log.info("=== INICIALIZANDO USUARIOS POR DEFECTO ===");
        
        // Crear usuario administrador por defecto
        createUserIfNotExists("admin", "admin123", "Administrador", "Sistema", 
                             "admin@empresa.com", "555-0001", PerfilUsuario.ADMINISTRADOR);
        
        // Crear usuario supervisor de prueba
        createUserIfNotExists("supervisor", "supervisor123", "Juan Carlos", "Supervisor", 
                             "supervisor@empresa.com", "555-0002", PerfilUsuario.SUPERVISOR);
        
        // Crear usuario coordinador de prueba
        createUserIfNotExists("coordinador", "coordinador123", "María Elena", "Coordinador", 
                             "coordinador@empresa.com", "555-0003", PerfilUsuario.COORDINADOR);
        
        // Crear usuario operario de prueba
        createUserIfNotExists("operario", "operario123", "Pedro José", "Operario", 
                             "operario@empresa.com", "555-0004", PerfilUsuario.OPERARIO);

        log.info("=== INICIALIZACIÓN DE USUARIOS COMPLETADA ===");
        
        // Mostrar información de acceso
        log.info("");
        log.info("👤 USUARIOS DISPONIBLES:");
        log.info("   admin / admin123 (Administrador)");
        log.info("   supervisor / supervisor123 (Supervisor)");
        log.info("   coordinador / coordinador123 (Coordinador)");
        log.info("   operario / operario123 (Operario)");
        log.info("");
    }

    private void createUserIfNotExists(String username, String password, String nombre, String apellido, 
                                     String email, String telefono, PerfilUsuario perfil) {
        
        if (!usuarioRepository.existsByUsernameAndActivoTrue(username)) {
            String hashedPassword = passwordEncoder.encode(password);
            
            Usuario usuario = Usuario.builder()
                    .username(username)
                    .password(hashedPassword)
                    .nombre(nombre)
                    .apellido(apellido)
                    .email(email)
                    .telefono(telefono)
                    .perfil(perfil)
                    .activo(true)
                    .build();
            
            usuarioRepository.save(usuario);
            
            log.info("✅ Usuario creado: {} ({}) - Hash: {}", 
                    username, perfil.getDescripcion(), hashedPassword.substring(0, 20) + "...");
            
            // Verificar que la contraseña funciona
            boolean matches = passwordEncoder.matches(password, hashedPassword);
            log.info("   Verificación de contraseña: {}", matches ? "✅ CORRECTA" : "❌ ERROR");
            
        } else {
            log.info("ℹ️  Usuario ya existe: {} ({})", username, perfil.getDescripcion());
            
            // Verificar contraseña del usuario existente
            Usuario existingUser = usuarioRepository.findByUsernameAndActivoTrue(username)
                    .orElse(null);
            if (existingUser != null) {
                boolean matches = passwordEncoder.matches(password, existingUser.getPassword());
                log.info("   Verificación de contraseña: {}", matches ? "✅ CORRECTA" : "❌ ERROR");
                
                if (!matches) {
                    log.warn("⚠️  CONTRASEÑA INCORRECTA PARA {}, ACTUALIZANDO...", username);
                    existingUser.setPassword(passwordEncoder.encode(password));
                    usuarioRepository.save(existingUser);
                    log.info("✅ Contraseña actualizada para: {}", username);
                }
            }
        }
    }
}