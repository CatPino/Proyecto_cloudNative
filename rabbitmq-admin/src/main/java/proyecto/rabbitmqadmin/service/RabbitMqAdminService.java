package proyecto.rabbitmqadmin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMqAdminService implements ApplicationRunner {

    public static final String DIRECT_EXCHANGE = "tienda.exchange";
    public static final String TOPIC_EXCHANGE = "tienda.topic.exchange";
    public static final String DLX_EXCHANGE = "tienda.dlx";

    public static final String INVENTARIO_QUEUE = "inventario.queue";
    public static final String AUDITORIA_QUEUE = "auditoria.queue";
    public static final String NOTIFICACIONES_QUEUE = "notificaciones.queue";

    public static final String TIENDA_DLQ = "tienda.dlq";
    public static final String NOTIFICACIONES_DLQ = "notificaciones.dlq";

    public static final String RK_COMPRA_CREADA = "compra.creada";
    public static final String RK_COMPRA_CANCELADA = "compra.cancelada";
    public static final String RK_STOCK_ACTUALIZADO = "inventario.stock.actualizado";
    public static final String RK_DLX_DEFAULT = "mensaje.muerto";
    public static final String RK_DLX_NOTIFICACIONES = "notificaciones.muerto";

    private final AmqpAdmin amqpAdmin;

    private final Set<String> colasRegistradas = ConcurrentHashMap.newKeySet();
    private final Set<String> exchangesRegistrados = ConcurrentHashMap.newKeySet();

    @Override
    public void run(ApplicationArguments args) {
        try {
            inicializarTopologiaBase();
            log.info("[RABBITMQ-ADMIN] Topología base verificada/declarada correctamente mediante AmqpAdmin.");
        } catch (Exception e) {
            log.warn("[RABBITMQ-ADMIN] No se pudo inicializar topología al arrancar (RabbitMQ no disponible aún): {}", e.getMessage());
        }
    }

    public Map<String, Object> inicializarTopologiaBase() {
        // 1. Exchanges: Direct (existente), Topic (patrones) y DLX
        DirectExchange directExchange = new DirectExchange(DIRECT_EXCHANGE, true, false);
        TopicExchange topicExchange = new TopicExchange(TOPIC_EXCHANGE, true, false);
        DirectExchange dlxExchange = new DirectExchange(DLX_EXCHANGE, true, false);

        amqpAdmin.declareExchange(directExchange);
        amqpAdmin.declareExchange(topicExchange);
        amqpAdmin.declareExchange(dlxExchange);

        exchangesRegistrados.addAll(List.of(DIRECT_EXCHANGE, TOPIC_EXCHANGE, DLX_EXCHANGE));

        // 2. Dead Letter Queues
        Queue tiendaDlq = QueueBuilder.durable(TIENDA_DLQ).build();
        Queue notificacionesDlq = QueueBuilder.durable(NOTIFICACIONES_DLQ).build();
        amqpAdmin.declareQueue(tiendaDlq);
        amqpAdmin.declareQueue(notificacionesDlq);

        // 3. Colas principales (mismos argumentos que compra/inventario/usuario para 100% compatibilidad)
        Queue inventarioQueue = QueueBuilder.durable(INVENTARIO_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RK_DLX_DEFAULT)
                .build();

        Queue auditoriaQueue = QueueBuilder.durable(AUDITORIA_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RK_DLX_DEFAULT)
                .build();

        Queue notificacionesQueue = QueueBuilder.durable(NOTIFICACIONES_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RK_DLX_NOTIFICACIONES)
                .build();

        amqpAdmin.declareQueue(inventarioQueue);
        amqpAdmin.declareQueue(auditoriaQueue);
        amqpAdmin.declareQueue(notificacionesQueue);

        colasRegistradas.addAll(List.of(
                INVENTARIO_QUEUE,
                AUDITORIA_QUEUE,
                NOTIFICACIONES_QUEUE,
                TIENDA_DLQ,
                NOTIFICACIONES_DLQ
        ));

        // 4. Bindings DirectExchange
        amqpAdmin.declareBinding(BindingBuilder.bind(inventarioQueue).to(directExchange).with(RK_COMPRA_CREADA));
        amqpAdmin.declareBinding(BindingBuilder.bind(auditoriaQueue).to(directExchange).with(RK_COMPRA_CREADA));
        amqpAdmin.declareBinding(BindingBuilder.bind(auditoriaQueue).to(directExchange).with(RK_COMPRA_CANCELADA));
        amqpAdmin.declareBinding(BindingBuilder.bind(auditoriaQueue).to(directExchange).with(RK_STOCK_ACTUALIZADO));

        // 5. Bindings TopicExchange (enrutamiento por comodines)
        amqpAdmin.declareBinding(BindingBuilder.bind(notificacionesQueue).to(topicExchange).with("compra.*"));
        amqpAdmin.declareBinding(BindingBuilder.bind(notificacionesQueue).to(topicExchange).with("inventario.#"));

        // 6. Bindings DLX -> DLQ
        amqpAdmin.declareBinding(BindingBuilder.bind(tiendaDlq).to(dlxExchange).with(RK_DLX_DEFAULT));
        amqpAdmin.declareBinding(BindingBuilder.bind(notificacionesDlq).to(dlxExchange).with(RK_DLX_NOTIFICACIONES));

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("estado", "topologia_inicializada");
        resultado.put("exchanges", List.of(
                Map.of("nombre", DIRECT_EXCHANGE, "tipo", "direct"),
                Map.of("nombre", TOPIC_EXCHANGE, "tipo", "topic"),
                Map.of("nombre", DLX_EXCHANGE, "tipo", "direct (DLX)")
        ));
        resultado.put("colas", List.of(
                INVENTARIO_QUEUE,
                AUDITORIA_QUEUE,
                NOTIFICACIONES_QUEUE,
                TIENDA_DLQ,
                NOTIFICACIONES_DLQ
        ));
        return resultado;
    }

    public Map<String, Object> crearExchange(String nombre, String tipo, boolean durable, boolean autoDelete) {
        String tipoNorm = tipo == null ? "direct" : tipo.trim().toLowerCase(Locale.ROOT);
        Exchange exchange = switch (tipoNorm) {
            case "direct" -> new DirectExchange(nombre.trim(), durable, autoDelete);
            case "topic" -> new TopicExchange(nombre.trim(), durable, autoDelete);
            case "fanout" -> new FanoutExchange(nombre.trim(), durable, autoDelete);
            case "headers" -> new HeadersExchange(nombre.trim(), durable, autoDelete);
            default -> throw new IllegalArgumentException("Tipo de exchange no soportado: " + tipo + ". Usa: direct, topic, fanout o headers.");
        };

        amqpAdmin.declareExchange(exchange);
        exchangesRegistrados.add(exchange.getName());
        log.info("[RABBITMQ-ADMIN] Exchange declarado vía AmqpAdmin: '{}' (tipo={})", exchange.getName(), tipoNorm);

        return Map.of(
                "estado", "exchange_declarado",
                "nombre", exchange.getName(),
                "tipo", tipoNorm,
                "durable", durable,
                "autoDelete", autoDelete
        );
    }

    public Map<String, Object> eliminarExchange(String nombre) {
        boolean eliminado = amqpAdmin.deleteExchange(nombre.trim());
        exchangesRegistrados.remove(nombre.trim());
        log.info("[RABBITMQ-ADMIN] Exchange '{}' eliminado vía AmqpAdmin: {}", nombre, eliminado);
        return Map.of(
                "estado", eliminado ? "exchange_eliminado" : "no_encontrado",
                "nombre", nombre.trim(),
                "eliminado", eliminado
        );
    }

    public Map<String, Object> crearCola(String nombre, boolean durable, String dlxExchange, String dlxRoutingKey, boolean crearDlqAsociada) {
        String nombreCola = nombre.trim();
        QueueBuilder builder = durable ? QueueBuilder.durable(nombreCola) : QueueBuilder.nonDurable(nombreCola);

        String dlqCreada = null;
        if (crearDlqAsociada) {
            String dlx = (dlxExchange != null && !dlxExchange.isBlank()) ? dlxExchange.trim() : DLX_EXCHANGE;
            String dlqName = nombreCola + ".dlq";
            String dlxRk = (dlxRoutingKey != null && !dlxRoutingKey.isBlank()) ? dlxRoutingKey.trim() : dlqName;

            DirectExchange dlxBean = new DirectExchange(dlx, true, false);
            amqpAdmin.declareExchange(dlxBean);
            Queue dlqBean = QueueBuilder.durable(dlqName).build();
            amqpAdmin.declareQueue(dlqBean);
            amqpAdmin.declareBinding(BindingBuilder.bind(dlqBean).to(dlxBean).with(dlxRk));

            colasRegistradas.add(dlqName);
            exchangesRegistrados.add(dlx);

            builder.withArgument("x-dead-letter-exchange", dlx)
                   .withArgument("x-dead-letter-routing-key", dlxRk);
            dlqCreada = dlqName;
        } else if (dlxExchange != null && !dlxExchange.isBlank()) {
            builder.withArgument("x-dead-letter-exchange", dlxExchange.trim());
            if (dlxRoutingKey != null && !dlxRoutingKey.isBlank()) {
                builder.withArgument("x-dead-letter-routing-key", dlxRoutingKey.trim());
            }
        }

        Queue queue = builder.build();
        String declarada = amqpAdmin.declareQueue(queue);
        colasRegistradas.add(nombreCola);
        log.info("[RABBITMQ-ADMIN] Cola declarada vía AmqpAdmin: '{}' (dlq={})", declarada, dlqCreada);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("estado", "cola_declarada");
        resp.put("nombre", declarada != null ? declarada : nombreCola);
        resp.put("durable", durable);
        resp.put("dlqAsociada", dlqCreada);
        return resp;
    }

    public Map<String, Object> obtenerInfoCola(String nombre) {
        QueueInformation info = amqpAdmin.getQueueInfo(nombre.trim());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("nombre", nombre.trim());
        if (info == null) {
            resp.put("existe", false);
            resp.put("messageCount", 0);
            resp.put("consumerCount", 0);
        } else {
            resp.put("existe", true);
            resp.put("messageCount", info.getMessageCount());
            resp.put("consumerCount", info.getConsumerCount());
        }
        return resp;
    }

    public Map<String, Object> purgarCola(String nombre) {
        amqpAdmin.purgeQueue(nombre.trim(), false);
        log.info("[RABBITMQ-ADMIN] Cola '{}' purgada vía AmqpAdmin.", nombre);
        return Map.of(
                "estado", "cola_purgada",
                "nombre", nombre.trim()
        );
    }

    public Map<String, Object> eliminarCola(String nombre) {
        boolean eliminada = amqpAdmin.deleteQueue(nombre.trim());
        colasRegistradas.remove(nombre.trim());
        log.info("[RABBITMQ-ADMIN] Cola '{}' eliminada vía AmqpAdmin: {}", nombre, eliminada);
        return Map.of(
                "estado", eliminada ? "cola_eliminada" : "no_encontrada",
                "nombre", nombre.trim(),
                "eliminada", eliminada
        );
    }

    public Map<String, Object> crearBinding(String cola, String exchange, String routingKey) {
        Binding binding = new Binding(
                cola.trim(),
                Binding.DestinationType.QUEUE,
                exchange.trim(),
                routingKey == null ? "" : routingKey.trim(),
                Collections.emptyMap()
        );
        amqpAdmin.declareBinding(binding);
        colasRegistradas.add(cola.trim());
        exchangesRegistrados.add(exchange.trim());
        log.info("[RABBITMQ-ADMIN] Binding creado vía AmqpAdmin: cola='{}' <- exchange='{}' (rk='{}')",
                cola, exchange, routingKey);

        return Map.of(
                "estado", "binding_creado",
                "cola", cola.trim(),
                "exchange", exchange.trim(),
                "routingKey", routingKey == null ? "" : routingKey.trim()
        );
    }

    public Map<String, Object> eliminarBinding(String cola, String exchange, String routingKey) {
        Binding binding = new Binding(
                cola.trim(),
                Binding.DestinationType.QUEUE,
                exchange.trim(),
                routingKey == null ? "" : routingKey.trim(),
                Collections.emptyMap()
        );
        amqpAdmin.removeBinding(binding);
        log.info("[RABBITMQ-ADMIN] Binding eliminado vía AmqpAdmin: cola='{}' <- exchange='{}' (rk='{}')",
                cola, exchange, routingKey);

        return Map.of(
                "estado", "binding_eliminado",
                "cola", cola.trim(),
                "exchange", exchange.trim(),
                "routingKey", routingKey == null ? "" : routingKey.trim()
        );
    }

    public Map<String, Object> obtenerEstadoGeneral() {
        List<String> ordenadas = new ArrayList<>(colasRegistradas);
        Collections.sort(ordenadas);

        List<Map<String, Object>> detalleColas = new ArrayList<>();
        for (String nombreCola : ordenadas) {
            detalleColas.add(obtenerInfoCola(nombreCola));
        }

        List<String> exchangesOrdenados = new ArrayList<>(exchangesRegistrados);
        Collections.sort(exchangesOrdenados);

        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("servicio", "rabbitmq-admin");
        estado.put("mecanismo", "Spring AmqpAdmin (RabbitAdmin)");
        estado.put("exchangesRegistrados", exchangesOrdenados);
        estado.put("colas", detalleColas);
        return estado;
    }
}
