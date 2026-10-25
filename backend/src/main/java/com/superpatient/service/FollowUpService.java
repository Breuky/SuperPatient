package com.superpatient.service;

import com.superpatient.dto.FollowUpSource;
import com.superpatient.dto.FollowUpVO;
import com.superpatient.entity.FollowUp;
import com.superpatient.mapper.FollowUpMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 随访提醒：全部由用户自己的历史数据推导，不调用模型，也不给医疗建议。
 *
 * 规则：
 *  1. 最近一次结果异常，就生成提醒——不必等到期，到期只是紧迫度不同
 *  2. 至少有 2 次记录，才能算出「既往间隔中位数」
 *  3. 连续 2 次异常 → 间隔减半，标记为高优先级
 *  4. 指标有了更新的检查、或最新结果恢复正常 → 旧待办自动关闭
 *
 * 到期与否由 due_date 与今天的差值表达（overdueDays 为正=已超期，为负=还剩几天），
 * 不在服务端做拦截。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpService {

    /** 计算间隔时最多回看几次 */
    private static final int MAX_HISTORY = 6;

    private final FollowUpMapper followUpMapper;

    public List<FollowUpVO> list(Long userId) {
        refresh(userId);
        return followUpMapper.findPending(userId);
    }

    @Transactional
    public boolean updateStatus(Long id, String status, Long userId) {
        if (!"done".equals(status) && !"dismissed".equals(status) && !"pending".equals(status)) {
            return false;
        }
        return followUpMapper.updateStatus(id, userId, status) > 0;
    }

    /**
     * 重算待办。幂等：同一 (指标, 到期日) 只会有一条，已忽略的不会被复活。
     */
    @Transactional
    public void refresh(Long userId) {
        List<FollowUpSource> rows = followUpMapper.findHistory(userId);
        if (rows == null || rows.isEmpty()) return;

        Map<Integer, List<FollowUpSource>> grouped = new LinkedHashMap<>();
        for (FollowUpSource row : rows) {
            grouped.computeIfAbsent(row.getIndicatorId(), k -> new ArrayList<>()).add(row);
        }

        for (Map.Entry<Integer, List<FollowUpSource>> entry : grouped.entrySet()) {
            List<FollowUpSource> history = entry.getValue();   // SQL 已按日期倒序
            FollowUpSource latest = history.get(0);

            // 不再满足条件（最新一次正常 / 记录被删到不足两次）→ 关掉遗留待办
            if (latest.getIsAbnormal() == null || latest.getIsAbnormal() == 0 || history.size() < 2) {
                followUpMapper.closeAllPending(userId, entry.getKey());
                continue;
            }

            // 有了更新的检查，旧待办就该结束
            followUpMapper.closeStale(userId, entry.getKey(), latest.getExamDate());

            int interval = medianIntervalDays(history);
            if (interval <= 0) continue;

            int consecutive = leadingAbnormalCount(history);
            int priority = 0;
            if (consecutive >= 2) {
                interval = Math.max(1, interval / 2);
                priority = 1;
            }

            FollowUp followUp = new FollowUp();
            followUp.setUserId(userId);
            followUp.setIndicatorId(entry.getKey());
            followUp.setLastExamDate(latest.getExamDate());
            followUp.setIntervalDays(interval);
            followUp.setDueDate(latest.getExamDate().plusDays(interval));
            followUp.setPriority(priority);
            followUp.setReason(buildReason(history, interval, consecutive));

            followUpMapper.upsert(followUp);
            log.debug("生成随访提醒: userId={} indicator={} due={}", userId, entry.getKey(), followUp.getDueDate());
        }
    }

    /** 相邻两次检查间隔的中位数（天） */
    static int medianIntervalDays(List<FollowUpSource> history) {
        int limit = Math.min(history.size() - 1, MAX_HISTORY - 1);
        if (limit <= 0) return 0;

        List<Long> gaps = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            LocalDate newer = history.get(i).getExamDate();
            LocalDate older = history.get(i + 1).getExamDate();
            if (newer == null || older == null) continue;
            long days = ChronoUnit.DAYS.between(older, newer);
            if (days > 0) gaps.add(days);
        }
        if (gaps.isEmpty()) return 0;

        gaps.sort(Long::compareTo);
        int size = gaps.size();
        long median = size % 2 == 1
                ? gaps.get(size / 2)
                : Math.round((gaps.get(size / 2 - 1) + gaps.get(size / 2)) / 2.0);
        return (int) median;
    }

    /** 从最近一次往前数，连续异常的次数 */
    static int leadingAbnormalCount(List<FollowUpSource> history) {
        int count = 0;
        for (FollowUpSource row : history) {
            if (row.getIsAbnormal() == null || row.getIsAbnormal() == 0) break;
            count++;
        }
        return count;
    }

    private String buildReason(List<FollowUpSource> history, int interval, int consecutive) {
        int window = Math.min(history.size(), 5);
        int abnormal = 0;
        for (int i = 0; i < window; i++) {
            if (history.get(i).getIsAbnormal() != null && history.get(i).getIsAbnormal() != 0) abnormal++;
        }
        String direction = history.get(0).getIsAbnormal() == 2 ? "偏低" : "偏高";
        String basis = "近 " + window + " 次里有 " + abnormal + " 次" + direction;
        if (consecutive >= 2) basis += "（连续 " + consecutive + " 次）";
        return basis + "；你既往的复查间隔中位是 " + interval + " 天";
    }
}
