package compra.compra.service;


import compra.compra.entities.compra;
import compra.compra.messaging.KafkaCompraProducer;
import compra.compra.repositories.compraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
 
import java.util.List;
 
@Service
@RequiredArgsConstructor
public class compraService {
 
    private final compraRepository compraRepository;
    private final KafkaCompraProducer kafkaCompraProducer;
 
    public List<compra> listarTodas() {
        return compraRepository.findAll();
    }
 
    public compra buscarPorId(Long id) {
        return compraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compra no encontrada con id: " + id));
    }
 
    public compra crear(compra compra) {
        compra guardada = compraRepository.save(compra);
        kafkaCompraProducer.publicarCompraCreada(guardada, "CRUD_COMPRA");
        return guardada;
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
        compra existente = buscarPorId(id);
        compraRepository.delete(existente);
        kafkaCompraProducer.publicarCompraCancelada(existente, "CRUD_COMPRA");
    }
}
 