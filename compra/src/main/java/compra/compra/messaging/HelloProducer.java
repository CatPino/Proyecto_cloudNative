package compra.compra.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HelloProducer implements ApplicationRunner {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            rabbitTemplate.convertAndSend("hello", "Hola desde Compra");
            log.info("[COMPRA] Mensaje enviado a la cola 'hello'");
        } catch (Exception e) {
            log.error("[COMPRA] No se pudo enviar el mensaje: {}", e.getMessage());
        }
    }
}