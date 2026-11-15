-- ============================================================
-- 个人检查检验指标记录系统 - MySQL 数据库 Schema
-- 版本: 2.1（2026-11-12 重建）
-- 字符集: utf8mb4 / 引擎: InnoDB
--
-- 用法：
--   mysql -uroot -p --default-character-set=utf8mb4 < schema.sql
-- 会 DROP 并重建全部 15 张表；只含公共种子数据，不含任何用户记录。
-- 历史增量脚本在 db/ 目录，仅供升级已有库使用。
-- 改表结构或种子数据后，请重新导出本文件并在临时库验证。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `super_patient`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `super_patient`;

SET FOREIGN_KEY_CHECKS = 0;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `exam_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_category` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称 (如: 抽血、CT、MRI)',
  `icon` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标标识 (用于前端显示)',
  `sort_order` int DEFAULT '0' COMMENT '排序权重 (越小越靠前)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查分类表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `exam_indicator`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_indicator` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_id` int NOT NULL COMMENT '所属分类ID (关联exam_category.id)',
  `owner_id` int NOT NULL DEFAULT '0' COMMENT '归属: 0=系统公共, 其他=用户ID (私有指标)',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标名称 (如: 血红蛋白、白细胞计数)',
  `aliases` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '别名, 逗号分隔 (用于抽取归一)',
  `unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '单位 (如: g/L、10^9/L、mm)',
  `value_type` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT 'number' COMMENT '值类型: number=数值型 text=文本型',
  `normal_min` decimal(12,4) DEFAULT NULL COMMENT '正常下限 (数值型指标使用)',
  `normal_max` decimal(12,4) DEFAULT NULL COMMENT '正常上限 (数值型指标使用)',
  `sort_order` int DEFAULT '0' COMMENT '排序权重',
  `status` tinyint DEFAULT '1' COMMENT '状态: 1=启用 0=禁用 (软删除)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_owner_name` (`owner_id`,`category_id`,`name`),
  KEY `idx_category_id` (`category_id`),
  CONSTRAINT `fk_indicator_category` FOREIGN KEY (`category_id`) REFERENCES `exam_category` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=87 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查指标定义表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `exam_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID (关联user.id)',
  `category_id` int NOT NULL COMMENT '检查分类ID (关联exam_category.id)',
  `exam_date` datetime NOT NULL COMMENT '检查日期时间',
  `hospital` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '检查医院',
  `doctor` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主治医生',
  `notes` text COLLATE utf8mb4_unicode_ci COMMENT '备注 (医嘱、特殊情况等)',
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '报告照片URL (OSS存储路径)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`,`exam_date`),
  KEY `idx_category` (`category_id`),
  CONSTRAINT `fk_record_category` FOREIGN KEY (`category_id`) REFERENCES `exam_category` (`id`),
  CONSTRAINT `fk_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=52 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `exam_value`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_value` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `record_id` bigint NOT NULL COMMENT '检查记录ID (关联exam_record.id)',
  `indicator_id` int NOT NULL COMMENT '指标ID (关联exam_indicator.id)',
  `value_decimal` decimal(12,4) DEFAULT NULL COMMENT '数值型测量值 (血红蛋白、白细胞等)',
  `value_text` text COLLATE utf8mb4_unicode_ci COMMENT '文本型测量值 (CT描述、MRI描述等)',
  `ref_min` decimal(12,4) DEFAULT NULL COMMENT '本次报告印刷的参考下限 (优先于指标默认值)',
  `ref_max` decimal(12,4) DEFAULT NULL COMMENT '本次报告印刷的参考上限',
  `ref_text` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '参考区间原文 (如 4.0-10.0 / 阴性)',
  `ref_source` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '参考区间来源: report / WS/T 405-2012 / 成人男性参考区间 / default',
  `raw_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '报告上的原始指标名 (抽取溯源)',
  `raw_unit` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '报告上的原始单位 (抽取溯源)',
  `is_abnormal` tinyint DEFAULT '0' COMMENT '是否异常: 0=正常 1=偏高 2=偏低 (后端自动计算)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_indicator` (`record_id`,`indicator_id`),
  KEY `idx_record_id` (`record_id`),
  KEY `idx_indicator` (`indicator_id`),
  CONSTRAINT `fk_value_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `exam_indicator` (`id`),
  CONSTRAINT `fk_value_record` FOREIGN KEY (`record_id`) REFERENCES `exam_record` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=142 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查指标值表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `follow_up`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `follow_up` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '所属用户',
  `indicator_id` int NOT NULL COMMENT '指标ID',
  `last_exam_date` date NOT NULL COMMENT '推出这条提醒的那次检查日期',
  `interval_days` int NOT NULL COMMENT '该用户此项的既往复查间隔中位数(天)',
  `due_date` date NOT NULL COMMENT '到期复查日',
  `priority` tinyint NOT NULL DEFAULT '0' COMMENT '0=普通 1=连续异常',
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规则生成的依据，例如「近5次有3次偏高」',
  `note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Agent 润色后的一句话说明',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT 'pending/done/dismissed',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follow_up` (`user_id`,`indicator_id`,`due_date`),
  KEY `idx_follow_up_user` (`user_id`,`status`),
  KEY `fk_follow_up_indicator` (`indicator_id`),
  CONSTRAINT `fk_follow_up_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `exam_indicator` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_follow_up_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='随访提醒(基于用户自身历史间隔)';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `hospital`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hospital` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '医院名称',
  `city` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '城市',
  `district` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '行政区（可能因区划调整变化，留空表示未核对）',
  `level` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '等级，如三甲',
  `hospital_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '综合/中医/专科',
  `address` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `booking_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '官方预约入口',
  `source_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '信息来源链接，填写后才算核对过',
  `verified_at` date DEFAULT NULL COMMENT '核对日期，为空表示未核对',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1=启用 0=停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hospital` (`name`,`city`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医院白名单';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `hospital_department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hospital_department` (
  `id` int NOT NULL AUTO_INCREMENT,
  `hospital_id` int NOT NULL,
  `department` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '科室名，与导诊输出的科室名一致',
  `specialty_level` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '国家/省级临床重点专科，查不到就留空',
  `source_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `verified_at` date DEFAULT NULL COMMENT '为空表示该科室资质尚未核对',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hospital_dept` (`hospital_id`,`department`),
  CONSTRAINT `fk_hd_hospital` FOREIGN KEY (`hospital_id`) REFERENCES `hospital` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医院-科室（白名单的二级表）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `indicator_knowledge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `indicator_knowledge` (
  `id` int NOT NULL AUTO_INCREMENT,
  `indicator_id` int DEFAULT NULL COMMENT '关联指标；为空表示通用条目（如「参考区间为什么因医院而异」）',
  `field` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'definition/why_vary/interference/unit_note',
  `text` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '摘录或改写后的条文',
  `source_id` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `locator` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '定位：表1 / 5.3.2 / 第3章',
  `verified_at` date DEFAULT NULL COMMENT '人工核对日期，为空表示未核对',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_knowledge` (`indicator_id`,`field`,`source_id`,`locator`),
  KEY `fk_knowledge_source` (`source_id`),
  CONSTRAINT `fk_knowledge_source` FOREIGN KEY (`source_id`) REFERENCES `knowledge_source` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=134 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标知识条目（RAG 的语料）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `insight_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insight_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `record_id` bigint DEFAULT NULL,
  `mode` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'simple/agent/rule',
  `question` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `category` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'other' COMMENT 'abnormal/trend/cross_record/value/summary/boundary/other',
  `turns` int DEFAULT NULL COMMENT '对话轮数',
  `tool_calls` int DEFAULT '0' COMMENT '工具调用次数',
  `tokens` int DEFAULT '0',
  `elapsed_ms` int DEFAULT '0',
  `degraded` tinyint DEFAULT '0',
  `summary` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trace_json` text COLLATE utf8mb4_unicode_ci COMMENT '每次工具调用的参数与耗时',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_insight_user_time` (`user_id`,`created_at`),
  KEY `idx_insight_category` (`user_id`,`category`)
) ENGINE=InnoDB AUTO_INCREMENT=668 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 调用留痕';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `knowledge_source`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `knowledge_source` (
  `id` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '如 WST405-2012',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件标题',
  `publisher` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发布机构',
  `origin_level` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'A=国标/行标 B=指南共识 C=教材 D=医院说明/项目自述',
  `doc_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标准号/文号',
  `published_at` date DEFAULT NULL,
  `effective_at` date DEFAULT NULL,
  `url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '官方页面地址（可复核）',
  `license_note` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用范围说明',
  `retrieved_at` date DEFAULT NULL COMMENT '抓取/核对日期',
  `status` tinyint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外部知识来源登记';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `reference_range`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reference_range` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `indicator_id` int NOT NULL COMMENT '指标ID',
  `gender` tinyint DEFAULT NULL COMMENT '性别: NULL=不限 0=女 1=男',
  `age_min` int DEFAULT NULL COMMENT '年龄下限(含), NULL=不限',
  `age_max` int DEFAULT NULL COMMENT '年龄上限(含), NULL=不限',
  `ref_min` decimal(12,4) DEFAULT NULL COMMENT '参考下限',
  `ref_max` decimal(12,4) DEFAULT NULL COMMENT '参考上限',
  `ref_text` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '区间原文',
  `source` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来源说明',
  `priority` int DEFAULT '0' COMMENT '优先级, 越大越优先',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_rr_indicator` (`indicator_id`),
  CONSTRAINT `fk_rr_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `exam_indicator` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=63 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='参考区间规则表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `upload_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `upload_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '上传者ID (关联user.id)',
  `file_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '磁盘文件名',
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原始文件名',
  `content_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MIME 类型',
  `size_bytes` bigint DEFAULT NULL COMMENT '文件大小',
  `biz_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'report' COMMENT '用途: report=报告 avatar=头像',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_name` (`file_name`),
  KEY `idx_upload_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=66 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上传文件归属表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录用户名 (唯一)',
  `password_hash` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BCrypt 密码哈希',
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户昵称',
  `avatar_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `gender` tinyint DEFAULT NULL COMMENT '性别: 0=女 1=男 (参考区间匹配用)',
  `birth_date` date DEFAULT NULL COMMENT '出生日期 (参考区间匹配用)',
  `notes` text COLLATE utf8mb4_unicode_ci COMMENT '备注 (既往病史、过敏史等)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_indicator_focus`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_indicator_focus` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `indicator_id` int NOT NULL COMMENT '指标ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_indicator` (`user_id`,`indicator_id`),
  KEY `fk_focus_indicator` (`indicator_id`),
  CONSTRAINT `fk_focus_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `exam_indicator` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_focus_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注的指标';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_memory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_memory` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `kind` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'preference=偏好 / fact=用户自述事实 / focus=关注点(聚合)',
  `mkey` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '如 unit / focus_indicator',
  `value` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user_stated' COMMENT 'user_stated / aggregated',
  `confidence` decimal(3,2) NOT NULL DEFAULT '1.00',
  `expires_at` datetime DEFAULT NULL COMMENT '为空表示不过期',
  `deleted_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_memory` (`user_id`,`kind`,`mkey`),
  KEY `idx_user_active` (`user_id`,`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='长期记忆：只影响表达方式，不作为事实来源';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 种子数据：公共指标 / 参考区间 / 知识来源与条目 / 医院白名单
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40000 ALTER TABLE `exam_category` DISABLE KEYS */;
INSERT INTO `exam_category` (`id`, `name`, `icon`, `sort_order`, `created_at`) VALUES (1,'抽血','blood',1,'2026-10-21 16:11:32'),(2,'尿常规','urine',2,'2026-10-21 16:11:32'),(3,'CT','ct',3,'2026-10-21 16:11:32'),(4,'MRI','mri',4,'2026-10-21 16:11:32'),(5,'超声','ultrasound',5,'2026-10-21 16:11:32');
/*!40000 ALTER TABLE `exam_category` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40000 ALTER TABLE `exam_indicator` DISABLE KEYS */;
INSERT INTO `exam_indicator` (`id`, `category_id`, `owner_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`, `created_at`) VALUES (18,1,0,'C反应蛋白','CRP,hs-CRP,超敏C反应蛋白,高敏C反应蛋白,全血C反应蛋白','mg/L','number',0.0000,10.0000,18,1,'2026-10-21 16:11:32'),(57,1,0,'γ-谷氨酰转肽酶','GGT,γ-GT,谷氨酰转肽酶,r-GT,谷氨酰转酞酶','U/L','number',NULL,NULL,43,0,'2026-10-22 10:38:35'),(11,1,0,'丙氨酸氨基转移酶','ALT,GPT,谷丙转氨酶','U/L','number',9.0000,50.0000,11,0,'2026-10-21 16:11:32'),(3,1,0,'中性粒细胞百分比','NEUT%,NEUT,中性粒细胞比率,中性粒细胞占比,中性粒细胞%,中性粒%','%','number',40.0000,75.0000,3,1,'2026-10-21 16:11:32'),(42,1,0,'中性粒细胞绝对值','NEUT#,中性粒细胞计数,中性粒细胞数,中性粒细胞绝对数','10^9/L','number',NULL,NULL,28,1,'2026-10-22 09:24:26'),(15,1,0,'乳酸脱氢酶','LDH,乳酸脱氢酶测定','U/L','number',120.0000,250.0000,15,0,'2026-10-21 16:11:32'),(10,1,0,'低密度脂蛋白胆固醇','LDL-C,LDL,低密度脂蛋白','mmol/L','number',0.0000,3.4000,10,0,'2026-10-21 16:11:32'),(47,1,0,'单核细胞百分比','MONO%,单核细胞比率,单核细胞%','%','number',NULL,NULL,33,1,'2026-10-22 09:24:26'),(44,1,0,'单核细胞绝对值','MONO#,单核细胞计数,单核细胞数,单核细胞绝对数','10^9/L','number',NULL,NULL,30,1,'2026-10-22 09:24:26'),(49,1,0,'嗜碱性粒细胞百分比','BASO%,嗜碱细胞百分比,嗜碱性粒细胞比率,嗜碱性粒细胞%','%','number',NULL,NULL,35,1,'2026-10-22 09:24:26'),(46,1,0,'嗜碱性粒细胞绝对值','BASO#,嗜碱细胞计数,嗜碱性粒细胞绝对数,嗜碱性粒细胞计数','10^9/L','number',NULL,NULL,32,1,'2026-10-22 09:24:26'),(48,1,0,'嗜酸性粒细胞百分比','EO%,嗜酸细胞百分比,嗜酸性粒细胞比率,嗜酸性粒细胞%,嗜酸细胞%','%','number',NULL,NULL,34,1,'2026-10-22 09:24:26'),(45,1,0,'嗜酸性粒细胞绝对值','EO#,嗜酸细胞计数,嗜酸性粒细胞绝对数,嗜酸性粒细胞计数','10^9/L','number',NULL,NULL,31,1,'2026-10-22 09:24:26'),(56,1,0,'天冬氨酸氨基转移酶','AST,GOT,谷草转氨酶','U/L','number',NULL,NULL,42,0,'2026-10-22 10:38:35'),(58,1,0,'尿素','UREA,尿素氮,BUN','mmol/L','number',NULL,NULL,44,0,'2026-10-22 10:38:35'),(14,1,0,'尿酸','UA,血尿酸,尿酸测定','umol/L','number',208.0000,428.0000,14,0,'2026-10-21 16:11:32'),(36,1,0,'平均红细胞体积','MCV,红细胞平均体积,平均RBC体积','fL','number',NULL,NULL,22,1,'2026-10-22 09:24:26'),(38,1,0,'平均红细胞血红蛋白浓度','MCHC,平均血红蛋白浓度,平均HGB浓度','g/L','number',NULL,NULL,24,1,'2026-10-22 09:24:26'),(37,1,0,'平均红细胞血红蛋白量','MCH,平均红细胞血红蛋白,平均血红蛋白含量,平均红细胞血红蛋白含量,平均HGB量','pg','number',NULL,NULL,23,1,'2026-10-22 09:24:26'),(40,1,0,'平均血小板体积','MPV,血小板平均体积,血小板体积','fL','number',NULL,NULL,26,1,'2026-10-22 09:24:26'),(8,1,0,'总胆固醇','TC,CHOL,胆固醇','mmol/L','number',2.8000,5.2000,8,0,'2026-10-21 16:11:32'),(12,1,0,'总胆红素','TBIL,T-BIL,总胆红素测定','umol/L','number',0.0000,26.0000,12,0,'2026-10-21 16:11:32'),(50,1,0,'总蛋白','TP,总蛋白测定','g/L','number',NULL,NULL,36,0,'2026-10-22 10:38:35'),(61,1,0,'氯','Cl,血氯','mmol/L','number',NULL,NULL,47,0,'2026-10-22 10:38:35'),(4,1,0,'淋巴细胞百分比','LYMPH%,LYM,淋巴细胞比率,淋巴细胞占比,淋巴细胞%','%','number',20.0000,50.0000,4,1,'2026-10-21 16:11:32'),(43,1,0,'淋巴细胞绝对值','LYMPH#,淋巴细胞计数,淋巴细胞数,淋巴细胞绝对数','10^9/L','number',NULL,NULL,29,1,'2026-10-22 09:24:26'),(52,1,0,'球蛋白','GLB,球蛋白测定','g/L','number',NULL,NULL,38,0,'2026-10-22 10:38:35'),(9,1,0,'甘油三酯','TG,三酰甘油','mmol/L','number',0.4000,1.7000,9,0,'2026-10-21 16:11:32'),(2,1,0,'白细胞计数','WBC,白细胞,白细胞数,LEU','10^9/L','number',3.5000,9.5000,2,1,'2026-10-21 16:11:32'),(51,1,0,'白蛋白','ALB,清蛋白','g/L','number',NULL,NULL,37,0,'2026-10-22 10:38:35'),(53,1,0,'白蛋白球蛋白比值','A/G,白球比,白球比例,白蛋白/球蛋白比值',NULL,'number',NULL,NULL,39,0,'2026-10-22 10:38:35'),(54,1,0,'直接胆红素','DBIL,D-BIL,结合胆红素','umol/L','number',NULL,NULL,40,0,'2026-10-22 10:38:35'),(16,1,0,'碱性磷酸酶','ALP,AKP,碱性磷酸酶测定','U/L','number',40.0000,150.0000,16,0,'2026-10-21 16:11:32'),(62,1,0,'磷','P,血磷,无机磷','mmol/L','number',NULL,NULL,48,0,'2026-10-22 10:38:35'),(6,1,0,'空腹血糖','GLU,血糖,空腹葡萄糖,葡萄糖','mmol/L','number',3.9000,6.1000,6,0,'2026-10-21 16:11:32'),(7,1,0,'糖化血红蛋白','HbA1c,HbA1C,GHb,糖化血红蛋白A1c','%','number',4.0000,6.0000,7,0,'2026-10-21 16:11:32'),(39,1,0,'红细胞分布宽度','RDW,红细胞体积分布宽度,红细胞分布系数','%','number',NULL,NULL,25,1,'2026-10-22 09:24:26'),(35,1,0,'红细胞压积','HCT,红细胞比容,压积,血细胞比容','L/L','number',NULL,NULL,21,1,'2026-10-22 09:24:26'),(34,1,0,'红细胞计数','RBC,红细胞,红细胞数','10^12/L','number',NULL,NULL,20,1,'2026-10-22 09:24:26'),(13,1,0,'肌酐','Cr,CREA,血肌酐,肌酐测定','umol/L','number',44.0000,106.0000,13,0,'2026-10-21 16:11:32'),(67,1,0,'肌酸激酶','CK,CPK,肌酸磷酸激酶','U/L','number',NULL,NULL,53,0,'2026-10-22 10:38:35'),(68,1,0,'肌酸激酶同工酶','CK-MB,CKMB,CK同工酶,肌酸激酶MB同工酶','U/L','number',NULL,NULL,54,0,'2026-10-22 10:38:35'),(69,1,0,'血小板体积分布宽度','PDW,血小板分布宽度,PLT分布宽度','%','number',NULL,NULL,55,1,'2026-10-22 10:38:35'),(41,1,0,'血小板压积','PCT,血小板比积,血小板比容','%','number',NULL,NULL,27,1,'2026-10-22 09:24:26'),(5,1,0,'血小板计数','PLT,血小板,血小板数','10^9/L','number',125.0000,350.0000,5,1,'2026-10-21 16:11:32'),(19,1,0,'血沉','ESR,红细胞沉降率,血沉率','mm/h','number',0.0000,15.0000,19,0,'2026-10-21 16:11:32'),(1,1,0,'血红蛋白','HGB,Hb,HB,血红蛋白浓度','g/L','number',120.0000,160.0000,1,1,'2026-10-21 16:11:32'),(17,1,0,'血钙','Ca,总钙,钙','mmol/L','number',2.1000,2.6000,17,0,'2026-10-21 16:11:32'),(65,1,0,'载脂蛋白A1','ApoA1,APOA1,载脂蛋白A','g/L','number',NULL,NULL,51,0,'2026-10-22 10:38:35'),(66,1,0,'载脂蛋白B','ApoB,APOB','g/L','number',NULL,NULL,52,0,'2026-10-22 10:38:35'),(60,1,0,'钠','Na,血钠','mmol/L','number',NULL,NULL,46,0,'2026-10-22 10:38:35'),(59,1,0,'钾','K,血钾','mmol/L','number',NULL,NULL,45,0,'2026-10-22 10:38:35'),(63,1,0,'镁','Mg,血镁','mmol/L','number',NULL,NULL,49,0,'2026-10-22 10:38:35'),(55,1,0,'间接胆红素','IBIL,I-BIL,非结合胆红素','umol/L','number',NULL,NULL,41,0,'2026-10-22 10:38:35'),(64,1,0,'高密度脂蛋白胆固醇','HDL-C,HDL,高密度脂蛋白','mmol/L','number',NULL,NULL,50,0,'2026-10-22 10:38:35'),(21,2,0,'尿糖','GLU-U,尿葡萄糖',NULL,'text',NULL,NULL,2,1,'2026-10-21 16:11:32'),(20,2,0,'尿蛋白','PRO,URO-PRO,蛋白质',NULL,'text',NULL,NULL,1,1,'2026-10-21 16:11:32'),(22,2,0,'尿隐血','BLD,隐血,潜血',NULL,'text',NULL,NULL,3,1,'2026-10-21 16:11:32'),(24,3,0,'影像描述','描述,影像所见',NULL,'text',NULL,NULL,2,1,'2026-10-21 16:11:32'),(25,3,0,'检查结论','结论,诊断意见',NULL,'text',NULL,NULL,3,1,'2026-10-21 16:11:32'),(23,3,0,'检查部位','部位,扫描部位',NULL,'text',NULL,NULL,1,1,'2026-10-21 16:11:32'),(27,4,0,'影像描述','描述,影像所见',NULL,'text',NULL,NULL,2,1,'2026-10-21 16:11:32'),(28,4,0,'检查结论','结论,诊断意见',NULL,'text',NULL,NULL,3,1,'2026-10-21 16:11:32'),(26,4,0,'检查部位','部位,扫描部位',NULL,'text',NULL,NULL,1,1,'2026-10-21 16:11:32'),(30,5,0,'影像描述','描述,超声所见',NULL,'text',NULL,NULL,2,1,'2026-10-21 16:11:32'),(31,5,0,'检查结论','结论,超声提示',NULL,'text',NULL,NULL,3,1,'2026-10-21 16:11:32'),(29,5,0,'检查部位','部位,扫查部位',NULL,'text',NULL,NULL,1,1,'2026-10-21 16:11:32');
/*!40000 ALTER TABLE `exam_indicator` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40000 ALTER TABLE `reference_range` DISABLE KEYS */;
INSERT INTO `reference_range` (`id`, `indicator_id`, `gender`, `age_min`, `age_max`, `ref_min`, `ref_max`, `ref_text`, `source`, `priority`, `created_at`) VALUES (1,1,1,18,NULL,130.0000,175.0000,'130-175','成人男性参考区间',10,'2026-10-21 16:11:32'),(2,1,0,18,NULL,115.0000,150.0000,'115-150','成人女性参考区间',10,'2026-10-21 16:11:32'),(3,1,NULL,0,17,110.0000,160.0000,'110-160','儿童参考区间',10,'2026-10-21 16:11:32'),(4,14,1,18,NULL,208.0000,428.0000,'208-428','成人男性参考区间',10,'2026-10-21 16:11:32'),(5,14,0,18,NULL,155.0000,357.0000,'155-357','成人女性参考区间',10,'2026-10-21 16:11:32'),(6,13,1,18,NULL,57.0000,97.0000,'57-97','成人男性参考区间',10,'2026-10-21 16:11:32'),(7,13,0,18,NULL,41.0000,73.0000,'41-73','成人女性参考区间',10,'2026-10-21 16:11:32'),(8,19,1,NULL,NULL,0.0000,15.0000,'0-15','男性参考区间',5,'2026-10-21 16:11:32'),(9,19,0,NULL,NULL,0.0000,20.0000,'0-20','女性参考区间',5,'2026-10-21 16:11:32'),(10,16,NULL,1,14,100.0000,350.0000,'100-350','儿童生长期参考区间',10,'2026-10-21 16:11:32'),(11,2,NULL,0,14,5.0000,12.0000,'5-12','儿童参考区间',10,'2026-10-21 16:11:32'),(12,2,NULL,18,NULL,3.5000,9.5000,'3.50-9.50','WS/T 405-2012',10,'2026-11-12 18:49:42'),(13,3,NULL,18,NULL,40.0000,75.0000,'40.00-75.00','WS/T 405-2012',10,'2026-11-12 18:49:42'),(14,4,NULL,18,NULL,20.0000,50.0000,'20.00-50.00','WS/T 405-2012',10,'2026-11-12 18:49:42'),(15,48,NULL,18,NULL,0.4000,8.0000,'0.40-8.00','WS/T 405-2012',10,'2026-11-12 18:49:42'),(16,49,NULL,18,NULL,0.0000,1.0000,'0.00-1.00','WS/T 405-2012',10,'2026-11-12 18:49:42'),(17,47,NULL,18,NULL,3.0000,10.0000,'3.00-10.00','WS/T 405-2012',10,'2026-11-12 18:49:42'),(18,42,NULL,18,NULL,1.8000,6.3000,'1.80-6.30','WS/T 405-2012',10,'2026-11-12 18:49:42'),(19,43,NULL,18,NULL,1.1000,3.2000,'1.10-3.20','WS/T 405-2012',10,'2026-11-12 18:49:42'),(20,45,NULL,18,NULL,0.0200,0.5200,'0.02-0.52','WS/T 405-2012',10,'2026-11-12 18:49:42'),(21,46,NULL,18,NULL,0.0000,0.0600,'0.00-0.06','WS/T 405-2012',10,'2026-11-12 18:49:42'),(22,44,NULL,18,NULL,0.1000,0.6000,'0.10-0.60','WS/T 405-2012',10,'2026-11-12 18:49:42'),(23,36,NULL,18,NULL,82.0000,100.0000,'82.00-100.00','WS/T 405-2012',10,'2026-11-12 18:49:42'),(24,37,NULL,18,NULL,27.0000,34.0000,'27.00-34.00','WS/T 405-2012',10,'2026-11-12 18:49:42'),(25,38,NULL,18,NULL,316.0000,354.0000,'316.00-354.00','WS/T 405-2012',10,'2026-11-12 18:49:42'),(26,5,NULL,18,NULL,125.0000,350.0000,'125.00-350.00','WS/T 405-2012',10,'2026-11-12 18:49:42'),(27,34,1,18,NULL,4.3000,5.8000,'4.3-5.8','WS/T 405-2012',10,'2026-11-12 18:49:42'),(28,34,0,18,NULL,3.8000,5.1000,'3.8-5.1','WS/T 405-2012',10,'2026-11-12 18:49:42'),(29,35,1,18,NULL,0.4000,0.5000,'0.40-0.50','WS/T 405-2012',10,'2026-11-12 18:49:42'),(30,35,0,18,NULL,0.3500,0.4500,'0.35-0.45','WS/T 405-2012',10,'2026-11-12 18:49:42'),(31,11,1,18,NULL,9.0000,50.0000,'9-50','WS/T 404.1-2012',10,'2026-11-12 20:00:29'),(32,11,0,18,NULL,7.0000,40.0000,'7-40','WS/T 404.1-2012',10,'2026-11-12 20:00:29'),(33,56,1,18,NULL,15.0000,40.0000,'15-40','WS/T 404.1-2012',10,'2026-11-12 20:00:29'),(34,56,0,18,NULL,13.0000,35.0000,'13-35','WS/T 404.1-2012',10,'2026-11-12 20:00:29'),(35,16,1,18,NULL,45.0000,125.0000,'45-125','WS/T 404.1-2012',10,'2026-11-12 20:00:29'),(36,16,0,20,49,35.0000,100.0000,'35-100（20-49岁）','WS/T 404.1-2012',10,'2026-11-12 20:00:29'),(37,16,0,50,79,50.0000,135.0000,'50-135（50-79岁）','WS/T 404.1-2012',10,'2026-11-12 20:00:29'),(38,57,1,18,NULL,10.0000,60.0000,'10-60','WS/T 404.1-2012',10,'2026-11-12 20:00:29'),(39,57,0,18,NULL,7.0000,45.0000,'7-45','WS/T 404.1-2012',10,'2026-11-12 20:00:29'),(40,50,NULL,18,NULL,65.0000,85.0000,'65-85','WS/T 404.2-2012',10,'2026-11-12 20:03:52'),(41,51,NULL,18,NULL,40.0000,55.0000,'40-55','WS/T 404.2-2012',10,'2026-11-12 20:03:52'),(42,52,NULL,18,NULL,20.0000,40.0000,'20-40','WS/T 404.2-2012',10,'2026-11-12 20:03:52'),(43,59,NULL,18,NULL,3.5000,5.3000,'3.5-5.3','WS/T 404.3-2012',10,'2026-11-12 20:03:52'),(44,60,NULL,18,NULL,137.0000,147.0000,'137-147','WS/T 404.3-2012',10,'2026-11-12 20:03:52'),(45,61,NULL,18,NULL,99.0000,110.0000,'99-110','WS/T 404.3-2012',10,'2026-11-12 20:03:52'),(46,12,1,20,79,NULL,26.0000,'≤26.0','WS/T 404.4-2012',10,'2026-11-12 20:03:52'),(47,12,0,20,79,NULL,21.0000,'≤21.0','WS/T 404.4-2012',10,'2026-11-12 20:03:52'),(48,58,1,20,59,3.1000,8.0000,'3.1-8.0（男 20-59岁）','WS/T 404.5-2012',10,'2026-11-12 20:03:52'),(49,58,1,60,79,3.6000,9.5000,'3.6-9.5（男 60-79岁）','WS/T 404.5-2012',10,'2026-11-12 20:03:52'),(50,58,0,20,59,2.6000,7.5000,'2.6-7.5（女 20-59岁）','WS/T 404.5-2012',10,'2026-11-12 20:03:52'),(51,58,0,60,79,3.1000,8.8000,'3.1-8.8（女 60-79岁）','WS/T 404.5-2012',10,'2026-11-12 20:03:52'),(55,13,1,20,59,57.0000,97.0000,'57-97（男 20-59岁）','WS/T 404.5-2012',10,'2026-11-12 20:03:52'),(56,13,1,60,79,57.0000,111.0000,'57-111（男 60-79岁）','WS/T 404.5-2012',10,'2026-11-12 20:03:52'),(57,13,0,20,59,41.0000,73.0000,'41-73（女 20-59岁）','WS/T 404.5-2012',10,'2026-11-12 20:03:52'),(58,13,0,60,79,41.0000,81.0000,'41-81（女 60-79岁）','WS/T 404.5-2012',10,'2026-11-12 20:03:52'),(62,18,NULL,18,79,NULL,6.0000,'≤6.0','WS/T 404.9-2026',10,'2026-11-12 20:03:52');
/*!40000 ALTER TABLE `reference_range` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40000 ALTER TABLE `knowledge_source` DISABLE KEYS */;
INSERT INTO `knowledge_source` (`id`, `title`, `publisher`, `origin_level`, `doc_no`, `published_at`, `effective_at`, `url`, `license_note`, `retrieved_at`, `status`) VALUES ('GUIDE-CKD-2023','中国慢性肾脏病早期评价与管理指南','中华预防医学会肾脏病预防与控制专业委员会','B','中华内科杂志 2023;62(8):902-930','2023-08-01',NULL,'http://mp.weixin.qq.com/s?__biz=MzI4MzAxOTMwMQ==&mid=2650114501&idx=6&sn=3b2b70e2619c626e188dd878f1ebd28e','期刊原文的推荐意见与图表转载；仅摘录表2、表3 的分期定义','2026-11-12',1),('GUIDE-DM-2024','中国糖尿病防治指南（2024 版）','中华医学会糖尿病学分会','B',NULL,'2024-12-01',NULL,'https://www.yantai.gov.cn:444/cms_files/jcms1/web1/site/attach/0/96e88fd3c75d4c8b91577814a7c86930.pdf','政府网站公开全文；仅摘录表3注释中的一句原文','2026-11-12',1),('GUIDE-LIPID-2023','中国血脂管理指南（2023 年）','中国血脂管理指南修订联合专家委员会','B',NULL,'2023-03-01',NULL,'https://csc.cma.org.cn/attach/0/96b4e603cee74b7fb208b278aa92bd8b.pdf','指南全文公开；仅摘录要点提示与条文并标注章节，不转载原文','2026-11-12',1),('PROJECT-DRAFT','项目自述条目（待核对）','本项目','D','—',NULL,NULL,NULL,'由项目整理，尚未逐条核对权威出处；确认来源前不进入检索索引','2026-11-12',1),('TEXTBOOK-COMMON','常见医学教材通用表述','项目整理','C',NULL,NULL,NULL,NULL,'教材级通用定义，尚未逐条标注页码；回答中会明示来源等级为 C，不作为诊断依据','2026-11-12',1),('UNIT-CONV','法定计量单位换算','国际单位制（SI）定义','A','—',NULL,NULL,NULL,'换算系数为计量单位定义，不涉及医学判断','2026-11-12',1),('WST404.1-2012','临床常用生化检验项目参考区间 第1部分：ALT、AST、ALP、GGT','中华人民共和国卫生部','A','WS/T 404.1-2012','2012-12-25','2013-08-01','http://www.nhc.gov.cn/ewebeditor/uploadfile/2013/01/20130109170719547.pdf','卫生行业标准；仅摘录表1数值并标注出处','2026-11-12',1),('WST404.2-2012','临床常用生化检验项目参考区间 第2部分：血清总蛋白、白蛋白','中华人民共和国卫生部','A','WS/T 404.2-2012','2012-12-25','2013-08-01','https://www.nhc.gov.cn/ewebeditor/uploadfile/2013/01/20130109170840325.pdf','卫生行业标准；仅摘录表1数值','2026-11-12',1),('WST404.3-2012','临床常用生化检验项目参考区间 第3部分：血清钾、钠、氯','中华人民共和国卫生部','A','WS/T 404.3-2012','2012-12-25','2013-08-01','https://www.nhc.gov.cn/ewebeditor/uploadfile/2013/01/20130109170954741.pdf','卫生行业标准；仅摘录表1数值','2026-11-12',1),('WST404.4-2012','临床常用生化检验项目参考区间 第4部分：血清总胆红素、直接胆红素','中华人民共和国卫生部','A','WS/T 404.4-2012','2018-05-01','2018-11-01','https://www.nhc.gov.cn/wjw/s9492/201805/67aa364588f74ec69a1de85f893ed2c4/files/1739781483928_13416.pdf','卫生行业标准；仅摘录表1数值','2026-11-12',1),('WST404.5-2012','临床常用生化检验项目参考区间 第5部分：血清尿素、肌酐','中华人民共和国卫生部','A','WS/T 404.5-2012','2015-05-01','2015-11-01','https://www.nhc.gov.cn/ewebeditor/uploadfile/2015/05/20150504151554530.pdf','卫生行业标准；仅摘录表1数值','2026-11-12',1),('WST404.9-2026','临床常用生化检验项目参考区间 第9部分：血清C-反应蛋白、前白蛋白、转铁蛋白、β2-微球蛋白','中华人民共和国国家卫生健康委员会','A','WS/T 404.9-2026','2026-05-25','2026-11-01','https://www.nhc.gov.cn/wjw/s9492/202606/7659f697fc954a188cf7f53c62501cf6/files/WST 404.9—2026-20260612152247997.pdf','卫生行业标准；仅摘录表1数值','2026-11-12',1),('WST405-2012','血细胞分析参考区间','中华人民共和国卫生部','A','WS/T 405-2012','2012-12-25','2013-08-01','http://www.nhc.gov.cn/ewebeditor/uploadfile/2013/01/20130109171100186.pdf','卫生行业标准，公开征求意见稿/发布稿；仅摘录条文并标注出处，不转载全文','2026-11-12',1),('WST661-2020','静脉血液标本采集指南','中华人民共和国国家卫生健康委员会','A','WS/T 661-2020','2020-03-26','2020-10-01','https://www.nhc.gov.cn/fzs/c100048/202004/b70d1212e11d4c38b6cdc5449594ade7/files/1733125608508_41277.pdf','卫生行业标准；仅摘录第4章条文并标注条款号','2026-11-12',1);
/*!40000 ALTER TABLE `knowledge_source` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40000 ALTER TABLE `indicator_knowledge` DISABLE KEYS */;
INSERT INTO `indicator_knowledge` (`id`, `indicator_id`, `field`, `text`, `source_id`, `locator`, `verified_at`) VALUES (1,NULL,'why_vary','不同人群与采血方式会影响血细胞分析结果：新生儿及儿童的白细胞、红细胞计数随年龄增长明显变化；高海拔地区人群血红蛋白与红细胞计数明显高于其他地区；末梢采血与静脉采血结果存在差异；民族、生活习惯、地区差异也会造成结果变化。','WST405-2012','5.3.2','2026-11-12'),(2,NULL,'why_vary','本标准参考区间基于中国成年人群多中心研究结果，适用于静脉血仪器检测方法；临床实验室应优先采用本标准区间，并在使用前做必要的验证或评估。','WST405-2012','5.1.1 / 表1注','2026-11-12'),(3,NULL,'why_vary','当实验室的检测系统存在恒定系统偏倚时，会影响参考区间的适用性；而实验室内变异过大属于分析质量问题，不是参考区间适用性问题。','WST405-2012','5.1.2 注2','2026-11-12'),(4,6,'unit_note','血糖类结果常用两种单位：mmol/L 与 mg/dL，换算关系为 1 mmol/L = 18 mg/dL（mg/dL = mmol/L × 18）。','UNIT-CONV','单位定义','2026-11-12'),(5,13,'unit_note','肌酐常用 μmol/L 与 mg/dL 两种单位，换算关系为 1 mg/dL ≈ 88.4 μmol/L。','UNIT-CONV','单位定义','2026-11-12'),(6,8,'unit_note','胆固醇类结果 1 mmol/L ≈ 38.67 mg/dL；甘油三酯 1 mmol/L ≈ 88.5 mg/dL。两者系数不同，不能混用。','UNIT-CONV','单位定义','2026-11-12'),(7,1,'unit_note','血红蛋白常用 g/L 与 g/dL：1 g/dL = 10 g/L。报告写 13.5 g/dL 相当于 135 g/L。','UNIT-CONV','单位定义','2026-11-12'),(86,NULL,'prep','采血前不宜改变饮食习惯，24 小时内不宜饮酒。','WST661-2020','4.1.1','2026-11-12'),(87,NULL,'prep','需要空腹采血的项目包括（不限于）：糖代谢类（空腹血糖、空腹胰岛素、空腹 C 肽等）、血脂类（总胆固醇、甘油三酯、高密度与低密度脂蛋白胆固醇、载脂蛋白等）、血液流变学、骨代谢标志物、血小板聚集率。','WST661-2020','4.1.2','2026-11-12'),(88,NULL,'prep','空腹要求至少禁食 8 小时，以 12～14 小时为宜，但不宜超过 16 小时；宜安排在上午 7:00～9:00 采血；空腹期间可少量饮水。','WST661-2020','4.1','2026-11-12'),(89,NULL,'prep','采血前 24 小时不宜剧烈运动；采血当天宜避免情绪激动；采血前宜静息至少 5 分钟。','WST661-2020','4.2','2026-11-12'),(90,NULL,'prep','部分项目对采血时间有特殊要求，例如促肾上腺皮质激素与皮质醇存在昼夜节律、女性性激素随生理周期变化，采血时间需按医嘱安排。','WST661-2020','4.3','2026-11-12'),(91,12,'unit_note','总胆红素常用 μmol/L 与 mg/dL：1 mg/dL ≈ 17.1 μmol/L（μmol/L = mg/dL × 17.1）。','UNIT-CONV','单位定义','2026-11-12'),(92,58,'unit_note','尿素常用 mmol/L 与 mg/dL：1 mmol/L ≈ 6 mg/dL（以尿素氮计需另行注明，两种口径不能混用）。','UNIT-CONV','单位定义','2026-11-12'),(93,14,'unit_note','尿酸常用 μmol/L 与 mg/dL：1 mg/dL ≈ 59.5 μmol/L。','UNIT-CONV','单位定义','2026-11-12'),(94,NULL,'unit_note','电解质结果通常以 mmol/L 报告（钾、钠、氯），一般不做 mg/dL 换算；报告上的单位差异主要来自不同实验室习惯，读数值时以报告单位为准。','UNIT-CONV','单位定义','2026-11-12'),(95,1,'definition','血红蛋白是红细胞内负责运输氧的蛋白质，血液中的血红蛋白浓度反映红细胞的携氧能力，单位为 g/L。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(96,2,'definition','白细胞计数是单位体积血液中白细胞的总数，白细胞参与机体免疫防御，单位为 ×10⁹/L。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(97,5,'definition','血小板计数是单位体积血液中血小板的数量，血小板参与止血与凝血过程，单位为 ×10⁹/L。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(98,34,'definition','红细胞计数是单位体积血液中红细胞的数量，单位为 ×10¹²/L。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(99,35,'definition','红细胞压积是红细胞体积占全血体积的比例，又称血细胞比容。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(100,36,'definition','平均红细胞体积（MCV）是单个红细胞的平均体积，常用于贫血的形态学分类，单位为 fL。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(101,37,'definition','平均红细胞血红蛋白量（MCH）是单个红细胞内血红蛋白的平均含量，单位为 pg。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(102,38,'definition','平均红细胞血红蛋白浓度（MCHC）是红细胞内血红蛋白的平均浓度，单位为 g/L。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(103,3,'definition','中性粒细胞百分比是中性粒细胞占白细胞总数的比例。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(104,4,'definition','淋巴细胞百分比是淋巴细胞占白细胞总数的比例。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(105,18,'definition','C 反应蛋白（CRP）是一种急性时相反应蛋白，机体存在炎症或感染时其浓度可升高，单位为 mg/L。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(106,6,'definition','空腹血糖是至少空腹 8 小时后测得的血液葡萄糖浓度，单位为 mmol/L。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(107,7,'definition','糖化血红蛋白是红细胞内血红蛋白与葡萄糖结合的产物，其比例反映近 2～3 个月的平均血糖水平。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(108,8,'definition','总胆固醇是血液中各类脂蛋白所含胆固醇的总量，单位为 mmol/L。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(109,9,'definition','甘油三酯是血液中的一种脂类，主要来源于饮食摄入与肝脏合成，单位为 mmol/L。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(110,10,'definition','低密度脂蛋白胆固醇（LDL-C）是主要将胆固醇运送至外周组织的脂蛋白，单位为 mmol/L。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(111,64,'definition','高密度脂蛋白胆固醇（HDL-C）参与胆固醇的逆向转运，单位为 mmol/L。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(112,11,'definition','丙氨酸氨基转移酶（ALT）是主要存在于肝细胞胞浆中的酶，单位为 U/L。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(113,56,'definition','天冬氨酸氨基转移酶（AST）是存在于肝细胞线粒体及心肌、骨骼肌中的酶，单位为 U/L。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(114,13,'definition','肌酐是肌肉中磷酸肌酸的代谢产物，主要经肾小球滤过排出，单位为 μmol/L。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(115,58,'definition','尿素是蛋白质代谢的终产物，主要经肾脏排出，单位为 mmol/L。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(116,12,'definition','总胆红素是血红蛋白分解代谢的产物，经肝脏处理后随胆汁排泄，单位为 μmol/L。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(117,51,'definition','白蛋白是血浆中含量最高的蛋白质，参与维持血浆胶体渗透压并运输多种物质，单位为 g/L。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(118,14,'definition','尿酸是嘌呤代谢的终产物，单位为 μmol/L。','TEXTBOOK-COMMON',NULL,'2026-11-12'),(126,NULL,'why_vary','血脂的“合适水平”随动脉粥样硬化性心血管疾病（ASCVD）危险分层不同而异：在没有危险因素的人群中可视为“正常”的低密度脂蛋白胆固醇水平，对高危或极高危患者而言可能已属明显升高。因此指南以危险分层确定目标值，而不是用单一参考区间判断。','GUIDE-LIPID-2023','摘要','2026-11-12'),(127,10,'definition','低密度脂蛋白胆固醇（LDL-C）是动脉粥样硬化性心血管疾病的致病性危险因素，也是血脂干预的首要靶点。','GUIDE-LIPID-2023','摘要','2026-11-12'),(128,8,'definition','与临床密切相关的血脂成分主要包括胆固醇和甘油三酯；胆固醇又分为低密度脂蛋白胆固醇、高密度脂蛋白胆固醇等，不同成分的临床意义不同。','GUIDE-LIPID-2023','二、血脂与脂蛋白','2026-11-12'),(129,9,'prep','血脂检测通常要求空腹采血（空腹约 12 小时），因为餐后乳糜微粒会使血清外观混浊并影响甘油三酯的测定。','GUIDE-LIPID-2023','二、血脂与脂蛋白','2026-11-12'),(130,NULL,'prep','空腹状态指至少 8 小时没有进食热量。','GUIDE-DM-2024','表3 注释','2026-11-12'),(131,NULL,'definition','慢性肾脏病按肾小球滤过率（eGFR）分为 G1（≥90）、G2（60~89）、G3a（45~59）、G3b（30~44）、G4（15~29）、G5（肾衰竭）期，单位 ml·min⁻¹·1.73m⁻²。','GUIDE-CKD-2023','表2','2026-11-12'),(132,NULL,'definition','尿白蛋白/肌酐比值（UACR）用于分级：A1 为正常至轻度增加，A2 为 30~300 mg/g（中度增加），A3 为 >300 mg/g（显著增加）。','GUIDE-CKD-2023','表3','2026-11-12'),(133,13,'definition','血肌酐是估算肾小球滤过率（eGFR）的基础指标之一，单独看肌酐难以反映早期肾功能变化，临床上常结合 eGFR 与尿白蛋白分级评估。','GUIDE-CKD-2023','表2、表3','2026-11-12');
/*!40000 ALTER TABLE `indicator_knowledge` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40000 ALTER TABLE `hospital` DISABLE KEYS */;
INSERT INTO `hospital` (`id`, `name`, `city`, `district`, `level`, `hospital_type`, `address`, `booking_url`, `source_url`, `verified_at`, `status`, `created_at`) VALUES (1,'浙江大学医学院附属第一医院','杭州',NULL,'三甲','综合',NULL,NULL,'https://wsjkw.zj.gov.cn/art/2020/7/31/art_1229560650_2320281.html','2026-11-12',1,'2026-11-12 14:12:31'),(2,'浙江大学医学院附属第二医院','杭州',NULL,'三甲','综合',NULL,NULL,'https://wsjkw.zj.gov.cn/art/2020/7/31/art_1229560650_2320281.html','2026-11-12',1,'2026-11-12 14:12:31'),(3,'浙江大学医学院附属邵逸夫医院','杭州',NULL,'三甲','综合',NULL,NULL,'https://wsjkw.zj.gov.cn/art/2020/7/31/art_1229560650_2320281.html','2026-11-12',1,'2026-11-12 14:12:31'),(4,'浙江大学医学院附属妇产科医院','杭州',NULL,'三甲','专科',NULL,NULL,'https://www.womanhospital.cn/','2026-11-12',1,'2026-11-12 14:12:31'),(5,'浙江大学医学院附属儿童医院','杭州',NULL,'三甲','专科',NULL,NULL,'https://www.zjuch.cn/news/default/id/8243/cid/99','2026-11-12',1,'2026-11-12 14:12:31'),(6,'浙江大学医学院附属口腔医院','杭州',NULL,'三甲','专科',NULL,NULL,'https://wsjkw.zj.gov.cn/art/2024/1/15/art_1229560650_2510173.html','2026-11-12',1,'2026-11-12 14:12:31'),(7,'浙江省人民医院','杭州',NULL,'三甲','综合',NULL,NULL,'https://wsjkw.zj.gov.cn/art/2020/7/31/art_1229560650_2320281.html','2026-11-12',1,'2026-11-12 14:12:31'),(8,'浙江省中医院','杭州',NULL,'三甲','中医',NULL,NULL,'https://wsjkw.zj.gov.cn/art/2023/6/30/art_1229560650_2482072.html','2026-11-12',1,'2026-11-12 14:12:31'),(9,'浙江省肿瘤医院','杭州',NULL,'三甲','专科',NULL,NULL,'https://wsjkw.zj.gov.cn/art/2023/3/22/art_1229560650_2471410.html','2026-11-12',1,'2026-11-12 14:12:31'),(12,'杭州市第一人民医院','杭州',NULL,'三甲','综合',NULL,NULL,'https://wsjkw.zj.gov.cn/art/2020/7/31/art_1229560650_2320281.html','2026-11-12',1,'2026-11-12 14:12:31'),(14,'杭州市第三人民医院','杭州',NULL,'三甲','专科',NULL,NULL,'https://wsjkw.zj.gov.cn/art/2024/1/15/art_1229560650_2510173.html','2026-11-12',1,'2026-11-12 14:12:31'),(20,'杭州市第七人民医院','杭州',NULL,'三甲','专科',NULL,NULL,'https://wsjkw.zj.gov.cn/art/2023/3/22/art_1229560650_2471410.html','2026-11-12',1,'2026-11-12 14:12:31');
/*!40000 ALTER TABLE `hospital` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40000 ALTER TABLE `hospital_department` DISABLE KEYS */;
INSERT INTO `hospital_department` (`id`, `hospital_id`, `department`, `specialty_level`, `source_url`, `verified_at`) VALUES (1,7,'内分泌科',NULL,'https://www.hospitalstar.com/#/intro/detail?id=201','2026-11-12'),(2,3,'内分泌科','省级医学重点学科','https://www.srrsh.com/departmentInfo/24','2026-11-12'),(3,2,'内分泌科',NULL,'https://www.z2hospital.com/Jxgl/News/Detail/13692','2026-11-12'),(4,1,'内分泌科',NULL,'https://www.zy91.com/server/characteristic_dept/209','2026-11-12'),(6,12,'内分泌科',NULL,'https://www.hz-hospital.com/feature/zlcenter','2026-11-12'),(7,7,'肾内科',NULL,'https://www.hospitalstar.com/#/intro/detail?id=201','2026-11-12'),(8,3,'肾内科','省级医学重点专科','https://www.srrsh.com/departmentInfo/20','2026-11-12'),(9,2,'肾内科',NULL,'https://www.z2hospital.com/Jxgl/News/Detail/13692','2026-11-12'),(10,1,'肾内科','国家临床重点专科','https://www.zy91.com/server/characteristic_dept/209','2026-11-12'),(12,12,'肾内科',NULL,'https://www.hz-hospital.com/feature/zlcenter','2026-11-12'),(13,7,'消化内科','省级临床重点专科','https://www.hospitalstar.com/#/intro/detail?id=201','2026-11-12'),(14,3,'消化内科',NULL,'https://www.srrsh.com/departmentInfo/15','2026-11-12'),(15,2,'消化内科',NULL,'https://www.z2hospital.com/Jxgl/News/Detail/13692','2026-11-12'),(16,1,'消化内科','国家临床重点专科','https://www.zy91.com/server/characteristic_dept/209','2026-11-12'),(18,12,'消化内科','国家临床重点专科','https://www.hz-hospital.com/feature/zlcenter','2026-11-12');
/*!40000 ALTER TABLE `hospital_department` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

SET FOREIGN_KEY_CHECKS = 1;
