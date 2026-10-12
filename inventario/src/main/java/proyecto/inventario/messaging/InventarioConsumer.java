package proyecto.inventario.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import proyecto.inventario.config.RabbitConfig;

@Component
@Slf4j
public class InventarioConsumer {

    @RabbitListener(queues = RabbitConfig.INVENTARIO_QUEUE)
    public void recibir(String mensaje) {
        log.info("[INVENTARIO] Evento recibido: {}", mensaje);
    }
}