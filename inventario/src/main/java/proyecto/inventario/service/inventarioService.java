package proyecto.inventario.service;


import proyecto.inventario.config.KafkaConfig;
import proyecto.inventario.entities.inventario;
import proyecto.inventario.messaging.InventarioAlertaEvent;
import proyecto.inventario.messaging.KafkaInventarioProducer;
import proyecto.inventario.repositories.inventarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
 
import java.util.List;
 
@Service
@RequiredArgsConstructor
public class inventarioService {
 
    private final inventarioRepository inventarioRepository;
    private final KafkaInventarioProducer kafkaInventarioProducer;
 
    public List<inventario> listarTodos() {
        return inventarioRepository.findAll();
    }
 
    public inventario buscarPorId(Long id) {
        return inventarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventario no encontrado con id: " + id));
    }
 
    public inventario crear(inventario inventario) {
        inventario guardado = inventarioRepository.save(inventario);
        emitirEventoStock(guardado, 0, guardado.getStock(), "Producto creado en inventario");
        return guardado;
    }
 
    public inventario actualizar(Long id, inventario datosActualizados) {
        inventario inventarioExistente = buscarPorId(id);
        int stockAnterior = inventarioExistente.getStock() != null ? inventarioExistente.getStock() : 0;
        inventarioExistente.setProducto(datosActualizados.getProducto());
        inventarioExistente.setStock(datosActualizados.getStock());
        inventarioExistente.setPrecio(datosActualizados.getPrecio());
        inventario guardado = inventarioRepository.save(inventarioExistente);
        emitirEventoStock(guardado, stockAnterior, guardado.getStock(), "Stock actualizado manualmente");
        return guardado;
    }
 
    public void eliminar(Long id) {
        inventarioRepository.delete(buscarPorId(id));
    }

    private void emitirEventoStock(inventario item, int stockAnterior, Integer stockActualObj, String motivo) {
        int stockActual = stockActualObj != null ? stockActualObj : 0;
        String eventType;
        String severidad;
        if (stockActual == 0) {
            eventType = "ALERTA_SIN_STOCK";
            severidad = "CRITICAL";
        } else if (stockActual <= KafkaConfig.UMBRAL_STOCK_BAJO) {
            eventType = "ALERTA_STOCK_BAJO";
            severidad = "WARNING";
        } else {
            eventType = "STOCK_ACTUALIZADO";
            severidad = "INFO";
        }

        InventarioAlertaEvent alerta = InventarioAlertaEvent.of(
                eventType,
                item.getId(),
                item.getProducto(),
                stockAnterior,
                stockActual,
                KafkaConfig.UMBRAL_STOCK_BAJO,
                severidad,
                null,
                String.format("%s: '%s' (%d -> %d)", motivo, item.getProducto(), stockAnterior, stockActual)
        );
        kafkaInventarioProducer.publicarAlerta(alerta);
    }
}