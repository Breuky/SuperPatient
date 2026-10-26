package com.superpatient.service;

import com.superpatient.dto.InsightRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 对话历史：要带上文，但不能无限灌——超长会被静默截断，避免成本失控
 */
class InsightHistoryTest {

    @Test
    void emptyHistoryStaysEmpty() {
        assertTrue(InsightService.trimmedHistory(null).isEmpty());
        assertTrue(InsightService.trimmedHistory(List.of()).isEmpty());
    }

    @Test
    void keepsOnlyLatestEightTurns() {
        List<InsightRequest.ChatTurn> history = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            history.add(turn("user", "问题" + i));
        }

        List<Map<String, String>> trimmed = InsightService.trimmedHistory(history);

        assertEquals(8, trimmed.size());
        assertEquals("问题4", trimmed.get(0).get("content"), "最早的几轮要被丢掉");
        assertEquals("问题11", trimmed.get(7).get("content"));
    }

    @Test
    void truncatesLongAnswersAndSkipsBlanks() {
        List<InsightRequest.ChatTurn> history = List.of(
                turn("assistant", "x".repeat(900)),
                turn("user", "   "),
                turn("system", "不该被透传"));

        List<Map<String, String>> trimmed = InsightService.trimmedHistory(history);

        assertEquals(2, trimmed.size(), "空白内容被跳过");
        assertEquals(600, trimmed.get(0).get("content").length());
        assertEquals("user", trimmed.get(1).get("role"), "未知角色一律当作用户输入");
    }

    private InsightRequest.ChatTurn turn(String role, String content) {
        InsightRequest.ChatTurn t = new InsightRequest.ChatTurn();
        t.setRole(role);
        t.setContent(content);
        return t;
    }
}
