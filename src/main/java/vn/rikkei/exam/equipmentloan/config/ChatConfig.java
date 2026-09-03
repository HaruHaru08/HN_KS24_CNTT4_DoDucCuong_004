package vn.rikkei.exam.equipmentloan.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.rikkei.exam.equipmentloan.tool.EquipmentTools;

@Configuration
public class ChatConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(10)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder,
                                 ChatMemory chatMemory,
                                 EquipmentTools equipmentTools) {
        return chatClientBuilder
                .defaultSystem("""
                        Bạn là trợ lý ảo AI chuyên hỗ trợ mượn thiết bị CNTT theo quy định nội bộ của tổ chức.
                        
                        NGUYÊN TẮC HOẠT ĐỘNG:
                        1. Đối với các câu hỏi về quy định, chính sách, tiêu chuẩn mượn thiết bị:
                           - Dựa vào nội dung tài liệu nội bộ được cung cấp trong ngữ cảnh.
                        
                        2. Đối với các thao tác tra cứu dữ liệu nghiệp vụ thực tế hoặc tạo yêu cầu:
                           - BẮT BUỘC gọi các Java Service qua @Tool được cung cấp:
                             + 'getEquipmentAvailability': tra cứu tình trạng còn khả dụng theo khoảng ngày.
                             + 'createEquipmentLoanRequest': tạo yêu cầu mượn thiết bị ở trạng thái PENDING.
                           - Tuyệt đối không tự suy đoán số liệu tồn kho hoặc tạo dữ liệu giả.
                        
                        3. QUY TẮC BẮT BUỘC VỀ FALLBACK:
                           - Nếu câu hỏi của người dùng không có căn cứ trong tài liệu nội bộ và cũng không có công cụ (Tool) phù hợp để xử lý, bạn BẮT BUỘC PHẢI TRẢ LỜI CHÍNH XÁC NGUYÊN VĂN:
                             "Không đủ căn cứ trong tài liệu nội bộ"
                           - Tuyệt đối không bịa đặt, không suy diễn thêm thông tin ngoài tài liệu nội bộ.
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultTools(equipmentTools)
                .build();
    }
}
