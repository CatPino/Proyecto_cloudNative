package compra.compra.messaging;

import java.time.Instant;
import java.util.UUID;

public record CompraStreamEvent(
        String eventId,
        String eventType,
        String timestamp,
        Long compraId,
        String producto,
        Integer cantidad,
        Double precioUnitario,
        Double montoTotal,
        String origen
) {
    public static CompraStreamEvent of(
            String eventType,
            Long compraId,
            String producto,
            Integer cantidad,
            Double precioUnitario,
            String origen) {
        int cant = cantidad != null ? cantidad : 0;
        double precio = precioUnitario != null ? precioUnitario : 0.0;
        return new CompraStreamEvent(
                UUID.randomUUID().toString(),
                eventType,
                Instant.now().toString(),
                compraId,
                producto,
                cant,
                precio,
                cant * precio,
                origen
        );
    }
}
