package com.openatom.club.log.service;

import com.openatom.club.common.response.PageResult;
import com.openatom.club.common.security.ActorContext;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.log.dto.OperationLogResponse;
import com.openatom.club.log.entity.OperationLog;
import com.openatom.club.log.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {
    private final OperationLogRepository logRepository;
    private final PermissionChecker permissionChecker;

    public void log(String moduleName, String actionType, String targetId, String description) {
        try {
            ActorContext actor = ActorHolder.get();
            OperationLog opLog = new OperationLog();
            opLog.setOperatorName(actor.getName());
            opLog.setOperatorDepartment(actor.getDepartment());
            opLog.setOperatorPosition(actor.getPosition());
            opLog.setModuleName(moduleName);
            opLog.setActionType(actionType);
            opLog.setTargetId(targetId);
            opLog.setDescription(description);
            logRepository.save(opLog);
        } catch (Exception e) {
            log.error("Failed to save operation log", e);
        }
    }

    public PageResult<OperationLogResponse> list(String moduleName, String actionType,
                                                   String keyword, int page, int size) {
        permissionChecker.requireLogAccess();
        Page<OperationLog> logPage = logRepository.search(
                StringUtils.hasText(moduleName) ? moduleName : null,
                StringUtils.hasText(actionType) ? actionType : null,
                StringUtils.hasText(keyword) ? keyword : null,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<OperationLogResponse> list = logPage.getContent().stream()
                .map(OperationLogResponse::from).toList();
        return new PageResult<>(list, logPage.getTotalElements(), page, size);
    }
}
