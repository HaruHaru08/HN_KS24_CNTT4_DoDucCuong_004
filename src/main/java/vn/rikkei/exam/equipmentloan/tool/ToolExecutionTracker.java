package vn.rikkei.exam.equipmentloan.tool;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class ToolExecutionTracker {
    private final ThreadLocal<List<String>> executedTools = ThreadLocal.withInitial(ArrayList::new);

    public void record(String toolName) {
        executedTools.get().add(toolName);
    }

    public List<String> getToolsUsed() {
        return new ArrayList<>(executedTools.get());
    }

    public void clear() {
        executedTools.get().clear();
    }
}
