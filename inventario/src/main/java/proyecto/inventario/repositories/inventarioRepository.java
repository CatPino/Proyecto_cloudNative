package proyecto.inventario.repositories;

import proyecto.inventario.entities.inventario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
@Repository
public interface inventarioRepository extends JpaRepository<inventario, Long> {
}
 