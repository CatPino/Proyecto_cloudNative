package proyecto.usuario.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import proyecto.usuario.config.RabbitConfig;

@Component
@Slf4j
public class AuditoriaConsumer {

    @RabbitListener(queues = RabbitConfig.AUDITORIA_QUEUE)
    public void recibir(String mensaje) {
        log.info("[AUDITORIA] Evento recibido: {}", mensaje);
    }
}