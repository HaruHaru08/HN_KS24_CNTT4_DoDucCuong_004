package vn.rikkei.exam.equipmentloan.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "resource_types") @Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ResourceType { @Id @Column(name = "resource_code") private String resourceCode; private String displayName; private Integer maxParticipants; private Boolean active; }
