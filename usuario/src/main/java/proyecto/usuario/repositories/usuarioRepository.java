package proyecto.usuario.repositories;

import proyecto.usuario.entities.usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
@Repository
public interface usuarioRepository extends JpaRepository<usuario, Long> {
}
 