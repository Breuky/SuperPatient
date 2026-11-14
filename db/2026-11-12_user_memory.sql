-- 长期记忆（用户画像）：只存用户明说的偏好与事实，禁止存推断与诊断结论
CREATE TABLE IF NOT EXISTS user_memory (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT NOT NULL,
  kind       VARCHAR(20) NOT NULL COMMENT 'preference=偏好 / fact=用户自述事实 / focus=关注点(聚合)',
  mkey       VARCHAR(50) NOT NULL COMMENT '如 unit / focus_indicator',
  value      VARCHAR(500) NOT NULL,
  source     VARCHAR(20) NOT NULL DEFAULT 'user_stated' COMMENT 'user_stated / aggregated',
  confidence DECIMAL(3,2) NOT NULL DEFAULT 1.00,
  expires_at DATETIME NULL COMMENT '为空表示不过期',
  deleted_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_memory (user_id, kind, mkey),
  KEY idx_user_active (user_id, deleted_at)
) COMMENT '长期记忆：只影响表达方式，不作为事实来源';
