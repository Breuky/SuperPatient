# -*- coding: utf-8 -*-
"""能力分级的单测：python agent/test_scope.py"""
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from scope import (L0_DATA, L1_EXPLAIN, L2_MEDICAL, L3_SERVICE, OFF_TOPIC,
                   ceiling_hint, classify, is_off_topic, label)  # noqa: E402


class OffTopicTest(unittest.TestCase):

    def test_weather_is_off_topic(self):
        self.assertTrue(is_off_topic("今天天气如何？"))
        self.assertTrue(is_off_topic("明天会下雨吗"))

    def test_writing_and_coding_are_off_topic(self):
        self.assertTrue(is_off_topic("帮我写一首关于秋天的诗"))
        self.assertTrue(is_off_topic("用 Python 写个快排"))

    def test_data_questions_go_through(self):
        for q in ("这次有哪几项超出参考区间？", "我的血糖最近怎么样？",
                  "上次检查是什么时候？", "血小板比上次高了还是低了？"):
            self.assertFalse(is_off_topic(q), q)

    def test_medical_judgement_is_not_off_topic(self):
        # 属于「越界提问」，要由模型给范围说明，而不是当成天气问题
        for q in ("血糖高有什么危害？", "我是不是得了糖尿病？", "要不要吃药"):
            self.assertFalse(is_off_topic(q), q)

    def test_blank_is_off_topic(self):
        self.assertTrue(is_off_topic(""))
        self.assertTrue(is_off_topic("   "))


class CapabilityTest(unittest.TestCase):

    def test_l0_pure_facts(self):
        for q in ("上次检查是什么时候？", "这次血糖是多少", "一共查过几次",
                  "这个指标的单位是什么"):
            self.assertEqual(classify(q), L0_DATA, q)

    def test_l1_explain_and_trend(self):
        for q in ("这次有哪几项超出参考区间？", "血小板比上次高了还是低了？",
                  "我的血糖最近怎么样？", "这次结果正常吗"):
            self.assertEqual(classify(q), L1_EXPLAIN, q)

    def test_l2_medical_judgement(self):
        for q in ("血糖高有什么危害？", "我是不是得了糖尿病？", "要不要吃药",
                  "血糖多少算高", "这个要紧吗", "需要复查吗"):
            self.assertEqual(classify(q), L2_MEDICAL, q)

    def test_l3_service(self):
        for q in ("帮我挂个内分泌科的号", "能帮我预约明天吗", "我想在线问诊",
                  "帮我买点药"):
            self.assertEqual(classify(q), L3_SERVICE, q)

    def test_trend_question_is_not_downgraded_by_weak_medical_word(self):
        # 「是不是」是医学判断的弱信号，但这句话问的是变化，应留在 L1
        self.assertEqual(classify("是不是比上次高了"), L1_EXPLAIN)
        self.assertEqual(classify("这个指标会不会比上次低"), L1_EXPLAIN)

    def test_medical_word_with_trend_still_wins(self):
        # 强信号不受趋势词影响：「有没有危害，比上次高了多少」
        self.assertEqual(classify("比上次高了不少，有什么危害"), L2_MEDICAL)

    def test_default_is_explain(self):
        self.assertEqual(classify("帮我看看这次的检查"), L1_EXPLAIN)

    def test_labels_and_ceilings(self):
        for level in (L0_DATA, L1_EXPLAIN, L2_MEDICAL, L3_SERVICE):
            self.assertTrue(label(level))
            self.assertIn(level, ceiling_hint(level))
        self.assertEqual(label(OFF_TOPIC), "无关话题")


if __name__ == "__main__":
    unittest.main()
