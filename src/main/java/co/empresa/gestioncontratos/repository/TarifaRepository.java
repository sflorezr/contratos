package co.empresa.gestioncontratos.repository;

import co.empresa.gestioncontratos.entity.Servicio;
import co.empresa.gestioncontratos.entity.Tarifa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarifaRepository extends JpaRepository<Tarifa, Long> {
    
    List<Tarifa> findByServicioOrderByPrecioUrbanoAsc(Servicio servicio);
   // boolean existsByNombre(String nombre);    
    long countByServicio(Servicio servicio);

    
}