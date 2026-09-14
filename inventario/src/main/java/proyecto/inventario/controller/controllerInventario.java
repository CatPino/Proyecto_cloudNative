package proyecto.inventario.controller;


import proyecto.inventario.entities.inventario;
import proyecto.inventario.service.inventarioService;
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
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class controllerInventario {
 
    private final inventarioService inventarioService;
 
    @GetMapping
    public ResponseEntity<List<inventario>> listarTodos() {
        return ResponseEntity.ok(inventarioService.listarTodos());
    }
 
    @GetMapping("/{id}")
    public ResponseEntity<inventario> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(inventarioService.buscarPorId(id));
    }
 
    @PostMapping
    public ResponseEntity<inventario> crear(@RequestBody inventario inventario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.crear(inventario));
    }
 
    @PutMapping("/{id}")
    public ResponseEntity<inventario> actualizar(@PathVariable Long id, @RequestBody inventario inventario) {
        return ResponseEntity.ok(inventarioService.actualizar(id, inventario));
    }
 
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        inventarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
 