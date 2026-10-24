package com.superpatient.service;

import com.superpatient.dto.CategoryCreateRequest;
import com.superpatient.dto.CategoryUpdateRequest;
import com.superpatient.dto.CategoryVO;
import com.superpatient.dto.IndicatorVO;
import com.superpatient.entity.ExamCategory;
import com.superpatient.entity.ExamIndicator;
import com.superpatient.mapper.CategoryMapper;
import com.superpatient.mapper.IndicatorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final IndicatorMapper indicatorMapper;

    public List<CategoryVO> listWithIndicators(Long userId) {
        List<ExamCategory> categories = categoryMapper.findAll();
        List<ExamIndicator> allIndicators = indicatorMapper.findAllEnabled(userId);

        Map<Integer, List<ExamIndicator>> indicatorMap = allIndicators.stream()
                .collect(Collectors.groupingBy(ExamIndicator::getCategoryId));

        List<CategoryVO> result = new ArrayList<>();
        for (ExamCategory cat : categories) {
            CategoryVO vo = new CategoryVO();
            vo.setId(cat.getId());
            vo.setName(cat.getName());
            vo.setIcon(cat.getIcon());
            vo.setSortOrder(cat.getSortOrder());

            List<ExamIndicator> catIndicators = indicatorMap.getOrDefault(cat.getId(), List.of());
            vo.setIndicators(catIndicators.stream().map(ind -> {
                IndicatorVO iv = new IndicatorVO();
                iv.setId(ind.getId());
                iv.setName(ind.getName());
                iv.setUnit(ind.getUnit());
                iv.setValueType(ind.getValueType());
                iv.setNormalMin(ind.getNormalMin());
                iv.setNormalMax(ind.getNormalMax());
                return iv;
            }).collect(Collectors.toList()));

            result.add(vo);
        }
        return result;
    }

    @Transactional
    public ExamCategory create(CategoryCreateRequest request) {
        ExamCategory category = new ExamCategory();
        category.setName(request.getName());
        category.setIcon(request.getIcon());
        category.setSortOrder(request.getSortOrder());
        categoryMapper.insert(category);
        return category;
    }

    @Transactional
    public boolean update(Integer id, CategoryUpdateRequest request) {
        ExamCategory category = new ExamCategory();
        category.setId(id);
        category.setName(request.getName());
        category.setIcon(request.getIcon());
        category.setSortOrder(request.getSortOrder());
        return categoryMapper.update(category) > 0;
    }

    @Transactional
    public boolean delete(Integer id) {
        return categoryMapper.deleteById(id) > 0;
    }
}
