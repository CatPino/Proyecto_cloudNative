package proyecto.usuario.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import proyecto.usuario.config.RabbitConfig;

@Component
@Slf4j
public class AuditoriaConsumer {

    @RabbitListener(queues = RabbitConfig.AUDITORIA_QUEUE)
    public void recibir(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            log.warn("[AUDITORIA] Mensaje vacío: se rechaza y pasa a la DLQ ({})", RabbitConfig.DLQ_QUEUE);
            throw new AmqpRejectAndDontRequeueException("Mensaje vacío");
        }
        if (mensaje.contains("FAIL")) {
            log.warn("[AUDITORIA] Falla simulada al procesar: '{}'", mensaje);
            throw new IllegalStateException("Fallo simulado en Auditoría");
        }
        log.info("[AUDITORIA] Evento recibido: {}", mensaje);
    }
}