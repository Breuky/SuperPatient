-- 提醒文案缓存：由 Agent 润色后的版本，模型不可用时为空，前端回落到 reason
ALTER TABLE `follow_up`
    ADD COLUMN `note` VARCHAR(500) DEFAULT NULL COMMENT 'Agent 润色后的一句话说明' AFTER `reason`;
