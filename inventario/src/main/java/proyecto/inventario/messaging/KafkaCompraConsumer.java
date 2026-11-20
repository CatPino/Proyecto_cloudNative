package proyecto.inventario.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import proyecto.inventario.config.KafkaConfig;
import proyecto.inventario.entities.inventario;
import proyecto.inventario.repositories.inventarioRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaCompraConsumer {

    private final inventarioRepository inventarioRepository;
    private final KafkaInventarioProducer kafkaInventarioProducer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaConfig.TOPIC_COMPRAS_STREAM, groupId = KafkaConfig.GROUP_INVENTARIO_STOCK)
    public void consumirCompra(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            CompraStreamEvent evento = objectMapper.readValue(payload, CompraStreamEvent.class);
            log.info("[KAFKA-INVENTARIO][group={}] Recibido {} producto='{}' cant={} [partition={}, offset={}]",
                    KafkaConfig.GROUP_INVENTARIO_STOCK, evento.eventType(), evento.producto(),
                    evento.cantidad(), partition, offset);

            Optional<inventario> existenteOpt = inventarioRepository.findFirstByProductoIgnoreCase(evento.producto());
            if (existenteOpt.isEmpty()) {
                InventarioAlertaEvent alerta = InventarioAlertaEvent.of(
                        "PRODUCTO_NO_REGISTRADO",
                        null,
                        evento.producto(),
                        null,
                        null,
                        KafkaConfig.UMBRAL_STOCK_BAJO,
                        "WARNING",
                        evento.compraId(),
                        "Compra recibida por streaming para producto no registrado en inventario: " + evento.producto()
                );
                kafkaInventarioProducer.publicarAlerta(alerta);
                return;
            }

            inventario item = existenteOpt.get();
            int stockAnterior = item.getStock() != null ? item.getStock() : 0;
            int cantidad = evento.cantidad() != null ? evento.cantidad() : 0;
            int stockActual;
            String eventType;
            String severidad;
            String mensaje;

            if ("COMPRA_CANCELADA".equalsIgnoreCase(evento.eventType())) {
                stockActual = stockAnterior + cantidad;
                eventType = "STOCK_RESTAURADO";
                severidad = "INFO";
                mensaje = String.format("Stock restaurado para '%s': %d -> %d (+%d por cancelación)",
                        item.getProducto(), stockAnterior, stockActual, cantidad);
            } else {
                stockActual = Math.max(0, stockAnterior - cantidad);
                if (stockActual == 0) {
                    eventType = "ALERTA_SIN_STOCK";
                    severidad = "CRITICAL";
                    mensaje = String.format("CRÍTICO: '%s' quedó sin stock (%d -> 0). Requiere reposición inmediata.",
                            item.getProducto(), stockAnterior);
                } else if (stockActual <= KafkaConfig.UMBRAL_STOCK_BAJO) {
                    eventType = "ALERTA_STOCK_BAJO";
                    severidad = "WARNING";
                    mensaje = String.format("ALERTA: '%s' bajo el umbral mínimo (%d -> %d, umbral=%d).",
                            item.getProducto(), stockAnterior, stockActual, KafkaConfig.UMBRAL_STOCK_BAJO);
                } else {
                    eventType = "STOCK_ACTUALIZADO";
                    severidad = "INFO";
                    mensaje = String.format("Stock descontado automáticamente para '%s': %d -> %d (-%d).",
                            item.getProducto(), stockAnterior, stockActual, cantidad);
                }
            }

            item.setStock(stockActual);
            inventarioRepository.save(item);

            InventarioAlertaEvent alerta = InventarioAlertaEvent.of(
                    eventType,
                    item.getId(),
                    item.getProducto(),
                    stockAnterior,
                    stockActual,
                    KafkaConfig.UMBRAL_STOCK_BAJO,
                    severidad,
                    evento.compraId(),
                    mensaje
            );
            kafkaInventarioProducer.publicarAlerta(alerta);
        } catch (Exception e) {
            log.error("[KAFKA-INVENTARIO] Error procesando evento [partition={}, offset={}]: {}",
                    partition, offset, e.getMessage());
        }
    }
}
