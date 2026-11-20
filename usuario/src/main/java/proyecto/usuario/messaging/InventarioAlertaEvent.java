package proyecto.usuario.messaging;

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
) {}
