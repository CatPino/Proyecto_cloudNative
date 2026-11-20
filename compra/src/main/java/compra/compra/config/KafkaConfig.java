package compra.compra.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_COMPRAS_STREAM = "compras.eventos.stream";
    public static final String TOPIC_INVENTARIO_ALERTAS = "inventario.alertas.stream";

    // Retención diferenciada por caso de uso:
    // - Compras/analítica: 7 días (permite reprocesamiento e historial analítico)
    // - Alertas de inventario: 24 horas (alertas operacionales de corta vigencia)
    public static final String RETENTION_COMPRAS_MS = "604800000";
    public static final String RETENTION_ALERTAS_MS = "86400000";

    @Bean
    public NewTopic comprasEventosTopic() {
        return TopicBuilder.name(TOPIC_COMPRAS_STREAM)
                .partitions(3)
                .replicas(3)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .config(TopicConfig.RETENTION_MS_CONFIG, RETENTION_COMPRAS_MS)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }

    @Bean
    public NewTopic inventarioAlertasTopic() {
        return TopicBuilder.name(TOPIC_INVENTARIO_ALERTAS)
                .partitions(3)
                .replicas(3)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .config(TopicConfig.RETENTION_MS_CONFIG, RETENTION_ALERTAS_MS)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }
}
