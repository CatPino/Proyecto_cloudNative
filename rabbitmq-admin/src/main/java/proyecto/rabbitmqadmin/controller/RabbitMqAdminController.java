package proyecto.rabbitmqadmin.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import proyecto.rabbitmqadmin.service.RabbitMqAdminService;

import java.util.Map;

@RestController
@RequestMapping("/api/rabbitmq-admin")
@RequiredArgsConstructor
public class RabbitMqAdminController {

    private final RabbitMqAdminService adminService;

    public record ExchangeRequest(
            @NotBlank(message = "El nombre del exchange es obligatorio") String nombre,
            @NotBlank(message = "El tipo de exchange es obligatorio (direct, topic, fanout, headers)") String tipo,
            Boolean durable,
            Boolean autoDelete
    ) {}

    public record ColaRequest(
            @NotBlank(message = "El nombre de la cola es obligatorio") String nombre,
            Boolean durable,
            String dlxExchange,
            String dlxRoutingKey,
            Boolean crearDlqAsociada
    ) {}

    public record BindingRequest(
            @NotBlank(message = "La cola es obligatoria") String cola,
            @NotBlank(message = "El exchange es obligatorio") String exchange,
            String routingKey
    ) {}

    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> estado() {
        return ResponseEntity.ok(adminService.obtenerEstadoGeneral());
    }

    @PostMapping("/topologia/inicializar")
    public ResponseEntity<Map<String, Object>> inicializarTopologia() {
        return ResponseEntity.ok(adminService.inicializarTopologiaBase());
    }

    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, Object>> crearExchange(@Valid @RequestBody ExchangeRequest request) {
        boolean durable = request.durable() == null || request.durable();
        boolean autoDelete = request.autoDelete() != null && request.autoDelete();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.crearExchange(request.nombre(), request.tipo(), durable, autoDelete));
    }

    @DeleteMapping("/exchanges/{nombre}")
    public ResponseEntity<Map<String, Object>> eliminarExchange(@PathVariable String nombre) {
        return ResponseEntity.ok(adminService.eliminarExchange(nombre));
    }

    @PostMapping("/colas")
    public ResponseEntity<Map<String, Object>> crearCola(@Valid @RequestBody ColaRequest request) {
        boolean durable = request.durable() == null || request.durable();
        boolean crearDlq = request.crearDlqAsociada() != null && request.crearDlqAsociada();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.crearCola(
                        request.nombre(),
                        durable,
                        request.dlxExchange(),
                        request.dlxRoutingKey(),
                        crearDlq
                ));
    }

    @GetMapping("/colas/{nombre}")
    public ResponseEntity<Map<String, Object>> infoCola(@PathVariable String nombre) {
        return ResponseEntity.ok(adminService.obtenerInfoCola(nombre));
    }

    @PostMapping("/colas/{nombre}/purgar")
    public ResponseEntity<Map<String, Object>> purgarCola(@PathVariable String nombre) {
        return ResponseEntity.ok(adminService.purgarCola(nombre));
    }

    @DeleteMapping("/colas/{nombre}")
    public ResponseEntity<Map<String, Object>> eliminarCola(@PathVariable String nombre) {
        return ResponseEntity.ok(adminService.eliminarCola(nombre));
    }

    @PostMapping("/bindings")
    public ResponseEntity<Map<String, Object>> crearBinding(@Valid @RequestBody BindingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.crearBinding(request.cola(), request.exchange(), request.routingKey()));
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Map<String, Object>> eliminarBinding(
            @RequestParam String cola,
            @RequestParam String exchange,
            @RequestParam(defaultValue = "") String routingKey) {
        return ResponseEntity.ok(adminService.eliminarBinding(cola, exchange, routingKey));
    }
}
