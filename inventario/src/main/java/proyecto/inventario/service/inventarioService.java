package proyecto.inventario.service;


import proyecto.inventario.entities.inventario;
import proyecto.inventario.repositories.inventarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
 
import java.util.List;
 
@Service
@RequiredArgsConstructor
public class inventarioService {
 
    private final inventarioRepository inventarioRepository;
 
    public List<inventario> listarTodos() {
        return inventarioRepository.findAll();
    }
 
    public inventario buscarPorId(Long id) {
        return inventarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventario no encontrado con id: " + id));
    }
 
    public inventario crear(inventario inventario) {
        return inventarioRepository.save(inventario);
    }
 
    public inventario actualizar(Long id, inventario datosActualizados) {
        inventario inventarioExistente = buscarPorId(id);
        inventarioExistente.setProducto(datosActualizados.getProducto());
        inventarioExistente.setStock(datosActualizados.getStock());
        inventarioExistente.setPrecio(datosActualizados.getPrecio());
        return inventarioRepository.save(inventarioExistente);
    }
 
    public void eliminar(Long id) {
        inventarioRepository.delete(buscarPorId(id));
    }
}