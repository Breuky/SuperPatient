# -*- coding: utf-8 -*-
"""科室映射规则单测：python test_departments.py"""
import unittest

from departments import suggest, GENERAL, ENDOCRINE, NEPHRO, GASTRO, HEMATO, RHEUM


def names(result):
    return [d["name"] for d in result["departments"]]


class DepartmentsTest(unittest.TestCase):

    def test_alt_points_to_gastro(self):
        r = suggest([{"name": "谷丙转氨酶", "direction": "high"}])
        self.assertEqual(GASTRO, names(r)[0])

    def test_creatinine_points_to_nephrology(self):
        r = suggest([{"name": "肌酐", "direction": "high"}])
        self.assertEqual(NEPHRO, names(r)[0])

    def test_hba1c_and_glucose_point_to_endocrine(self):
        r = suggest([
            {"name": "糖化血红蛋白", "direction": "high"},
            {"name": "空腹血糖", "direction": "high"},
        ])
        self.assertEqual(ENDOCRINE, names(r)[0])
        self.assertGreaterEqual(r["departments"][0]["score"], 1.0)

    def test_multiple_abnormal_aggregates(self):
        r = suggest([
            {"name": "肌酐", "direction": "high"},
            {"name": "尿蛋白", "direction": "positive"},
            {"name": "尿素", "direction": "high"},
        ])
        self.assertEqual(NEPHRO, names(r)[0])
        self.assertEqual(1.0, r["departments"][0]["score"])

    def test_uric_acid_has_two_candidates(self):
        r = suggest([{"name": "尿酸", "direction": "high"}])
        self.assertIn(RHEUM, names(r))
        self.assertGreaterEqual(len(names(r)), 2, "尿酸同时指向风湿免疫与内分泌")

    def test_cbc_points_to_hematology(self):
        r = suggest([
            {"name": "血红蛋白", "direction": "low"},
            {"name": "红细胞", "direction": "low"},
        ])
        self.assertEqual(HEMATO, names(r)[0])

    def test_unknown_indicator_falls_back_to_general(self):
        r = suggest([{"name": "某项未收录指标", "direction": "high"}])
        self.assertEqual([GENERAL], names(r))
        self.assertEqual(["某项未收录指标"], r["unmapped"])

    def test_empty_input(self):
        r = suggest([])
        self.assertEqual([GENERAL], names(r))

    def test_never_returns_more_than_three(self):
        r = suggest([
            {"name": "肌酐", "direction": "high"},
            {"name": "谷丙转氨酶", "direction": "high"},
            {"name": "血红蛋白", "direction": "low"},
            {"name": "肌钙蛋白", "direction": "high"},
            {"name": "c反应蛋白", "direction": "high"},
        ])
        self.assertLessEqual(len(r["departments"]), 3)

    def test_reasons_are_present(self):
        r = suggest([{"name": "肌酐", "direction": "high"}])
        self.assertTrue(r["departments"][0]["reasons"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
