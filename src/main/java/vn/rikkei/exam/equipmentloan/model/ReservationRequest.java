package vn.rikkei.exam.equipmentloan.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Table(name = "reservation_requests") @Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReservationRequest { @Id private String requestId; @ManyToOne @JoinColumn(name = "user_id") private AppUser requester; @ManyToOne @JoinColumn(name = "resource_code") private ResourceType resourceType; private LocalDate startDate; private LocalDate endDate; private Integer participantCount; private String purpose; @Enumerated(EnumType.STRING) private ReservationStatus status; private String decisionNote; private Instant createdAt; private Instant updatedAt; }
