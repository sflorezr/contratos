package co.empresa.gestioncontratos;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
        public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Generar hashes para todas las contraseñas
        System.out.println("Contraseñas hasheadas:");
        System.out.println("admin123: " + encoder.encode("admin123"));
        System.out.println("supervisor123: " + encoder.encode("supervisor123"));
        System.out.println("coordinador123: " + encoder.encode("coordinador123"));
        System.out.println("operario123: " + encoder.encode("operario123"));
        
        // Verificar que funciona
        String hash = encoder.encode("admin123");
        System.out.println("\nVerificación:");
        System.out.println("Hash generado: " + hash);
        System.out.println("Verificación exitosa: " + encoder.matches("admin123", hash));
    }
    
}
