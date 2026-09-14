package compra.compra.entities;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.time.LocalDate;
 
@Entity
@Table(name = "compra")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class compra {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(nullable = false)
    private String producto;
 
    @Column(nullable = false)
    private Integer cantidad;
 
    @Column(nullable = false)
    private Double precio;
 
    @Column(name = "fecha_compra")
    private LocalDate fecha;
}