package vn.rikkei.exam.equipmentloan.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import vn.rikkei.exam.equipmentloan.service.chat.EquipmentService;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EquipmentTools {

    private final EquipmentService equipmentService;
    private final ToolExecutionTracker toolExecutionTracker;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Tool(name = "getEquipmentAvailability", description = "Kiểm tra tình trạng thiết bị CNTT còn khả dụng theo khoảng ngày. Yêu cầu startDate < endDate.")
    public String getEquipmentAvailability(
            @ToolParam(description = "Mã loại thiết bị CNTT: STD (Standard) hoặc PRM (Premium)") String resourceType,
            @ToolParam(description = "Ngày bắt đầu kiểm tra theo định dạng YYYY-MM-DD") LocalDate startDate,
            @ToolParam(description = "Ngày kết thúc kiểm tra theo định dạng YYYY-MM-DD") LocalDate endDate) {
        log.info("Tool executed: getEquipmentAvailability(resourceType={}, startDate={}, endDate={})",
                resourceType, startDate, endDate);
        toolExecutionTracker.record("getEquipmentAvailability");
        try {
            Map<String, Object> result = equipmentService.checkAvailability(resourceType, startDate, endDate);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("Error in getEquipmentAvailability tool: {}", e.getMessage());
            return "Lỗi kiểm tra tình trạng thiết bị: " + e.getMessage();
        }
    }

    @Tool(name = "createEquipmentLoanRequest", description = "Tạo yêu cầu mượn thiết bị CNTT ở trạng thái PENDING. Trả về requestId và bản tóm tắt yêu cầu.")
    public String createEquipmentLoanRequest(
            @ToolParam(description = "Mã người dùng (ví dụ: USR-001, USR-002)") String userId,
            @ToolParam(description = "Mã loại thiết bị: STD hoặc PRM") String resourceType,
            @ToolParam(description = "Ngày bắt đầu mượn theo định dạng YYYY-MM-DD") LocalDate startDate,
            @ToolParam(description = "Ngày kết thúc mượn theo định dạng YYYY-MM-DD") LocalDate endDate,
            @ToolParam(description = "Số lượng người tham gia sử dụng thiết bị") int participantCount,
            @ToolParam(description = "Mục đích mượn thiết bị (bắt buộc từ 10 đến 200 ký tự)") String purpose) {
        log.info("Tool executed: createEquipmentLoanRequest(userId={}, resourceType={}, {} to {}, participants={}, purpose={})",
                userId, resourceType, startDate, endDate, participantCount, purpose);
        toolExecutionTracker.record("createEquipmentLoanRequest");
        try {
            Map<String, Object> result = equipmentService.createLoanRequest(
                    userId, resourceType, startDate, endDate, participantCount, purpose);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("Error in createEquipmentLoanRequest tool: {}", e.getMessage());
            return "Lỗi tạo yêu cầu mượn thiết bị: " + e.getMessage();
        }
    }
}
