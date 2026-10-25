package com.superpatient.util;

/**
 * 给解读的问题打一个题型标签，用于统计「用户到底在问什么、哪类最贵」。
 *
 * 纯关键词规则，不调模型：分类是确定性工作，用概率方法只会让统计口径飘。
 * 顺序即优先级——先判「是否越界提问」，再判跨记录/趋势，最后才是取单个值。
 */
public final class InsightClassifier {

    public static final String BOUNDARY = "boundary";
    public static final String CROSS_RECORD = "cross_record";
    public static final String TREND = "trend";
    public static final String ABNORMAL = "abnormal";
    public static final String VALUE = "value";
    public static final String SUMMARY = "summary";
    public static final String OTHER = "other";

    private static final String[] BOUNDARY_WORDS = {
            "严重", "是不是", "要不要", "能不能", "会不会", "得了", "吃什么", "怎么治", "风险"
    };
    // 「上次」单独出现多半是「比上次……」这类趋势问题，所以只匹配更具体的说法
    private static final String[] CROSS_WORDS = {
            "上次检查", "上次的", "上一次", "最近一次", "多久没", "之前", "间隔", "做过"
    };
    private static final String[] TREND_WORDS = {
            "比上次", "变化", "升高", "降低", "高了", "低了", "升还是降", "高还是低", "趋势", "历史", "最近怎么样"
    };
    private static final String[] ABNORMAL_WORDS = {
            "异常", "超出", "偏高", "偏低", "高于", "低于", "超标", "不正常",
            "高吗", "低吗", "正常吗"
    };
    private static final String[] VALUE_WORDS = {
            "是多少", "多少", "数值", "结果", "值"
    };
    private static final String[] SUMMARY_WORDS = {
            "概括", "总结", "总体", "怎么样", "情况"
    };

    private InsightClassifier() {
    }

    public static String classify(String question) {
        if (question == null || question.isBlank()) return OTHER;
        if (hit(question, BOUNDARY_WORDS)) return BOUNDARY;
        if (hit(question, CROSS_WORDS)) return CROSS_RECORD;
        if (hit(question, TREND_WORDS)) return TREND;
        if (hit(question, ABNORMAL_WORDS)) return ABNORMAL;
        if (hit(question, VALUE_WORDS)) return VALUE;
        if (hit(question, SUMMARY_WORDS)) return SUMMARY;
        return OTHER;
    }

    private static boolean hit(String question, String[] words) {
        for (String w : words) {
            if (question.contains(w)) return true;
        }
        return false;
    }
}
