package com.openatom.club.archive.service;

import com.openatom.club.archive.dto.ArchiveLinkRequest;
import com.openatom.club.archive.dto.ArchiveLinkResponse;
import com.openatom.club.archive.entity.ArchiveCohort;
import com.openatom.club.archive.entity.ArchiveLink;
import com.openatom.club.archive.repository.ArchiveCohortRepository;
import com.openatom.club.archive.repository.ArchiveLinkRepository;
import com.openatom.club.cohort.dto.CohortResponse;
import com.openatom.club.cohort.entity.Cohort;
import com.openatom.club.cohort.repository.CohortRepository;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArchiveLinkService {
    private final ArchiveLinkRepository archiveLinkRepository;
    private final ArchiveCohortRepository archiveCohortRepository;
    private final CohortRepository cohortRepository;
    private final PermissionChecker permissionChecker;
    private final OperationLogService logService;

    /**
     * @param cohortId null=全部；-1=未分届；其他=指定届次
     */
    public PageResult<ArchiveLinkResponse> list(Integer year, String type, Long cohortId, String keyword, int page, int size) {
        Page<ArchiveLink> linkPage = archiveLinkRepository.search(
                year,
                StringUtils.hasText(type) ? type : null,
                cohortId,
                StringUtils.hasText(keyword) ? keyword : null,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        Map<Long, Integer> years = cohortYearMap();
        List<ArchiveLinkResponse> list = linkPage.getContent().stream()
                .map(a -> toResponse(a, years)).toList();
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
        ArchiveLink saved = archiveLinkRepository.save(link);
        saveCohortLinks(saved.getId(), req.getCohortIds());
        logService.log("archive", "CREATE", String.valueOf(saved.getId()),
                "新增归档链接: " + saved.getTitle());
        return toResponse(saved, cohortYearMap());
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
        ArchiveLink saved = archiveLinkRepository.save(link);
        if (req.getCohortIds() != null) {
            saveCohortLinks(saved.getId(), req.getCohortIds());
        }
        logService.log("archive", "UPDATE", String.valueOf(id),
                "修改归档链接: " + saved.getTitle());
        return toResponse(saved, cohortYearMap());
    }

    @Transactional
    public void delete(Long id) {
        permissionChecker.requireManage();
        ArchiveLink link = archiveLinkRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("资料归档不存在"));
        link.setDeletedAt(OffsetDateTime.now());
        archiveLinkRepository.save(link);
        archiveCohortRepository.deleteByArchiveId(id);
        logService.log("archive", "DELETE", String.valueOf(id), "删除归档链接: " + link.getTitle());
    }

    private void saveCohortLinks(Long archiveId, List<Long> cohortIds) {
        archiveCohortRepository.deleteByArchiveId(archiveId);
        if (cohortIds != null) {
            for (Long cohortId : cohortIds) {
                ArchiveCohort link = new ArchiveCohort();
                link.setArchiveId(archiveId);
                link.setCohortId(cohortId);
                archiveCohortRepository.save(link);
            }
        }
    }

    private ArchiveLinkResponse toResponse(ArchiveLink a, Map<Long, Integer> years) {
        ArchiveLinkResponse dto = ArchiveLinkResponse.from(a);
        List<CohortResponse> cohorts = archiveCohortRepository.findByArchiveId(a.getId()).stream().map(l -> {
            CohortResponse c = new CohortResponse();
            c.setId(l.getCohortId());
            c.setYear(years.get(l.getCohortId()));
            c.setEnabled(true);
            return c;
        }).toList();
        dto.setCohorts(cohorts);
        return dto;
    }

    private Map<Long, Integer> cohortYearMap() {
        return cohortRepository.findAllByDeletedAtIsNullOrderByYearDesc().stream()
                .collect(Collectors.toMap(Cohort::getId, Cohort::getYear));
    }
}
