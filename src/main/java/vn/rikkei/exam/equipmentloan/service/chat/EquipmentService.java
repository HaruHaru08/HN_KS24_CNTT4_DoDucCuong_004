package vn.rikkei.exam.equipmentloan.service.chat;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.rikkei.exam.equipmentloan.model.*;
import vn.rikkei.exam.equipmentloan.repository.AppUserRepository;
import vn.rikkei.exam.equipmentloan.repository.ReservationRequestRepository;
import vn.rikkei.exam.equipmentloan.repository.ResourceInventoryRepository;
import vn.rikkei.exam.equipmentloan.repository.ResourceTypeRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final AppUserRepository appUserRepository;
    private final ResourceTypeRepository resourceTypeRepository;
    private final ResourceInventoryRepository resourceInventoryRepository;
    private final ReservationRequestRepository reservationRequestRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> checkAvailability(String resourceTypeStr, LocalDate startDate, LocalDate endDate) {
        log.info("Checking availability for resource: {}, from: {} to: {}", resourceTypeStr, startDate, endDate);

        if (resourceTypeStr == null || resourceTypeStr.isBlank()) {
            throw new IllegalArgumentException("Loại thiết bị (resourceType) không được để trống");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc không được để trống");
        }
        if (!startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu (" + startDate + ") phải trước ngày kết thúc (" + endDate + ")");
        }

        ResourceType resourceType = resolveResourceType(resourceTypeStr);
        List<ResourceInventory> inventories = resourceInventoryRepository
                .findByResourceType_ResourceCodeAndAvailableDateBetween(resourceType.getResourceCode(), startDate, endDate);

        Map<LocalDate, Integer> slotsMap = inventories.stream()
                .collect(Collectors.toMap(ResourceInventory::getAvailableDate, ResourceInventory::getAvailableSlots, (a, b) -> a));

        boolean allAvailable = true;
        List<Map<String, Object>> dailyDetails = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Integer slots = slotsMap.getOrDefault(date, 0);
            boolean available = slots > 0;
            if (!available) {
                allAvailable = false;
            }
            Map<String, Object> dayInfo = new HashMap<>();
            dayInfo.put("date", date.toString());
            dayInfo.put("availableSlots", slots);
            dayInfo.put("status", available ? "AVAILABLE" : "UNAVAILABLE");
            dailyDetails.add(dayInfo);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("resourceCode", resourceType.getResourceCode());
        result.put("displayName", resourceType.getDisplayName());
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("overallStatus", allAvailable ? "AVAILABLE" : "UNAVAILABLE");
        result.put("dailyDetails", dailyDetails);
        result.put("summary", allAvailable
                ? "Thiết bị " + resourceType.getDisplayName() + " còn khả dụng đầy đủ từ ngày " + startDate + " đến " + endDate
                : "Thiết bị " + resourceType.getDisplayName() + " không đủ khả dụng trong toàn bộ khoảng thời gian từ " + startDate + " đến " + endDate);

        return result;
    }

    @Transactional
    public Map<String, Object> createLoanRequest(String userId, String resourceTypeStr,
                                                LocalDate startDate, LocalDate endDate,
                                                int participantCount, String purpose) {
        log.info("Creating loan request: user={}, resource={}, {} to {}, participants={}, purpose={}",
                userId, resourceTypeStr, startDate, endDate, participantCount, purpose);

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Mã người dùng (userId) không được để trống");
        }
        AppUser requester = appUserRepository.findById(userId.trim())
                .orElseThrow(() -> new EntityNotFoundException("Người dùng không tồn tại trong hệ thống: " + userId));

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc không được để trống");
        }
        if (!startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu (" + startDate + ") phải trước ngày kết thúc (" + endDate + ")");
        }
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 14) {
            throw new IllegalArgumentException("Thời gian mượn tối đa là 14 ngày (bạn đang yêu cầu: " + daysBetween + " ngày)");
        }

        ResourceType resourceType = resolveResourceType(resourceTypeStr);

        if (participantCount <= 0) {
            throw new IllegalArgumentException("Số lượng người tham gia phải lớn hơn 0");
        }
        if (participantCount > resourceType.getMaxParticipants()) {
            throw new IllegalArgumentException("Số người tham gia (" + participantCount + ") vượt quá sức chứa tối đa của "
                    + resourceType.getDisplayName() + " (tối đa " + resourceType.getMaxParticipants() + " người)");
        }

        if (isPremium(resourceType) && participantCount < 2) {
            throw new IllegalArgumentException("Nhóm thiết bị PREMIUM yêu cầu tối thiểu 2 người tham gia");
        }

        if (purpose == null || purpose.trim().length() < 10 || purpose.trim().length() > 200) {
            int len = purpose == null ? 0 : purpose.trim().length();
            throw new IllegalArgumentException("Mục đích mượn thiết bị phải có độ dài từ 10 đến 200 ký tự (hiện tại: " + len + " ký tự)");
        }

        String requestId = "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ReservationRequest request = ReservationRequest.builder()
                .requestId(requestId)
                .requester(requester)
                .resourceType(resourceType)
                .startDate(startDate)
                .endDate(endDate)
                .participantCount(participantCount)
                .purpose(purpose.trim())
                .status(ReservationStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        reservationRequestRepository.save(request);

        String summary = String.format("Yêu cầu mượn thiết bị đã tạo thành công ở trạng thái PENDING. " +
                        "Mã yêu cầu: %s, Người mượn: %s (%s), Thiết bị: %s, Thời gian: %s đến %s (%d ngày), Số người: %d, Mục đích: %s",
                requestId, requester.getFullName(), requester.getUserId(),
                resourceType.getDisplayName(), startDate, endDate, daysBetween, participantCount, purpose.trim());

        Map<String, Object> response = new HashMap<>();
        response.put("requestId", requestId);
        response.put("status", ReservationStatus.PENDING.name());
        response.put("summary", summary);
        return response;
    }

    @Transactional
    public ReservationRequest processApproval(String requestId, String decision, String note) {
        log.info("Processing approval: requestId={}, decision={}, note={}", requestId, decision, note);

        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId không được để trống");
        }
        if (decision == null || decision.isBlank()) {
            throw new IllegalArgumentException("decision không được để trống");
        }

        String normalizedDecision = decision.trim().toUpperCase();
        if (!normalizedDecision.equals("APPROVE") && !normalizedDecision.equals("REJECT")) {
            throw new IllegalArgumentException("decision phải là APPROVE hoặc REJECT");
        }

        ReservationRequest request = reservationRequestRepository.findById(requestId.trim())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy yêu cầu mượn thiết bị với mã: " + requestId));

        if (request.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Chỉ yêu cầu ở trạng thái PENDING mới được xử lý. Trạng thái hiện tại: " + request.getStatus());
        }

        if (normalizedDecision.equals("APPROVE")) {
            if (!request.getStartDate().isBefore(request.getEndDate())) {
                throw new IllegalStateException("Ngày bắt đầu không hợp lệ trước ngày kết thúc");
            }
            long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
            if (days > 14) {
                throw new IllegalStateException("Khoảng thời gian mượn vượt quá 14 ngày (" + days + " ngày)");
            }

            ResourceType type = request.getResourceType();
            if (request.getParticipantCount() > type.getMaxParticipants()) {
                throw new IllegalStateException("Số người tham gia vượt quá sức chứa tối đa (" + type.getMaxParticipants() + ")");
            }
            if (isPremium(type) && request.getParticipantCount() < 2) {
                throw new IllegalStateException("Nhóm PREMIUM yêu cầu tối thiểu 2 người");
            }

            List<ResourceInventory> inventories = resourceInventoryRepository
                    .findByResourceType_ResourceCodeAndAvailableDateBetween(
                            type.getResourceCode(), request.getStartDate(), request.getEndDate());
            Map<LocalDate, Integer> slotsMap = inventories.stream()
                    .collect(Collectors.toMap(ResourceInventory::getAvailableDate, ResourceInventory::getAvailableSlots, (a, b) -> a));

            for (LocalDate date = request.getStartDate(); !date.isAfter(request.getEndDate()); date = date.plusDays(1)) {
                int slots = slotsMap.getOrDefault(date, 0);
                if (slots <= 0) {
                    throw new IllegalStateException("Không thể phê duyệt vì thiết bị không còn khả dụng vào ngày " + date);
                }
            }

            request.setStatus(ReservationStatus.APPROVED);
        } else {
            request.setStatus(ReservationStatus.REJECTED);
        }

        request.setDecisionNote(note != null ? note.trim() : null);
        request.setUpdatedAt(Instant.now());
        return reservationRequestRepository.save(request);
    }

    private ResourceType resolveResourceType(String codeOrName) {
        String trimmed = codeOrName.trim();
        Optional<ResourceType> byId = resourceTypeRepository.findById(trimmed.toUpperCase());
        if (byId.isPresent()) {
            return byId.get();
        }
        if (trimmed.toUpperCase().contains("PREMIUM") || trimmed.toUpperCase().contains("PRM")) {
            return resourceTypeRepository.findById("PRM")
                    .orElseThrow(() -> new EntityNotFoundException("Loại thiết bị PRM không tồn tại"));
        }
        if (trimmed.toUpperCase().contains("STANDARD") || trimmed.toUpperCase().contains("STD")) {
            return resourceTypeRepository.findById("STD")
                    .orElseThrow(() -> new EntityNotFoundException("Loại thiết bị STD không tồn tại"));
        }
        throw new EntityNotFoundException("Không tìm thấy loại thiết bị tương ứng với: " + codeOrName);
    }

    private boolean isPremium(ResourceType type) {
        return "PRM".equalsIgnoreCase(type.getResourceCode())
                || (type.getDisplayName() != null && type.getDisplayName().toUpperCase().contains("PREMIUM"));
    }
}
