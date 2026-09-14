package compra.compra.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import compra.compra.entities.compra;
 
@Repository
public interface compraRepository extends JpaRepository<compra, Long> {
}
 