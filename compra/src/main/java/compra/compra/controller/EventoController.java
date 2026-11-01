package compra.compra.controller;

import compra.compra.config.RabbitConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    // El mensaje puede venir vacío a propósito: así se prueba el rechazo del consumidor hacia la DLQ.
    public record EventoRequest(
            @NotBlank(message = "routingKey es obligatoria") String routingKey,
            @Size(max = 500, message = "mensaje no puede superar 500 caracteres") String mensaje) {}

    @PostMapping
    public ResponseEntity<Map<String, Object>> publicar(@Valid @RequestBody EventoRequest request) {
        if (!ROUTING_KEYS_PERMITIDAS.contains(request.routingKey())) {
            log.warn("[COMPRA] Routing key no permitida: '{}'", request.routingKey());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "routingKey no permitida",
                    "permitidas", ROUTING_KEYS_PERMITIDAS));
        }

        String mensaje = request.mensaje() == null ? "" : request.mensaje();
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, request.routingKey(), mensaje);
        log.info("[COMPRA] Evento publicado con routing key '{}': {}", request.routingKey(), mensaje);

        return ResponseEntity.ok(Map.of("estado", "enviado", "routingKey", request.routingKey()));
    }

    // ========== ENDPOINT DE MÉTRICAS / ALERTA DE LA DLQ ==========
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metrics() {
        // Consulta la cantidad de mensajes en la DLQ directamente al canal de RabbitMQ
        Long dlqReady = rabbitTemplate.execute(channel -> channel.messageCount(RabbitConfig.DLQ_QUEUE));

        long cantidadDlq = dlqReady != null ? dlqReady : 0;
        boolean hayAlerta = cantidadDlq > 0;

        log.info("[MÉTRICAS] Consulta de DLQ: {} mensajes en cuarentena. Alerta: {}", cantidadDlq, hayAlerta);

        return ResponseEntity.ok(Map.of(
                "dlqReady", cantidadDlq,
                "alert", hayAlerta));
    }
}