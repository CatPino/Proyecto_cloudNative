package proyecto.inventario.messaging;

import java.time.Instant;
import java.util.UUID;

public record InventarioAlertaEvent(
        String eventId,
        String eventType,
        String timestamp,
        Long inventarioId,
        String producto,
        Integer stockAnterior,
        Integer stockActual,
        Integer umbralMinimo,
        String severidad,
        Long compraIdOrigen,
        String mensaje
) {
    public static InventarioAlertaEvent of(
            String eventType,
            Long inventarioId,
            String producto,
            Integer stockAnterior,
            Integer stockActual,
            Integer umbralMinimo,
            String severidad,
            Long compraIdOrigen,
            String mensaje) {
        return new InventarioAlertaEvent(
                UUID.randomUUID().toString(),
                eventType,
                Instant.now().toString(),
                inventarioId,
                producto,
                stockAnterior,
                stockActual,
                umbralMinimo,
                severidad,
                compraIdOrigen,
                mensaje
        );
    }
}
