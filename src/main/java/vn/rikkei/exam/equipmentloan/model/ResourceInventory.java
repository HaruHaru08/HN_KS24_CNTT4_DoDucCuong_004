package vn.rikkei.exam.equipmentloan.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
@Entity @Table(name = "resource_inventory", uniqueConstraints = @UniqueConstraint(columnNames = {"resource_code", "availableDate"})) @Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ResourceInventory { @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id; @ManyToOne @JoinColumn(name = "resource_code") private ResourceType resourceType; private LocalDate availableDate; private Integer availableSlots; }
