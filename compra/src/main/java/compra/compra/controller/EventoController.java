package compra.compra.controller;

import compra.compra.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/compras/eventos")
@RequiredArgsConstructor
@Slf4j
public class EventoController {

    private static final Set<String> ROUTING_KEYS_PERMITIDAS = Set.of(
            RabbitConfig.RK_COMPRA_CREADA,
            RabbitConfig.RK_COMPRA_CANCELADA,
            RabbitConfig.RK_STOCK_ACTUALIZADO);

    private final RabbitTemplate rabbitTemplate;

    public record EventoRequest(String routingKey, String mensaje) {}

    @PostMapping
    public ResponseEntity<Map<String, String>> publicar(@RequestBody EventoRequest request) {
        if (request.routingKey() == null || !ROUTING_KEYS_PERMITIDAS.contains(request.routingKey())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Routing key no permitida: " + request.routingKey()));
        }

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, request.routingKey(), request.mensaje());
        log.info("[COMPRA] Evento publicado con routing key '{}': {}", request.routingKey(), request.mensaje());

        return ResponseEntity.ok(Map.of("estado", "enviado", "routingKey", request.routingKey()));
    }

  // ========== NUEVO ENDPOINT DE MÉTRICAS / DLQ ==========
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metrics() {
        // Consulta la cantidad de mensajes en la DLQ directamente al canal de RabbitMQ
        Long dlqReady = rabbitTemplate.execute(channel -> channel.messageCount(RabbitConfig.DLQ_QUEUE));
        
        long cantidadDlq = dlqReady != null ? dlqReady : 0;
        boolean hayAlerta = cantidadDlq > 0;
        
        log.info("[MÉTRICAS] Consulta de DLQ: {} mensajes en cuarentena. Alerta: {}", cantidadDlq, hayAlerta);

        return ResponseEntity.ok(Map.of(
            "dlqReady", cantidadDlq,
            "alert", hayAlerta
        ));
    }
}