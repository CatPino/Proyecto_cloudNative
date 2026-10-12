package proyecto.usuario.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "tienda.exchange";

    public static final String INVENTARIO_QUEUE = "inventario.queue";
    public static final String AUDITORIA_QUEUE = "auditoria.queue";

    public static final String RK_COMPRA_CREADA = "compra.creada";
    public static final String RK_COMPRA_CANCELADA = "compra.cancelada";
    public static final String RK_STOCK_ACTUALIZADO = "inventario.stock.actualizado";

    // ========== CONSTANTES DLX / DLQ ==========
    public static final String DLX_EXCHANGE = "tienda.dlx";
    public static final String DLQ_QUEUE = "tienda.dlq";
    public static final String DLX_ROUTING_KEY = "mensaje.muerto";

    @Bean
    public DirectExchange tiendaExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue inventarioQueue() {
        return QueueBuilder.durable(INVENTARIO_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue auditoriaQueue() {
        return QueueBuilder.durable(AUDITORIA_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding bindInventarioCompraCreada(DirectExchange tiendaExchange, Queue inventarioQueue) {
        return BindingBuilder.bind(inventarioQueue).to(tiendaExchange).with(RK_COMPRA_CREADA);
    }

    @Bean
    public Binding bindAuditoriaCompraCreada(DirectExchange tiendaExchange, Queue auditoriaQueue) {
        return BindingBuilder.bind(auditoriaQueue).to(tiendaExchange).with(RK_COMPRA_CREADA);
    }

    @Bean
    public Binding bindAuditoriaCompraCancelada(DirectExchange tiendaExchange, Queue auditoriaQueue) {
        return BindingBuilder.bind(auditoriaQueue).to(tiendaExchange).with(RK_COMPRA_CANCELADA);
    }

    @Bean
    public Binding bindAuditoriaStockActualizado(DirectExchange tiendaExchange, Queue auditoriaQueue) {
        return BindingBuilder.bind(auditoriaQueue).to(tiendaExchange).with(RK_STOCK_ACTUALIZADO);
    }

    @Bean
    public Binding dlqBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DLX_ROUTING_KEY);
    }
}