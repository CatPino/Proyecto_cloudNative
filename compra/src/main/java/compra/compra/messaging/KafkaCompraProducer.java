package compra.compra.messaging;

import compra.compra.config.KafkaConfig;
import compra.compra.entities.compra;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaCompraProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publicarCompraCreada(compra compraGuardada, String origen) {
        CompraStreamEvent evento = CompraStreamEvent.of(
                "COMPRA_CREADA",
                compraGuardada.getId(),
                compraGuardada.getProducto(),
                compraGuardada.getCantidad(),
                compraGuardada.getPrecio(),
                origen
        );
        enviarAsincrono(evento);
    }

    public void publicarCompraCancelada(compra compraEliminada, String origen) {
        CompraStreamEvent evento = CompraStreamEvent.of(
                "COMPRA_CANCELADA",
                compraEliminada.getId(),
                compraEliminada.getProducto(),
                compraEliminada.getCantidad(),
                compraEliminada.getPrecio(),
                origen
        );
        enviarAsincrono(evento);
    }

    public Map<String, Object> publicarSincrono(CompraStreamEvent evento) {
        String key = normalizarKey(evento.producto());
        String payload = objectMapper.writeValueAsString(evento);

        try {
            SendResult<String, String> result = kafkaTemplate
                    .send(KafkaConfig.TOPIC_COMPRAS_STREAM, key, payload)
                    .get(5, TimeUnit.SECONDS);

            RecordMetadata meta = result.getRecordMetadata();
            log.info("[KAFKA-COMPRA] Evento {} publicado en {} [partition={}, offset={}, key={}]",
                    evento.eventId(), meta.topic(), meta.partition(), meta.offset(), key);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("estado", "publicado");
            response.put("topic", meta.topic());
            response.put("partition", meta.partition());
            response.put("offset", meta.offset());
            response.put("key", key);
            response.put("evento", evento);
            return response;
        } catch (Exception e) {
            log.error("[KAFKA-COMPRA] Error al publicar evento en Kafka: {}", e.getMessage());
            throw new IllegalStateException("No se pudo publicar el evento en Kafka: " + e.getMessage(), e);
        }
    }

    private void enviarAsincrono(CompraStreamEvent evento) {
        try {
            String key = normalizarKey(evento.producto());
            String payload = objectMapper.writeValueAsString(evento);
            kafkaTemplate.send(KafkaConfig.TOPIC_COMPRAS_STREAM, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("[KAFKA-COMPRA] No se pudo publicar evento {}: {}", evento.eventId(), ex.getMessage());
                        } else {
                            RecordMetadata meta = result.getRecordMetadata();
                            log.info("[KAFKA-COMPRA] Evento {} ({}) -> {} [partition={}, offset={}, key={}]",
                                    evento.eventType(), evento.producto(), meta.topic(), meta.partition(), meta.offset(), key);
                        }
                    });
        } catch (Exception e) {
            log.warn("[KAFKA-COMPRA] Error serializando o enviando evento a Kafka (CRUD no afectado): {}", e.getMessage());
        }
    }

    private String normalizarKey(String producto) {
        if (producto == null || producto.isBlank()) {
            return "sin-producto";
        }
        return producto.trim().toLowerCase(Locale.ROOT);
    }
}
