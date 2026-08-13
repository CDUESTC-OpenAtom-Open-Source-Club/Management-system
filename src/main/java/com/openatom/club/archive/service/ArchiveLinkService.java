package com.openatom.club.archive.service;

import com.openatom.club.archive.dto.ArchiveLinkRequest;
import com.openatom.club.archive.dto.ArchiveLinkResponse;
import com.openatom.club.archive.entity.ArchiveLink;
import com.openatom.club.archive.repository.ArchiveLinkRepository;
import com.openatom.club.common.exception.BizException;
import com.openatom.club.common.response.PageResult;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.log.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArchiveLinkService {
    private final ArchiveLinkRepository archiveLinkRepository;
    private final PermissionChecker permissionChecker;
    private final OperationLogService logService;

    public PageResult<ArchiveLinkResponse> list(Integer year, String type, String keyword, int page, int size) {
        Page<ArchiveLink> linkPage = archiveLinkRepository.search(
                year,
                StringUtils.hasText(type) ? type : null,
                StringUtils.hasText(keyword) ? keyword : null,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<ArchiveLinkResponse> list = linkPage.getContent().stream()
                .map(ArchiveLinkResponse::from).toList();
        return new PageResult<>(list, linkPage.getTotalElements(), page, size);
    }

    @Transactional
    public ArchiveLinkResponse create(ArchiveLinkRequest req) {
        permissionChecker.requireManage();
        ArchiveLink link = new ArchiveLink();
        link.setTitle(req.getTitle());
        link.setArchiveYear(req.getArchiveYear());
        link.setArchiveType(StringUtils.hasText(req.getArchiveType()) ? req.getArchiveType() : "其他");
        link.setUrl(req.getUrl());
        link.setDescription(req.getDescription());
        link.setCreatedBy(ActorHolder.get().getName());
        link.setUpdatedBy(ActorHolder.get().getName());
        return ArchiveLinkResponse.from(archiveLinkRepository.save(link));
    }

    @Transactional
    public ArchiveLinkResponse update(Long id, ArchiveLinkRequest req) {
        permissionChecker.requireManage();
        ArchiveLink link = archiveLinkRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("资料归档不存在"));
        link.setTitle(req.getTitle());
        link.setArchiveYear(req.getArchiveYear());
        if (StringUtils.hasText(req.getArchiveType())) link.setArchiveType(req.getArchiveType());
        link.setUrl(req.getUrl());
        link.setDescription(req.getDescription());
        link.setUpdatedBy(ActorHolder.get().getName());
        return ArchiveLinkResponse.from(archiveLinkRepository.save(link));
    }

    @Transactional
    public void delete(Long id) {
        permissionChecker.requireManage();
        ArchiveLink link = archiveLinkRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("资料归档不存在"));
        link.setDeletedAt(OffsetDateTime.now());
        archiveLinkRepository.save(link);
        logService.log("archive", "DELETE", String.valueOf(id), "删除归档链接: " + link.getTitle());
    }
}
