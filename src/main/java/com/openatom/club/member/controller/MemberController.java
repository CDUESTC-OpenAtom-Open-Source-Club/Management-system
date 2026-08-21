package com.openatom.club.member.controller;

import com.openatom.club.common.response.ApiResponse;
import com.openatom.club.common.response.PageResult;
import com.openatom.club.member.dto.BatchDeleteMembersRequest;
import com.openatom.club.member.dto.BatchDeleteMembersResponse;
import com.openatom.club.member.dto.BatchSetCohortRequest;
import com.openatom.club.member.dto.MemberRequest;
import com.openatom.club.member.dto.MemberResponse;
import com.openatom.club.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "成员管理")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @Operation(summary = "查询成员列表（cohortId: 空=全部，-1=未分届）")
    @GetMapping
    public ApiResponse<PageResult<MemberResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cohortId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(memberService.list(keyword, cohortId, page, size));
    }

    @Operation(summary = "修改成员")
    @PutMapping("/{id}")
    public ApiResponse<MemberResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody MemberRequest req) {
        return ApiResponse.success(memberService.update(id, req));
    }

    @Operation(summary = "删除成员（软删除成员 + 清理积分数据 + 禁用账号）")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        memberService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "批量删除成员（软删除成员 + 清理积分数据 + 禁用账号）")
    @PostMapping("/batch-delete")
    public ApiResponse<BatchDeleteMembersResponse> batchDelete(@Valid @RequestBody BatchDeleteMembersRequest req) {
        return ApiResponse.success(memberService.batchDelete(req.getMemberIds()));
    }

    @Operation(summary = "查看成员详情")
    @GetMapping("/{id}")
    public ApiResponse<MemberResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(memberService.getById(id));
    }

    @Operation(summary = "批量设置成员届次")
    @PutMapping("/batch-cohort")
    public ApiResponse<Integer> batchSetCohort(@Valid @RequestBody BatchSetCohortRequest req) {
        return ApiResponse.success(memberService.batchSetCohort(req.getMemberIds(), req.getCohortId()));
    }
}
