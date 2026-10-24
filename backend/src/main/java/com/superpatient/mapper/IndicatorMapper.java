package com.superpatient.mapper;

import com.superpatient.entity.ExamIndicator;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IndicatorMapper {
    /** 当前用户可见的指标：公共 + 自己的私有 */
    List<ExamIndicator> findVisibleByCategory(@Param("categoryId") Integer categoryId,
                                              @Param("userId") Long userId);

    /** 当前用户可见的全部启用指标 */
    List<ExamIndicator> findAllEnabled(@Param("userId") Long userId);

    /** 不带归属过滤，仅用于渲染历史数据（值已经落在库里了，指标被禁用也要能显示名字） */
    ExamIndicator findById(@Param("id") Integer id);
    int insert(ExamIndicator indicator);
    int update(ExamIndicator indicator);
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);
    int deleteById(@Param("id") Integer id);
    int countEnabled();

    /** 同分类下是否已有同名指标（公共或本人私有），用于创建/改名时给出可读的错误 */
    boolean existsByName(@Param("categoryId") Integer categoryId,
                         @Param("name") String name,
                         @Param("userId") Long userId,
                         @Param("excludeId") Integer excludeId);
    boolean existsValuesByIndicatorId(@Param("indicatorId") Integer indicatorId);
}
