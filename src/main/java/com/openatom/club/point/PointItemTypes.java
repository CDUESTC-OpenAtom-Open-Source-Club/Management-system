package com.openatom.club.point;

import java.util.List;
import java.util.Set;

/**
 * 积分项目类型统一定义。
 * 展示顺序严格保持 ALL 中的顺序，业务代码禁止散落硬编码类型值。
 */
public final class PointItemTypes {

    public static final String ACTIVITY = "活动";
    public static final String COMPETITION = "比赛";
    public static final String OPEN_SOURCE_LEARNING = "开源学习";
    public static final String COMMUNITY_CONTRIBUTION = "社区贡献";
    public static final String SPEECH_HOST = "演讲或主持";
    public static final String OTHER = "其他";

    /** 展示顺序固定为以下顺序 */
    public static final List<String> ALL = List.of(
            ACTIVITY,
            COMPETITION,
            OPEN_SOURCE_LEARNING,
            COMMUNITY_CONTRIBUTION,
            SPEECH_HOST,
            OTHER);

    private static final Set<String> VALID = Set.copyOf(ALL);

    private PointItemTypes() {
    }

    /** 是否为合法类型（严格等于 6 个合法值之一） */
    public static boolean isValid(String type) {
        return type != null && VALID.contains(type);
    }
}
