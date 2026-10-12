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

    @Bean
    public DirectExchange tiendaExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue inventarioQueue() {
        return new Queue(INVENTARIO_QUEUE, true);
    }

    @Bean
    public Queue auditoriaQueue() {
        return new Queue(AUDITORIA_QUEUE, true);
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
}