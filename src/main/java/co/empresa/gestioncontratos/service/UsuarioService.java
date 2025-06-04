package co.empresa.gestioncontratos.service;

import co.empresa.gestioncontratos.dto.UsuarioDTO;
import co.empresa.gestioncontratos.entity.Usuario;
import co.empresa.gestioncontratos.enums.PerfilUsuario;
import co.empresa.gestioncontratos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import java.nio.file.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // ==================== CONSULTAS ====================

    @Transactional(readOnly = true)
    public Page<Usuario> listarTodos(Pageable pageable) {
        log.info("=== LISTANDO TODOS LOS USUARIOS ===");
        try {
            Page<Usuario> usuarios = usuarioRepository.findAll(pageable);
            log.info("✅ Usuarios encontrados: {} de {}", usuarios.getNumberOfElements(), usuarios.getTotalElements());
            return usuarios;
        } catch (Exception e) {
            log.error("❌ Error al listar usuarios: ", e);
            throw new RuntimeException("Error al obtener lista de usuarios", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<Usuario> buscarConFiltros(PerfilUsuario perfil, String filtro, Pageable pageable) {
        log.info("=== BUSCANDO USUARIOS CON FILTROS ===");
        log.info("Perfil: {}, Filtro: {}", perfil, filtro);
        
        try {
            Page<Usuario> usuarios = usuarioRepository.findUsuariosConFiltros(perfil, 
                    filtro != null ? filtro.trim() : null, pageable);
            log.info("✅ Usuarios encontrados con filtros: {}", usuarios.getTotalElements());
            return usuarios;
        } catch (Exception e) {
            log.error("❌ Error al buscar usuarios con filtros: ", e);
            throw new RuntimeException("Error en búsqueda de usuarios", e);
        }
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorUuid(UUID uuid) {
        log.info("=== BUSCANDO USUARIO POR UUID: {} ===", uuid);
        
        try {
            Usuario usuario = usuarioRepository.findByUuid(uuid)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con UUID: " + uuid));
            log.info("✅ Usuario encontrado: {}", usuario.getUsername());
            return usuario;
        } catch (Exception e) {
            log.error("❌ Error al buscar usuario por UUID: ", e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorUsername(String username) {
        log.info("=== BUSCANDO USUARIO POR USERNAME: {} ===", username);
        
        try {
            Usuario usuario = usuarioRepository.findByUsernameAndActivoTrue(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));
            log.info("✅ Usuario encontrado: {}", usuario.getUsername());
            return usuario;
        } catch (Exception e) {
            log.error("❌ Error al buscar usuario por username: ", e);
            throw e;
        }
    }

    // ==================== OPERACIONES CRUD ====================

   public Usuario crear(UsuarioDTO usuarioDTO, MultipartFile fotoFile) {
        log.info("=== INICIANDO CREACIÓN DE USUARIO ===");
        log.info("Datos recibidos: {}", usuarioDTO);
        log.info("Foto recibida: {}", fotoFile != null ? fotoFile.getOriginalFilename() : "ninguna");
        
        try {
            // Limpiar espacios en blanco
            usuarioDTO.trim();
            
            log.info("Validando unicidad de username: {}", usuarioDTO.getUsername());
            if (usuarioRepository.existsByUsernameAndActivoTrue(usuarioDTO.getUsername())) {
                log.error("Username ya existe: {}", usuarioDTO.getUsername());
                throw new RuntimeException("Ya existe un usuario con el username: " + usuarioDTO.getUsername());
            }

            log.info("Validando unicidad de email: {}", usuarioDTO.getEmail());
            if (usuarioRepository.existsByEmailAndActivoTrue(usuarioDTO.getEmail())) {
                log.error("Email ya existe: {}", usuarioDTO.getEmail());
                throw new RuntimeException("Ya existe un usuario con el email: " + usuarioDTO.getEmail());
            }

            // Procesar foto si se proporciona
            String fotoBase64 = null;
            if (fotoFile != null && !fotoFile.isEmpty()) {
                fotoBase64 = procesarFoto(fotoFile);
                log.info("Foto procesada, tamaño: {} bytes", fotoBase64 != null ? fotoBase64.length() : 0);
            }

            // Hashear contraseña
            String hashedPassword = passwordEncoder.encode(usuarioDTO.getPassword());
            log.info("Contraseña hasheada: {}", hashedPassword.substring(0, 20) + "...");

            Usuario usuario = Usuario.builder()
                    .username(usuarioDTO.getUsername())
                    .password(hashedPassword)
                    .nombre(usuarioDTO.getNombre())
                    .apellido(usuarioDTO.getApellido())
                    .email(usuarioDTO.getEmail())
                    .telefono(usuarioDTO.getTelefono())
                    .perfil(usuarioDTO.getPerfil())
                    .foto(fotoBase64)
                    .activo(true)
                    .build();

            log.info("Usuario construido: {}", usuario);
            log.info("Guardando en base de datos...");
            
            Usuario usuarioGuardado = usuarioRepository.save(usuario);
            
            log.info("✅ Usuario guardado con ID: {}", usuarioGuardado.getId());
            log.info("✅ Usuario guardado con UUID: {}", usuarioGuardado.getUuid());
            log.info("✅ Usuario creado: {} ({})", usuarioGuardado.getUsername(), usuarioGuardado.getPerfil());
            
            return usuarioGuardado;
            
        } catch (Exception e) {
            log.error("❌ ERROR al crear usuario: ", e);
            throw new RuntimeException("Error al crear usuario: " + e.getMessage(), e);
        }
    }
    private String procesarFoto(MultipartFile archivo) {
        try {
            // Validar que sea una imagen
            String tipoContenido = archivo.getContentType();
            if (tipoContenido == null || !tipoContenido.startsWith("image/")) {
                throw new RuntimeException("El archivo debe ser una imagen");
            }
            
            // Validar tamaño (máximo 5MB)
            if (archivo.getSize() > 5 * 1024 * 1024) {
                throw new RuntimeException("La imagen no puede superar los 5MB");
            }
            
            // Convertir a Base64
            byte[] bytes = archivo.getBytes();
            String base64 = "data:" + tipoContenido + ";base64," + 
                        java.util.Base64.getEncoder().encodeToString(bytes);
            
            log.info("Foto procesada: tipo={}, tamaño={} bytes", tipoContenido, bytes.length);
            return base64;
            
        } catch (Exception e) {
            log.error("Error al procesar foto: ", e);
            throw new RuntimeException("Error al procesar la foto: " + e.getMessage(), e);
        }
    }
    public Usuario actualizar(UUID uuid, UsuarioDTO usuarioDTO, MultipartFile fotoFile) {
        log.info("=== ACTUALIZANDO USUARIO: {} ===", uuid);
        log.info("Nuevos datos: {}", usuarioDTO);
        log.info("Foto recibida: {}", fotoFile != null ? fotoFile.getOriginalFilename() : "ninguna");
        
        try {
            Usuario usuario = buscarPorUuid(uuid);
            log.info("Usuario encontrado para actualizar: {}", usuario.getUsername());

            // Limpiar espacios
            usuarioDTO.trim();

            // Validar username único (excluyendo el usuario actual)
            if (usuarioRepository.existsByUsernameAndIdNotAndActivoTrue(usuarioDTO.getUsername(), usuario.getId())) {
                throw new RuntimeException("Ya existe otro usuario con el username: " + usuarioDTO.getUsername());
            }

            // Validar email único (excluyendo el usuario actual)
            if (usuarioRepository.existsByEmailAndIdNotAndActivoTrue(usuarioDTO.getEmail(), usuario.getId())) {
                throw new RuntimeException("Ya existe otro usuario con el email: " + usuarioDTO.getEmail());
            }

            // Procesar nueva foto si se proporciona
            if (fotoFile != null && !fotoFile.isEmpty()) {
                String fotoBase64 = procesarFoto(fotoFile);
                usuario.setFoto(fotoBase64);
                log.info("Foto actualizada, tamaño: {} bytes", fotoBase64 != null ? fotoBase64.length() : 0);
            }

            // Actualizar campos
            usuario.setUsername(usuarioDTO.getUsername());
            usuario.setNombre(usuarioDTO.getNombre());
            usuario.setApellido(usuarioDTO.getApellido());
            usuario.setEmail(usuarioDTO.getEmail());
            usuario.setTelefono(usuarioDTO.getTelefono());
            usuario.setPerfil(usuarioDTO.getPerfil());

            // Actualizar contraseña solo si se proporcionó una nueva
            if (usuarioDTO.getPassword() != null && !usuarioDTO.getPassword().trim().isEmpty()) {
                String hashedPassword = passwordEncoder.encode(usuarioDTO.getPassword());
                usuario.setPassword(hashedPassword);
                log.info("Contraseña actualizada");
            }

            Usuario usuarioActualizado = usuarioRepository.save(usuario);
            log.info("✅ Usuario actualizado: {} ({})", usuarioActualizado.getUsername(), usuarioActualizado.getPerfil());
            
            return usuarioActualizado;
            
        } catch (Exception e) {
            log.error("❌ Error al actualizar usuario: ", e);
            throw new RuntimeException("Error al actualizar usuario: " + e.getMessage(), e);
        }
    }

    public Usuario cambiarEstado(UUID uuid) {
        log.info("=== CAMBIANDO ESTADO USUARIO: {} ===", uuid);
        
        try {
            Usuario usuario = buscarPorUuid(uuid);
            boolean estadoAnterior = usuario.getActivo();
            
            usuario.setActivo(!usuario.getActivo());
            
            Usuario usuarioActualizado = usuarioRepository.save(usuario);
            log.info("✅ Estado del usuario {} cambiado de {} a {}", 
                    usuario.getUsername(), estadoAnterior, usuario.getActivo());
            
            return usuarioActualizado;
            
        } catch (Exception e) {
            log.error("❌ Error al cambiar estado: ", e);
            throw new RuntimeException("Error al cambiar estado del usuario", e);
        }
    }

    public void eliminar(UUID uuid) {
        log.info("=== ELIMINANDO USUARIO: {} ===", uuid);
        
        try {
            Usuario usuario = buscarPorUuid(uuid);
            
            // Verificar si el usuario puede ser eliminado
            // (aquí puedes agregar validaciones adicionales)
            
            usuarioRepository.delete(usuario);
            log.info("✅ Usuario eliminado: {}", usuario.getUsername());
            
        } catch (Exception e) {
            log.error("❌ Error al eliminar usuario: ", e);
            throw new RuntimeException("Error al eliminar usuario", e);
        }
    }

    // ==================== VALIDACIONES ====================

    @Transactional(readOnly = true)
    public boolean existeUsername(String username, UUID uuidExcluir) {
        log.debug("Validando username: {} (excluir UUID: {})", username, uuidExcluir);
        
        try {
            if (uuidExcluir != null) {
                Usuario usuarioExistente = usuarioRepository.findByUuid(uuidExcluir).orElse(null);
                if (usuarioExistente != null) {
                    return usuarioRepository.existsByUsernameAndIdNotAndActivoTrue(username, usuarioExistente.getId());
                }
            }
            boolean existe = usuarioRepository.existsByUsernameAndActivoTrue(username);
            log.debug("Username {} existe: {}", username, existe);
            return existe;
            
        } catch (Exception e) {
            log.error("Error al validar username: ", e);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean existeEmail(String email, UUID uuidExcluir) {
        log.debug("Validando email: {} (excluir UUID: {})", email, uuidExcluir);
        
        try {
            if (uuidExcluir != null) {
                Usuario usuarioExistente = usuarioRepository.findByUuid(uuidExcluir).orElse(null);
                if (usuarioExistente != null) {
                    return usuarioRepository.existsByEmailAndIdNotAndActivoTrue(email, usuarioExistente.getId());
                }
            }
            boolean existe = usuarioRepository.existsByEmailAndActivoTrue(email);
            log.debug("Email {} existe: {}", email, existe);
            return existe;
            
        } catch (Exception e) {
            log.error("Error al validar email: ", e);
            return false;
        }
    }

    // ==================== ESTADÍSTICAS ====================

    @Transactional(readOnly = true)
    public long contarTodos() {
        try {
            long total = usuarioRepository.count();
            log.debug("Total usuarios: {}", total);
            return total;
        } catch (Exception e) {
            log.error("Error al contar usuarios: ", e);
            return 0;
        }
    }

    @Transactional(readOnly = true)
    public long contarActivos() {
        try {
            long activos = usuarioRepository.countByActivoTrue();
            log.debug("Usuarios activos: {}", activos);
            return activos;
        } catch (Exception e) {
            log.error("Error al contar usuarios activos: ", e);
            return 0;
        }
    }

    @Transactional(readOnly = true)
    public long contarPorPerfil(PerfilUsuario perfil) {
        try {
            long count = usuarioRepository.countByPerfilAndActivoTrue(perfil);
            log.debug("Usuarios con perfil {}: {}", perfil, count);
            return count;
        } catch (Exception e) {
            log.error("Error al contar usuarios por perfil: ", e);
            return 0;
        }
    }

    // ==================== CONVERSIONES ====================

    public UsuarioDTO convertirADTO(Usuario usuario) {
        log.debug("Convirtiendo usuario a DTO: {}", usuario.getUsername());
        
        return UsuarioDTO.builder()
                .uuid(usuario.getUuid())
                .username(usuario.getUsername())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .email(usuario.getEmail())
                .telefono(usuario.getTelefono())
                .perfil(usuario.getPerfil())
                .foto(usuario.getFoto()) // Agregar esta línea
                .activo(usuario.getActivo())
                .build();
    }
    @Transactional(readOnly = true)
    public List<Usuario> findSupervisoresActivos() {
        log.info("Buscando supervisores activos");
        return usuarioRepository.findSupervisoresActivos();
    }
    @Transactional(readOnly = true)
    public List<Usuario> findCoordinadoresActivos() {
        log.info("Buscando coordinadores activos");
        return usuarioRepository.findCoordinadoresActivos();
    }    
    @Transactional(readOnly = true)
    public List<Usuario> findOperariosActivos() {
        log.info("Buscando operarios activos");
        return usuarioRepository.findOperariosActivos();
    }
}