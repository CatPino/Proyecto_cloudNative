package proyecto.inventario.messaging;

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
) {}
