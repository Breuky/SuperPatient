package com.superpatient.service;

import com.superpatient.dto.ReferenceRangeRequest;
import com.superpatient.dto.ReferenceRangeVO;
import com.superpatient.entity.ReferenceRange;
import com.superpatient.entity.User;
import com.superpatient.mapper.ReferenceRangeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 参考区间解析。
 * 优先级：报告印刷区间（exam_value.ref_*） &gt; 规则表命中 &gt; 指标默认值。
 * 这里只负责"规则表命中 → 指标默认值"这一段，报告区间由调用方先判断。
 */
@Service
@RequiredArgsConstructor
public class ReferenceRangeService {

    private final ReferenceRangeMapper referenceRangeMapper;

    /** 解析结果 */
    public record Resolved(BigDecimal min, BigDecimal max, String text, String source) {
    }

    /** 用户上下文：性别与年龄（年龄算不出时为 null） */
    public record UserContext(Integer gender, Integer age) {
        public static UserContext of(User user) {
            if (user == null) return new UserContext(null, null);
            Integer age = user.getBirthDate() == null ? null
                    : (int) ChronoUnit.YEARS.between(user.getBirthDate(), LocalDate.now());
            return new UserContext(user.getGender(), age);
        }
    }

    public Resolved resolve(Integer indicatorId, UserContext ctx) {
        return match(referenceRangeMapper.findByIndicatorId(indicatorId), ctx);
    }

    public Map<Integer, Resolved> resolveBatch(Collection<Integer> indicatorIds, UserContext ctx) {
        Map<Integer, Resolved> result = new HashMap<>();
        if (indicatorIds == null || indicatorIds.isEmpty()) return result;

        Map<Integer, List<ReferenceRange>> grouped = new HashMap<>();
        for (ReferenceRange r : referenceRangeMapper.findByIndicatorIds(indicatorIds)) {
            grouped.computeIfAbsent(r.getIndicatorId(), k -> new java.util.ArrayList<>()).add(r);
        }
        grouped.forEach((id, rules) -> {
            Resolved resolved = match(rules, ctx);
            if (resolved != null) result.put(id, resolved);
        });
        return result;
    }

    /**
     * 纯匹配逻辑：性别/年龄都需命中，取优先级最高的一条；
     * 同优先级时，条件越具体（性别明确 &gt; 年龄限定）越优先，再比年龄区间更窄。
     */
    static Resolved match(List<ReferenceRange> rules, UserContext ctx) {
        if (rules == null || rules.isEmpty() || ctx == null) return null;

        ReferenceRange best = null;
        for (ReferenceRange rule : rules) {
            if (!genderMatches(rule, ctx.gender()) || !ageMatches(rule, ctx.age())) {
                continue;
            }
            if (best == null || compare(rule, best) > 0) {
                best = rule;
            }
        }
        if (best == null) return null;
        return new Resolved(best.getRefMin(), best.getRefMax(), best.getRefText(), best.getSource());
    }

    private static boolean genderMatches(ReferenceRange rule, Integer gender) {
        return rule.getGender() == null || (gender != null && rule.getGender().equals(gender));
    }

    private static boolean ageMatches(ReferenceRange rule, Integer age) {
        boolean unbounded = rule.getAgeMin() == null && rule.getAgeMax() == null;
        if (unbounded) return true;
        // 规则限定了年龄但患者没有出生日期时，宁可不用这条规则
        if (age == null) return false;
        if (rule.getAgeMin() != null && age < rule.getAgeMin()) return false;
        return rule.getAgeMax() == null || age <= rule.getAgeMax();
    }

    private static int compare(ReferenceRange a, ReferenceRange b) {
        int sa = specificity(a);
        int sb = specificity(b);
        if (sa != sb) return Integer.compare(sa, sb);
        return Integer.compare(ageSpanScore(b), ageSpanScore(a)); // 区间更窄的优先
    }

    private static int specificity(ReferenceRange rule) {
        int score = rule.getPriority() == null ? 0 : rule.getPriority();
        if (rule.getGender() != null) score += 100;
        if (rule.getAgeMin() != null || rule.getAgeMax() != null) score += 50;
        return score;
    }

    private static int ageSpanScore(ReferenceRange rule) {
        if (rule.getAgeMin() == null || rule.getAgeMax() == null) return Integer.MIN_VALUE;
        return rule.getAgeMax() - rule.getAgeMin();
    }

    // ---------------- 规则维护 ----------------

    public List<ReferenceRangeVO> list(Integer indicatorId) {
        return referenceRangeMapper.findByIndicatorId(indicatorId).stream().map(this::toVO).toList();
    }

    @Transactional
    public ReferenceRangeVO create(ReferenceRangeRequest request) {
        ReferenceRange range = new ReferenceRange();
        range.setIndicatorId(request.getIndicatorId());
        applyRequest(range, request);
        referenceRangeMapper.insert(range);
        return toVO(referenceRangeMapper.findById(range.getId()));
    }

    @Transactional
    public boolean update(Integer id, ReferenceRangeRequest request) {
        ReferenceRange range = new ReferenceRange();
        range.setId(id);
        applyRequest(range, request);
        return referenceRangeMapper.update(range) > 0;
    }

    @Transactional
    public boolean delete(Integer id) {
        return referenceRangeMapper.deleteById(id) > 0;
    }

    private void applyRequest(ReferenceRange range, ReferenceRangeRequest request) {
        range.setGender(request.getGender());
        range.setAgeMin(request.getAgeMin());
        range.setAgeMax(request.getAgeMax());
        range.setRefMin(request.getRefMin());
        range.setRefMax(request.getRefMax());
        range.setRefText(request.getRefText());
        range.setSource(request.getSource());
        range.setPriority(request.getPriority() == null ? 0 : request.getPriority());
    }

    private ReferenceRangeVO toVO(ReferenceRange r) {
        ReferenceRangeVO vo = new ReferenceRangeVO();
        vo.setId(r.getId());
        vo.setIndicatorId(r.getIndicatorId());
        vo.setGender(r.getGender());
        vo.setAgeMin(r.getAgeMin());
        vo.setAgeMax(r.getAgeMax());
        vo.setRefMin(r.getRefMin());
        vo.setRefMax(r.getRefMax());
        vo.setRefText(r.getRefText());
        vo.setSource(r.getSource());
        vo.setPriority(r.getPriority());
        return vo;
    }
}
