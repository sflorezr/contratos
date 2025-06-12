package co.empresa.gestioncontratos.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.empresa.gestioncontratos.entity.Usuario;
import co.empresa.gestioncontratos.enums.PerfilUsuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Métodos para autenticación
    Optional<Usuario> findByUsernameAndActivoTrue(String username);
    Optional<Usuario> findByEmailAndActivoTrue(String email);
    
    // Búsquedas por UUID
    Optional<Usuario> findByUuid(UUID uuid);
    Optional<Usuario> findByUuidAndActivoTrue(UUID uuid);
    
    // Búsquedas por perfil
    List<Usuario> findByPerfilAndActivoTrue(PerfilUsuario perfil);

    Page<Usuario> findByPerfilAndActivoTrue(PerfilUsuario perfil, Pageable pageable);
    
    // Búsquedas con filtros
    @Query("SELECT u FROM Usuario u WHERE u.activo = true AND " +
           "(:perfil IS NULL OR u.perfil = :perfil) AND " +
           "(LOWER(u.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(u.apellido) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :filtro, '%')))")
    Page<Usuario> findUsuariosConFiltros(@Param("perfil") PerfilUsuario perfil, 
                                        @Param("filtro") String filtro, 
                                        Pageable pageable);
    
    // Verificar existencia
    boolean existsByUsernameAndActivoTrue(String username);
    boolean existsByEmailAndActivoTrue(String email);
    boolean existsByUsernameAndIdNotAndActivoTrue(String username, Long id);
    boolean existsByEmailAndIdNotAndActivoTrue(String email, Long id);
    
    // Contadores
    long countByPerfilAndActivoTrue(PerfilUsuario perfil);
    long countByActivoTrue();
    
    // Supervisores para asignación
    @Query("SELECT u FROM Usuario u WHERE u.perfil = 'SUPERVISOR' AND u.activo = true ORDER BY u.nombre, u.apellido")
    List<Usuario> findSupervisoresActivos();
    
    // Coordinadores para asignación
    @Query("SELECT u FROM Usuario u WHERE u.perfil = 'COORDINADOR' AND u.activo = true ORDER BY u.nombre, u.apellido")
    List<Usuario> findCoordinadoresActivos();
    
    // Operarios para asignación
    @Query("SELECT u FROM Usuario u WHERE u.perfil = 'OPERARIO' AND u.activo = true ORDER BY u.nombre, u.apellido")
    List<Usuario> findOperariosActivos();

    @Query("SELECT u FROM Usuario u WHERE u.perfil = :perfil AND u.activo = true " +
        "ORDER BY u.nombre ASC")
    Page<Usuario> findByPerfilAndActivoPage(@Param("perfil") PerfilUsuario perfil, Pageable pageable);

    @Query("SELECT u FROM Usuario u WHERE u.activo = true " +
        "ORDER BY u.nombre ASC")
    List<Usuario> findByActivoTrueOrderByNombreCompleto();
    
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByUsernameAndIdNot(String username, Long id);
}