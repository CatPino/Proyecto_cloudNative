package proyecto.inventario.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import proyecto.inventario.config.KafkaConfig;
import tools.jackson.databind.ObjectMapper;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaInventarioProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publicarAlerta(InventarioAlertaEvent evento) {
        try {
            String key = normalizarKey(evento.producto());
            String payload = objectMapper.writeValueAsString(evento);
            kafkaTemplate.send(KafkaConfig.TOPIC_INVENTARIO_ALERTAS, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("[KAFKA-INVENTARIO] Error publicando alerta {}: {}", evento.eventId(), ex.getMessage());
                        } else {
                            RecordMetadata meta = result.getRecordMetadata();
                            log.info("[KAFKA-INVENTARIO] Alerta {} ({}) -> {} [partition={}, offset={}, key={}]",
                                    evento.eventType(), evento.producto(), meta.topic(), meta.partition(), meta.offset(), key);
                        }
                    });
        } catch (Exception e) {
            log.warn("[KAFKA-INVENTARIO] No se pudo publicar evento en Kafka: {}", e.getMessage());
        }
    }

    private String normalizarKey(String producto) {
        if (producto == null || producto.isBlank()) {
            return "sin-producto";
        }
        return producto.trim().toLowerCase(Locale.ROOT);
    }
}
