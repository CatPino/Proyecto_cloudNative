package compra.compra.service;


import compra.compra.entities.compra;
import compra.compra.repositories.compraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
 
import java.util.List;
 
@Service
@RequiredArgsConstructor
public class compraService {
 
    private final compraRepository compraRepository;
 
    public List<compra> listarTodas() {
        return compraRepository.findAll();
    }
 
    public compra buscarPorId(Long id) {
        return compraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compra no encontrada con id: " + id));
    }
 
    public compra crear(compra compra) {
        return compraRepository.save(compra);
    }
 
    public compra actualizar(Long id, compra datosActualizados) {
        compra compraExistente = buscarPorId(id);
        compraExistente.setProducto(datosActualizados.getProducto());
        compraExistente.setCantidad(datosActualizados.getCantidad());
        compraExistente.setPrecio(datosActualizados.getPrecio());
        compraExistente.setFecha(datosActualizados.getFecha());
        return compraRepository.save(compraExistente);
    }
 
    public void eliminar(Long id) {
        compraRepository.delete(buscarPorId(id));
    }
}
 