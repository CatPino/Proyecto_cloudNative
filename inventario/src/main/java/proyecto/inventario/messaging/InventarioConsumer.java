package proyecto.inventario.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import proyecto.inventario.config.RabbitConfig;

@Component
@Slf4j
public class InventarioConsumer {

    @RabbitListener(queues = RabbitConfig.INVENTARIO_QUEUE)
    public void recibir(String mensaje) {
        // Mensaje inválido: se rechaza sin reencolar y pasa a la DLQ
        if (mensaje == null || mensaje.isBlank()) {
            log.warn("[INVENTARIO] Mensaje vacío: se rechaza y pasa a la DLQ ({})", RabbitConfig.DLQ_QUEUE);
            throw new AmqpRejectAndDontRequeueException("Mensaje vacío");
        }
        // Falla simulada: Spring reintenta con backoff y, al agotar los reintentos, el mensaje pasa a la DLQ
        if (mensaje.contains("FAIL")) {
            log.warn("[INVENTARIO] Falla simulada al procesar: '{}'", mensaje);
            throw new IllegalStateException("Fallo simulado en Inventario");
        }
        log.info("[INVENTARIO] Evento recibido: {}", mensaje);
    }
}