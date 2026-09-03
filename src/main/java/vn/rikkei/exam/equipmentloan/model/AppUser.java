package vn.rikkei.exam.equipmentloan.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "app_users") @Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser { @Id @Column(name = "user_id") private String userId; private String fullName; private String department; }
