package compra.compra.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import compra.compra.config.RabbitConfig;

@Service
public class CompraConsumer {

    // Cambiamos COMPRA_QUEUE por INVENTARIO_QUEUE (o AUDITORIA_QUEUE si lo prefieres)
    @RabbitListener(queues = RabbitConfig.INVENTARIO_QUEUE)
    public void procesarCompra(String mensaje) {
        System.out.println("[PROCESADOR] Recibida orden: " + mensaje);

        // Simular un fallo para probar la DLQ
        if (mensaje.contains("FAIL")) {
            throw new RuntimeException("Fallo didáctico para probar DLQ");
        }

        System.out.println("[ÉXITO] Orden procesada correctamente.");
    }
}