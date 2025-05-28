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

import java.util.UUID;

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

    public Usuario crear(UsuarioDTO usuarioDTO) {
        log.info("=== INICIANDO CREACIÓN DE USUARIO ===");
        log.info("Datos recibidos: {}", usuarioDTO);
        
        try {
            // Limpiar espacios en blanco
            usuarioDTO.trim();
            
            log.info("Validando unicidad de username: {}", usuarioDTO.getUsername());
            // Validar que no exista el username
            if (usuarioRepository.existsByUsernameAndActivoTrue(usuarioDTO.getUsername())) {
                log.error("Username ya existe: {}", usuarioDTO.getUsername());
                throw new RuntimeException("Ya existe un usuario con el username: " + usuarioDTO.getUsername());
            }

            log.info("Validando unicidad de email: {}", usuarioDTO.getEmail());
            // Validar que no exista el email
            if (usuarioRepository.existsByEmailAndActivoTrue(usuarioDTO.getEmail())) {
                log.error("Email ya existe: {}", usuarioDTO.getEmail());
                throw new RuntimeException("Ya existe un usuario con el email: " + usuarioDTO.getEmail());
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
                    .activo(true)
                    .build();

            log.info("Usuario construido: {}", usuario);
            log.info("Guardando en base de datos...");
            
            Usuario usuarioGuardado = usuarioRepository.save(usuario);
            
            log.info("✅ Usuario guardado con ID: {}", usuarioGuardado.getId());
            log.info("✅ Usuario guardado con UUID: {}", usuarioGuardado.getUuid());
            log.info("✅ Usuario creado: {} ({})", usuarioGuardado.getUsername(), usuarioGuardado.getPerfil());
            
            // Verificar que se guardó correctamente
            Usuario verificacion = usuarioRepository.findById(usuarioGuardado.getId()).orElse(null);
            if (verificacion != null) {
                log.info("✅ VERIFICACIÓN: Usuario encontrado en BD con ID: {}, Username: {}", 
                        verificacion.getId(), verificacion.getUsername());
            } else {
                log.error("❌ VERIFICACIÓN: Usuario NO encontrado en BD después de guardar");
            }
            
            // Contar usuarios totales
            long totalUsuarios = usuarioRepository.count();
            log.info("📊 Total de usuarios en BD después de crear: {}", totalUsuarios);
            
            return usuarioGuardado;
            
        } catch (Exception e) {
            log.error("❌ ERROR al crear usuario: ", e);
            throw new RuntimeException("Error al crear usuario: " + e.getMessage(), e);
        }
    }

    public Usuario actualizar(UUID uuid, UsuarioDTO usuarioDTO) {
        log.info("=== ACTUALIZANDO USUARIO: {} ===", uuid);
        log.info("Nuevos datos: {}", usuarioDTO);
        
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
                .activo(usuario.getActivo())
                .build();
    }
}