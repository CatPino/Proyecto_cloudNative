package compra.compra.controller;

import compra.compra.entities.compra;
import compra.compra.service.compraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import java.util.List;
 
@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class controller {
 
    private final compraService compraService;
 
    @GetMapping
    public ResponseEntity<List<compra>> listarTodas() {
        return ResponseEntity.ok(compraService.listarTodas());
    }
 
    @GetMapping("/{id}")
    public ResponseEntity<compra> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.buscarPorId(id));
    }
 
    @PostMapping
    public ResponseEntity<compra> crear(@RequestBody compra compra) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compraService.crear(compra));
    }
 
    @PutMapping("/{id}")
    public ResponseEntity<compra> actualizar(@PathVariable Long id, @RequestBody compra compra) {
        return ResponseEntity.ok(compraService.actualizar(id, compra));
    }
 
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        compraService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
 