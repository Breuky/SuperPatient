package com.superpatient.mapper;

import com.superpatient.dto.FollowUpSource;
import com.superpatient.dto.FollowUpVO;
import com.superpatient.entity.FollowUp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface FollowUpMapper {

    /** 用户可见指标的全部数值历史，用于推导复查间隔 */
    List<FollowUpSource> findHistory(@Param("userId") Long userId);

    /** 同一 (用户, 指标, 到期日) 只保留一条；已 dismissed 的不复活 */
    int upsert(FollowUp followUp);

    /** 指标不再满足提醒条件（如最新结果恢复正常）时，关掉它的待办 */
    int closeAllPending(@Param("userId") Long userId, @Param("indicatorId") Integer indicatorId);

    /** 指标有了更新的检查后，把旧的待办标记为已完成 */
    int closeStale(@Param("userId") Long userId,
                   @Param("indicatorId") Integer indicatorId,
                   @Param("lastExamDate") LocalDate lastExamDate);

    List<FollowUpVO> findPending(@Param("userId") Long userId);

    int updateStatus(@Param("id") Long id,
                     @Param("userId") Long userId,
                     @Param("status") String status);
}
