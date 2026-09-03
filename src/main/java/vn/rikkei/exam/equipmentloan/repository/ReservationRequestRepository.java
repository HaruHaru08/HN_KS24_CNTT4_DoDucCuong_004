package vn.rikkei.exam.equipmentloan.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.rikkei.exam.equipmentloan.model.ReservationRequest;
public interface ReservationRequestRepository extends JpaRepository<ReservationRequest, String> { }
