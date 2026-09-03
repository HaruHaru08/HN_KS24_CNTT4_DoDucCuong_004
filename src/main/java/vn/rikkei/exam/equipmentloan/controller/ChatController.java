package vn.rikkei.exam.equipmentloan.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.rikkei.exam.equipmentloan.model.ReservationRequest;
import vn.rikkei.exam.equipmentloan.service.chat.EquipmentService;
import vn.rikkei.exam.equipmentloan.service.rag.IngestDocumentService;
import vn.rikkei.exam.equipmentloan.tool.ToolExecutionTracker;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final EquipmentService equipmentService;
    private final IngestDocumentService ingestDocumentService;
    private final ToolExecutionTracker toolExecutionTracker;

    private static final String FALLBACK_MESSAGE = "Không đủ căn cứ trong tài liệu nội bộ";

    @PostMapping("/assistant/ask")
    public ResponseEntity<Map<String, Object>> askQuestion(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) String userMessage,
            @RequestParam(required = false) String conversationId) {

        String message = null;
        String convId = conversationId;

        if (body != null) {
            if (body.get("message") != null) {
                message = body.get("message").toString();
            } else if (body.get("userMessage") != null) {
                message = body.get("userMessage").toString();
            }
            if (convId == null && body.get("conversationId") != null) {
                convId = body.get("conversationId").toString();
            }
        }

        if (message == null && userMessage != null) {
            message = userMessage;
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống");
        }

        log.info("Received ask request with message: '{}', conversationId: '{}'", message, convId);

        toolExecutionTracker.clear();

        if (convId == null || convId.isBlank()) {
            convId = UUID.randomUUID().toString();
        }
        final String effectiveConvId = convId;

        List<Document> similarDocuments = Collections.emptyList();
        List<String> sources = new ArrayList<>();
        StringBuilder contextBuilder = new StringBuilder();

        try {
            similarDocuments = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(message)
                            .topK(3)
                            .similarityThreshold(0.5)
                            .build()
            );

            if (similarDocuments != null) {
                for (Document doc : similarDocuments) {
                    Object sourceObj = doc.getMetadata().get("source");
                    Object sectionObj = doc.getMetadata().get("section");
                    String sourceLabel = (sourceObj != null ? sourceObj.toString() : "tai_lieu_noi_bo.md");
                    if (sectionObj != null && !sectionObj.toString().isBlank()) {
                        sourceLabel += " (" + sectionObj + ")";
                    }
                    if (!sources.contains(sourceLabel)) {
                        sources.add(sourceLabel);
                    }
                    contextBuilder.append("\n[ĐOẠN TRÍCH TÀI LIỆU NỘI BỘ]:\n").append(doc.getText()).append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("Vector search failed or vectorstore empty: {}", e.getMessage());
        }

        final String retrievedContext = contextBuilder.toString();
        String userPrompt = message;
        if (!retrievedContext.isBlank()) {
            userPrompt = "Tài liệu tham chiếu nội bộ:\n" + retrievedContext + "\n\nCâu hỏi/Yêu cầu của người dùng:\n" + message;
        }

        String rawAnswer;
        try {
            rawAnswer = chatClient.prompt()
                    .user(userPrompt)
                    .advisors(advisorSpec -> advisorSpec.param("chat_memory_conversation_id", effectiveConvId))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Error calling chat client: ", e);
            rawAnswer = "Đã xảy ra lỗi khi trao đổi với trợ lý ảo: " + e.getMessage();
        }

        List<String> toolsUsed = toolExecutionTracker.getToolsUsed();

        String finalAnswer = (rawAnswer != null) ? rawAnswer.trim() : "";
        if (toolsUsed.isEmpty() && sources.isEmpty()) {
            finalAnswer = FALLBACK_MESSAGE;
        } else if (finalAnswer.contains("Không đủ căn cứ trong tài liệu nội bộ") ||
                (finalAnswer.toLowerCase().contains("không có thông tin") && toolsUsed.isEmpty())) {
            finalAnswer = FALLBACK_MESSAGE;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("answer", finalAnswer);
        response.put("conversationId", effectiveConvId);
        response.put("sources", sources);
        response.put("toolsUsed", toolsUsed);

        log.info("Completed ask response: conversationId={}, sourcesCount={}, toolsUsed={}",
                effectiveConvId, sources.size(), toolsUsed);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/operations/approve-request")
    public ResponseEntity<Map<String, Object>> approveRequest(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String decision,
            @RequestParam(required = false) String note) {

        String reqId = requestId;
        String dec = decision;
        String decisionNote = note;

        if (body != null) {
            if (body.get("requestId") != null) reqId = body.get("requestId").toString();
            if (body.get("decision") != null) dec = body.get("decision").toString();
            if (body.get("note") != null) decisionNote = body.get("note").toString();
        }

        log.info("Operation approve request received: requestId={}, decision={}, note={}", reqId, dec, decisionNote);

        ReservationRequest updatedRequest = equipmentService.processApproval(reqId, dec, decisionNote);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requestId", updatedRequest.getRequestId());
        response.put("status", updatedRequest.getStatus().name());
        response.put("decisionNote", updatedRequest.getDecisionNote());
        response.put("updatedAt", updatedRequest.getUpdatedAt() != null ? updatedRequest.getUpdatedAt().toString() : null);
        response.put("message", "Xử lý yêu cầu thành công. Trạng thái mới: " + updatedRequest.getStatus());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/rag/ingest")
    public ResponseEntity<Map<String, String>> triggerIngest() {
        String result = ingestDocumentService.ingestDocument();
        return ResponseEntity.ok(Map.of("message", result));
    }
}
