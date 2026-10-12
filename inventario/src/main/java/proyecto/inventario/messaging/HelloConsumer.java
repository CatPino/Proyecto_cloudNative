package proyecto.inventario.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HelloConsumer {

    @RabbitListener(queues = "hello")
    public void recibir(String mensaje) {
        log.info("[INVENTARIO] Mensaje recibido: {}", mensaje);
    }
}