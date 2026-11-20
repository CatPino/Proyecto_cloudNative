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
public class KafkaAnaliticaConsumer {

    private final AnaliticaStreamingService analiticaStreamingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaConfig.TOPIC_COMPRAS_STREAM, groupId = KafkaConfig.GROUP_ANALITICA_VENTAS)
    public void consumirParaAnalitica(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            CompraStreamEvent evento = objectMapper.readValue(payload, CompraStreamEvent.class);
            analiticaStreamingService.registrarCompra(evento, partition, offset);
            log.info("[KAFKA-ANALITICA][group={}] Procesado {} producto='{}' total={} [partition={}, offset={}]",
                    KafkaConfig.GROUP_ANALITICA_VENTAS, evento.eventType(), evento.producto(),
                    evento.montoTotal(), partition, offset);
        } catch (Exception e) {
            log.error("[KAFKA-ANALITICA] Error procesando evento [partition={}, offset={}]: {}",
                    partition, offset, e.getMessage());
        }
    }
}
