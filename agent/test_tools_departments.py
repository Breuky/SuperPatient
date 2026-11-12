# -*- coding: utf-8 -*-
"""suggest_departments 工具单测：不联网，桩掉 Java 接口"""
import json
import unittest
from unittest.mock import patch

import tools


def _overview(**kw):
    base = {
        "totalRecords": 5,
        "topAbnormal": [
            {"name": "肌酐", "unit": "μmol/L", "times": 3},
            {"name": "尿蛋白", "unit": None, "times": 2},
            {"name": "血红蛋白", "unit": "g/L", "times": 1},
        ],
    }
    base.update(kw)
    return base


class SuggestDepartmentsTest(unittest.TestCase):

    def setUp(self):
        self.box = tools.ToolBox("http://localhost:8081", "fake-token")

    def test_returns_ranked_departments_from_abnormal(self):
        with patch.object(tools.ToolBox, "_get", return_value=_overview()):
            payload = json.loads(self.box.call("suggest_departments", {"limit": 10}))

        names = [d["name"] for d in payload["departments"]]
        self.assertEqual("肾内科", names[0], "肌酐+尿蛋白应指向肾内科")
        self.assertIn("based_on", payload)
        self.assertEqual(3, len(payload["based_on"]))
        self.assertTrue(payload["note"])

    def test_no_abnormal_returns_empty_with_note(self):
        with patch.object(tools.ToolBox, "_get", return_value={"topAbnormal": []}):
            payload = json.loads(self.box.call("suggest_departments", {}))

        self.assertEqual([], payload["departments"])
        self.assertIn("没有异常记录", payload["note"])

    def test_unknown_indicator_goes_to_unmapped(self):
        data = {"topAbnormal": [{"name": "某种没收录的指标", "times": 1}]}
        with patch.object(tools.ToolBox, "_get", return_value=data):
            payload = json.loads(self.box.call("suggest_departments", {}))

        self.assertEqual(["某种没收录的指标"], payload["unmapped"])
        self.assertEqual("全科医学科", payload["departments"][0]["name"])

    def test_find_hospitals_passes_department(self):
        rows = [{"name": "浙江大学医学院附属第一医院", "level": "三甲",
                 "department": "肾内科", "specialtyLevel": None,
                 "bookingUrl": None, "verified": False}]
        with patch.object(tools.ToolBox, "_get", return_value=rows) as m:
            payload = json.loads(self.box.call("find_hospitals",
                                               {"city": "杭州", "department": "肾内科"}))

        self.assertEqual(1, len(payload["hospitals"]))
        self.assertFalse(payload["hospitals"][0]["verified"])
        self.assertIn("department=", m.call_args[0][0])

    def test_find_hospitals_empty_has_note(self):
        with patch.object(tools.ToolBox, "_get", return_value=[]):
            payload = json.loads(self.box.call("find_hospitals", {"department": "眼科"}))
        self.assertEqual([], payload["hospitals"])
        self.assertIn("暂无", payload["note"])

    def test_trace_is_recorded(self):
        with patch.object(tools.ToolBox, "_get", return_value=_overview()):
            self.box.call("suggest_departments", {"limit": 5})

        self.assertEqual(1, len(self.box.trace))
        self.assertEqual("suggest_departments", self.box.trace[0]["tool"])
        self.assertTrue(self.box.trace[0]["ok"])


if __name__ == "__main__":
    unittest.main(verbosity=2)