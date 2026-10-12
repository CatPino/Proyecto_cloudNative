package compra.compra.messaging;

import compra.compra.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventoProducer implements ApplicationRunner {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            enviar(RabbitConfig.RK_COMPRA_CREADA, "Compra #1 creada");
            enviar(RabbitConfig.RK_COMPRA_CANCELADA, "Compra #1 cancelada");
            enviar(RabbitConfig.RK_STOCK_ACTUALIZADO, "Stock del producto 1 actualizado");
        } catch (Exception e) {
            log.error("[COMPRA] No se pudieron enviar los eventos: {}", e.getMessage());
        }
    }

    private void enviar(String routingKey, String mensaje) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, routingKey, mensaje);
        log.info("[COMPRA] Enviado con routing key '{}': {}", routingKey, mensaje);
    }
}