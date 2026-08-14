package com.openatom.club.member.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BatchDeleteMembersResponse {

    /** 实际删除的成员数 */
    private int deletedMemberCount;

    /** 同步删除的积分申请数 */
    private int deletedPointApplicationCount;

    /** 同步删除的积分记录数 */
    private int deletedPointRecordCount;

    /** 被禁用的关联账号数 */
    private int disabledAccountCount;
}
