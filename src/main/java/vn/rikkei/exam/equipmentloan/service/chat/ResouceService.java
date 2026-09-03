package vn.rikkei.exam.equipmentloan.service.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import vn.rikkei.exam.equipmentloan.model.ReservationRequest;
import vn.rikkei.exam.equipmentloan.model.ReservationStatus;
import vn.rikkei.exam.equipmentloan.model.ResourceType;
import vn.rikkei.exam.equipmentloan.tool.ToolExecutionTracker;

import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResouceService {

    private final EquipmentService equipmentService;
    private final ToolExecutionTracker toolExecutionTracker;

    @Tool(description = "Tình trạng thiết bị CNTT còn khả dụng theo khoảng ngày")
    public ReservationStatus getEquipmentAvailability(
            ResourceType resourceType, LocalDate startDate, LocalDate endDate) {
        toolExecutionTracker.record("getEquipmentAvailability");
        String code = resourceType != null ? resourceType.getResourceCode() : "STD";
        Map<String, Object> res = equipmentService.checkAvailability(code, startDate, endDate);
        if ("AVAILABLE".equals(res.get("overallStatus"))) {
            return ReservationStatus.PENDING;
        } else {
            return ReservationStatus.REJECTED;
        }
    }

    @Tool(description = "Tạo request PENDING, trả requestId và summary")
    public ReservationRequest createEquipmentLoanRequest(
            String userId, ResourceType resourceType, LocalDate startDate, LocalDate endDate,
            int participantCount, String purpose) {
        toolExecutionTracker.record("createEquipmentLoanRequest");
        String code = resourceType != null ? resourceType.getResourceCode() : "STD";
        Map<String, Object> res = equipmentService.createLoanRequest(userId, code, startDate, endDate, participantCount, purpose);
        return ReservationRequest.builder()
                .requestId((String) res.get("requestId"))
                .status(ReservationStatus.PENDING)
                .purpose(purpose)
                .participantCount(participantCount)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}
