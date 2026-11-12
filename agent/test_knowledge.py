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

    def test_clean_strips_filler(self):
        self.assertEqual("ldl", knowledge.clean("帮我看看我的 LDL 啊"))


if __name__ == "__main__":
    unittest.main(verbosity=2)