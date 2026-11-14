# -*- coding: utf-8 -*-
"""检索层单测：不连数据库，用固定语料"""
import unittest

import knowledge

ENTRIES = [
    {"id": 1, "indicatorId": 7, "indicatorName": "糖化血红蛋白",
     "aliases": "HbA1c|糖化", "field": "definition",
     "text": "糖化血红蛋白反映近 2-3 个月的平均血糖水平，与单次血糖不同",
     "sourceId": "S1", "sourceTitle": "测试来源", "locator": "1.1"},
    {"id": 2, "indicatorId": 8, "indicatorName": "低密度脂蛋白胆固醇",
     "aliases": "LDL|LDL-C", "field": "definition",
     "text": "低密度脂蛋白胆固醇负责把胆固醇运送到外周组织，俗称坏胆固醇",
     "sourceId": "S1", "sourceTitle": "测试来源", "locator": "1.2"},
    {"id": 3, "indicatorId": None, "indicatorName": None, "aliases": None,
     "field": "prep", "text": "多数血液生化检查建议空腹采血，空腹指禁食 8-12 小时",
     "sourceId": "S2", "sourceTitle": "测试来源", "locator": "2.1"},
    {"id": 4, "indicatorId": None, "indicatorName": None, "aliases": None,
     "field": "why_vary", "text": "不同医院使用的检测系统、试剂与参考人群不同，参考区间会随之不同",
     "sourceId": "S2", "sourceTitle": "测试来源", "locator": "2.2"},
    {"id": 5, "indicatorId": 9, "indicatorName": "血红蛋白", "aliases": "Hb|HGB",
     "field": "interference", "text": "高海拔地区人群的血红蛋白通常高于平原地区",
     "sourceId": "S3", "sourceTitle": "测试来源", "locator": "3.1"},
]

INDICATORS = [
    {"id": 7, "name": "糖化血红蛋白", "aliases": "HbA1c|糖化"},
    {"id": 8, "name": "低密度脂蛋白胆固醇", "aliases": "LDL|LDL-C"},
    {"id": 9, "name": "血红蛋白", "aliases": "Hb|HGB"},
]


class KnowledgeTest(unittest.TestCase):
    def setUp(self):
        self.idx = knowledge.Index(ENTRIES)

    def test_definition_hit(self):
        r = self.idx.search("LDL 是什么", INDICATORS)
        self.assertTrue(r["hits"])
        self.assertEqual("低密度脂蛋白胆固醇", r["hits"][0]["indicator"])
        self.assertIn("definition", r["query"]["fields"])

    def test_alias_anchors_indicator(self):
        r = self.idx.search("糖化是什么意思", INDICATORS)
        self.assertEqual([7], r["query"]["anchored"])
        self.assertEqual("糖化血红蛋白", r["hits"][0]["indicator"])

    def test_prep_question_hits_general_entry(self):
        r = self.idx.search("抽血前要不要空腹", INDICATORS)
        self.assertTrue(r["hits"])
        self.assertEqual("prep", r["hits"][0]["field"])

    def test_why_vary(self):
        r = self.idx.search("为什么两家医院的参考区间不一样", INDICATORS)
        self.assertTrue(r["hits"])
        self.assertEqual("why_vary", r["hits"][0]["field"])

    def test_anchor_filters_other_indicators(self):
        r = self.idx.search("血红蛋白是什么", INDICATORS)
        names = [h["indicator"] for h in r["hits"]]
        self.assertNotIn("低密度脂蛋白胆固醇", names)

    def test_off_domain_returns_empty(self):
        r = self.idx.search("今天天气怎么样", INDICATORS)
        self.assertEqual([], r["hits"])
        self.assertTrue(r["note"])

    def test_too_short_query(self):
        r = self.idx.search("啊", INDICATORS)
        self.assertEqual([], r["hits"])

    def test_scores_are_sorted(self):
        r = self.idx.search("血红蛋白 高海拔 影响", INDICATORS, top_k=3)
        scores = [h["score"] for h in r["hits"]]
        self.assertEqual(scores, sorted(scores, reverse=True))

    def test_missing_field_returns_empty(self):
        # 语料里没有 definition 条目时，问「是什么」应返回空，而不是拿 why_vary 凑
        idx = knowledge.Index([e for e in ENTRIES if e["field"] != "definition"])
        r = idx.search("LDL 是什么", INDICATORS)
        self.assertEqual([], r["hits"])
        self.assertIn("definition", r["note"])

    def test_citation_fields_present(self):
        r = self.idx.search("糖化是什么", INDICATORS)
        hit = r["hits"][0]
        for key in ("source_id", "source_title", "locator", "text"):
            self.assertTrue(hit.get(key), key)
        self.assertIn("origin_level", hit, "命中结果要带来源等级字段供前端展示")

    def test_clean_strips_filler(self):
        self.assertEqual("ldl", knowledge.clean("帮我看看我的 LDL 啊"))


class HybridTest(unittest.TestCase):
    """向量层：用可预测的假向量验证 RRF 融合顺序"""

    def test_rrf_prefers_item_ranked_high_in_both_lists(self):
        scores = knowledge.rrf([[0, 1, 2], [0, 2, 1]])
        self.assertGreater(scores[0], scores[1])
        self.assertGreater(scores[0], scores[2])

    def test_vector_changes_order_when_bm25_ties(self):
        # 两条文本完全一样 → BM25 打平；向量只偏向第二条 → 顺序应翻转
        same = {"indicatorId": None, "indicatorName": None, "aliases": None,
                "field": "definition", "text": "同一条文本", "sourceId": "S", "sourceTitle": "T", "locator": "1"}
        idx = knowledge.Index([dict(same, id=1), dict(same, id=2)],
                              vectors=[[1.0, 0.0], [0.0, 1.0]])
        r = idx.search("文本", INDICATORS, query_vector=[0.0, 1.0])
        self.assertTrue(r["hits"])

    def test_source_level_boost(self):
        same = {"indicatorId": None, "indicatorName": None, "aliases": None,
                "field": "prep", "text": "空腹要求至少禁食 8 小时", "sourceTitle": "T", "locator": "1"}
        entries = [dict(same, id=1, sourceId="D1", originLevel="D"),
                   dict(same, id=2, sourceId="A1", originLevel="A")]
        r = knowledge.Index(entries).search("空腹要多久", INDICATORS)
        self.assertEqual("A1", r["hits"][0]["source_id"], "同为 prep 时 A 级来源应排前")

    def test_without_vectors_still_works(self):
        idx = knowledge.Index(ENTRIES)
        r = idx.search("LDL 是什么", INDICATORS, query_vector=[0.1, 0.9])
        self.assertTrue(r["hits"])


if __name__ == "__main__":
    unittest.main(verbosity=2)