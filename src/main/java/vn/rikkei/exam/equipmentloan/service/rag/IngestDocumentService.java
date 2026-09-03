package vn.rikkei.exam.equipmentloan.service.rag;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestDocumentService {

    private final VectorStore vectorStore;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            log.info("Starting automatic corpus ingestion into PgVectorStore...");
            ingestDocument();
        } catch (Exception e) {
            log.warn("Corpus auto-ingestion skipped or failed (check DB connection / pgvector): {}", e.getMessage());
        }
    }

    /**
     * Chunk tài liệu nội bộ theo từng mục chính sách có ý nghĩa,
     * gán metadata nguồn để truy vết và sinh deterministic ID chống nạp trùng.
     */
    public String ingestDocument() {
        Resource resource = new ClassPathResource("tai_lieu_noi_bo.md");
        if (!resource.exists()) {
            throw new IllegalStateException("File tai_lieu_noi_bo.md không tồn tại trong classpath");
        }

        List<Document> documents = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String fullText = reader.lines().collect(Collectors.joining("\n"));

            // Tách theo từng mục Markdown (bắt đầu bằng ##)
            String[] sections = fullText.split("(?=^##\\s+)");
            for (String section : sections) {
                String trimmed = section.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("# Sổ tay nội bộ")) {
                    continue;
                }

                // Lấy tiêu đề mục chính sách từ dòng đầu tiên
                String[] lines = trimmed.split("\n", 2);
                String headerLine = lines[0].replace("##", "").trim();
                String content = trimmed;

                // Deterministic UUID để chống nạp trùng khi ingest lại nhiều lần
                String deterministicId = UUID.nameUUIDFromBytes(
                        ("tai_lieu_noi_bo.md:" + headerLine).getBytes(StandardCharsets.UTF_8)
                ).toString();

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("source", "tai_lieu_noi_bo.md");
                metadata.put("section", headerLine);
                metadata.put("title", headerLine);

                Document doc = new Document(deterministicId, content, metadata);
                documents.add(doc);
                log.info("Created chunk: id={}, section={}, charCount={}", deterministicId, headerLine, content.length());
            }

            if (!documents.isEmpty()) {
                vectorStore.accept(documents);
                log.info("Successfully ingested {} meaningful chunks into VectorStore", documents.size());
            }
            return "Documents ingested successfully with " + documents.size() + " sections.";
        } catch (Exception e) {
            log.error("Error ingesting corpus: ", e);
            throw new RuntimeException("Lỗi nạp tài liệu vào vector store: " + e.getMessage(), e);
        }
    }
}
