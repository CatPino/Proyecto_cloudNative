package proyecto.usuario.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import proyecto.usuario.config.KafkaConfig;
import proyecto.usuario.service.AnaliticaStreamingService;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaAlertasConsumer {

    private final AnaliticaStreamingService analiticaStreamingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaConfig.TOPIC_INVENTARIO_ALERTAS, groupId = KafkaConfig.GROUP_MONITOREO_ALERTAS)
    public void consumirAlertas(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            InventarioAlertaEvent evento = objectMapper.readValue(payload, InventarioAlertaEvent.class);
            analiticaStreamingService.registrarAlerta(evento, partition, offset);
            log.info("[KAFKA-ALERTAS][group={}] Alerta {} [{}] producto='{}' [partition={}, offset={}]",
                    KafkaConfig.GROUP_MONITOREO_ALERTAS, evento.eventType(), evento.severidad(),
                    evento.producto(), partition, offset);
        } catch (Exception e) {
            log.error("[KAFKA-ALERTAS] Error procesando alerta [partition={}, offset={}]: {}",
                    partition, offset, e.getMessage());
        }
    }
}
