# -*- coding: utf-8 -*-
"""范围判定的单测：python agent/test_scope.py"""
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from scope import is_off_topic  # noqa: E402


class ScopeTest(unittest.TestCase):

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


if __name__ == "__main__":
    unittest.main()
