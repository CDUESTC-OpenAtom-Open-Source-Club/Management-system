package com.openatom.club.common.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前操作者上下文
 * V2：来源改为 JWT → UserAccount → Member，不再依赖 X-Actor-* 请求头
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActorContext {

    // 来自 JWT 的登录信息
    private Long userId;
    private String username;
    private Long memberId;

    // 来自绑定 Member 的身份信息
    private String name;
    private String department;
    private String position;

    // 届次归属（每次请求从 member 实时读取）
    private Long cohortId;
    private Integer cohortYear;

    /** 兼容旧构造方式（仅 name/department/position） */
    public ActorContext(String name, String department, String position) {
        this.name = name;
        this.department = department;
        this.position = position;
    }

    public boolean isPresident() {
        return "会长".equals(position);
    }

    public boolean isVicePresident() {
        return "副会长".equals(position);
    }

    public boolean isSecretary() {
        return "秘书处".equals(department);
    }

    /**
     * 全部权限：会长 | 副会长 | 秘书处成员
     * 含义：可以管理成员、积分、归档、会议、财务、操作日志、账号
     */
    public boolean isFullAccess() {
        return isPresident() || isVicePresident() || isSecretary();
    }

    /** 兼容旧代码 */
    public boolean isAdmin() {
        return isFullAccess();
    }

    /** 兼容旧代码 */
    public boolean hasManagePermission() {
        return isFullAccess();
    }
}
