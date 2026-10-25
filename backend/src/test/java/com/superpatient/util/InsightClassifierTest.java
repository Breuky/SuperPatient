package com.superpatient.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 题型分类：标签用于统计，宁可归到 other 也不要乱贴
 */
class InsightClassifierTest {

    @Test
    void boundaryQuestionsComeFirst() {
        assertEquals(InsightClassifier.BOUNDARY, InsightClassifier.classify("这个结果严重吗？"));
        assertEquals(InsightClassifier.BOUNDARY, InsightClassifier.classify("我是不是得了糖尿病？"));
        assertEquals(InsightClassifier.BOUNDARY, InsightClassifier.classify("要不要吃药"));
    }

    @Test
    void crossRecordBeatsTrend() {
        assertEquals(InsightClassifier.CROSS_RECORD, InsightClassifier.classify("上次检查是什么时候？"));
        assertEquals(InsightClassifier.CROSS_RECORD, InsightClassifier.classify("我做过 CT 吗？"));
    }

    @Test
    void trendQuestions() {
        assertEquals(InsightClassifier.TREND, InsightClassifier.classify("总胆固醇比上次高还是低？"));
        assertEquals(InsightClassifier.TREND, InsightClassifier.classify("我的血糖最近怎么样？"));
    }

    @Test
    void abnormalOverview() {
        assertEquals(InsightClassifier.ABNORMAL, InsightClassifier.classify("这次有哪些指标超出参考区间？"));
        assertEquals(InsightClassifier.ABNORMAL, InsightClassifier.classify("血红蛋白正常吗"));
    }

    @Test
    void singleValueQuestion() {
        assertEquals(InsightClassifier.VALUE, InsightClassifier.classify("白细胞计数是多少？"));
    }

    @Test
    void summaryQuestion() {
        assertEquals(InsightClassifier.SUMMARY, InsightClassifier.classify("帮我概括这次检查"));
    }

    @Test
    void unknownFallsBackToOther() {
        assertEquals(InsightClassifier.OTHER, InsightClassifier.classify("看看甘油三酯"));
        assertEquals(InsightClassifier.OTHER, InsightClassifier.classify(null));
        assertEquals(InsightClassifier.OTHER, InsightClassifier.classify("  "));
    }
}
