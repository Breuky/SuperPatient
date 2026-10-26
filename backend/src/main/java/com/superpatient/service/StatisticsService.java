package com.superpatient.service;

import com.superpatient.dto.OverviewVO;
import com.superpatient.mapper.RecordMapper;
import com.superpatient.util.CurrentHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 只读统计：范围永远是当前 token 对应的用户 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private static final int TOP_ABNORMAL = 5;

    private final RecordMapper recordMapper;

    public OverviewVO overview() {
        Long userId = CurrentHolder.getCurrentId();
        OverviewVO vo = recordMapper.overviewBase(userId);
        if (vo == null) {
            vo = new OverviewVO();
        }
        vo.setCategories(recordMapper.countByCategory(userId));
        vo.setTopAbnormal(recordMapper.topAbnormal(userId, TOP_ABNORMAL));
        return vo;
    }
}
