package com.superpatient.service;

import com.superpatient.dto.UserMemoryVO;
import com.superpatient.exception.BusinessException;
import com.superpatient.mapper.UserMemoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 长期记忆：只接受用户明确表达的偏好与事实。
 * 明确拒绝「推断」类内容——一旦把模型猜测存成事实，后续对话会不断复用错误。
 */
@Service
@RequiredArgsConstructor
public class UserMemoryService {

    private static final Set<String> ALLOWED_KINDS = Set.of("preference", "fact");

    private final UserMemoryMapper mapper;

    public List<UserMemoryVO> list(Long userId) {
        return mapper.findActive(userId);
    }

    public void save(Long userId, String kind, String key, String value) {
        if (kind == null || !ALLOWED_KINDS.contains(kind)) {
            throw new BusinessException("只支持记录偏好(preference)或用户自述事实(fact)");
        }
        if (key == null || key.isBlank() || key.length() > 50) {
            throw new BusinessException("记忆键不合法");
        }
        if (value == null || value.isBlank() || value.length() > 500) {
            throw new BusinessException("记忆内容不合法");
        }
        mapper.upsert(userId, kind, key.trim(), value.trim(), "user_stated", 1.0);
    }

    public void delete(Long userId, Integer id) {
        mapper.softDelete(userId, id);
    }

    public void clear(Long userId) {
        mapper.clear(userId);
    }
}
