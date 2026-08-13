package com.openatom.club.finance.service;

import com.openatom.club.common.exception.BizException;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.file.entity.FileRecord;
import com.openatom.club.file.service.FileStorageService;
import com.openatom.club.finance.dto.*;
import com.openatom.club.finance.entity.FinanceFile;
import com.openatom.club.finance.entity.FinancePeriod;
import com.openatom.club.finance.repository.FinanceFileRepository;
import com.openatom.club.finance.repository.FinancePeriodRepository;
import com.openatom.club.log.service.OperationLogService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FinanceService {
    private static final Set<String> WORD_EXTS = Set.of("doc", "docx");
    private static final Set<String> VOUCHER_EXTS = Set.of("jpg", "jpeg", "png", "pdf", "doc", "docx");

    private final FinancePeriodRepository periodRepository;
    private final FinanceFileRepository financeFileRepository;
    private final FileStorageService fileStorageService;
    private final PermissionChecker permissionChecker;
    private final OperationLogService logService;

    public List<FinancePeriodResponse> listPeriods(Integer year) {
        permissionChecker.requireFinanceAccess();
        List<FinancePeriod> periods = year != null
                ? periodRepository.findAllByFinanceYearOrderByFinanceMonthAsc(year)
                : periodRepository.findAllByOrderByFinanceYearDescFinanceMonthDesc();
        return periods.stream().map(FinancePeriodResponse::from).toList();
    }

    public FinanceMonthDetailResponse getMonthDetail(Integer year, Integer month) {
        permissionChecker.requireFinanceAccess();
        FinancePeriod period = periodRepository.findByFinanceYearAndFinanceMonth(year, month)
                .orElseThrow(() -> BizException.of("该月份财务数据不存在"));
        List<FinanceFile> allFiles = financeFileRepository.findAllByPeriodIdAndDeletedAtIsNull(period.getId());
        FinanceFileResponse report = null;
        List<FinanceFileResponse> vouchers = new ArrayList<>();
        for (FinanceFile f : allFiles) {
            FinanceFileResponse dto = FinanceFileResponse.from(f);
            // 填充文件元数据
            FileRecord fileRecord = fileStorageService.getFileRecord(f.getFileId());
            if (fileRecord != null) {
                dto.setOriginalName(fileRecord.getOriginalName());
                dto.setFileSize(fileRecord.getFileSize());
                dto.setContentType(fileRecord.getContentType());
            }
            if ("REPORT".equals(f.getFileType())) report = dto;
            else vouchers.add(dto);
        }
        FinanceMonthDetailResponse detail = new FinanceMonthDetailResponse();
        detail.setPeriod(FinancePeriodResponse.from(period));
        detail.setReport(report);
        detail.setVouchers(vouchers);
        return detail;
    }

    @Transactional
    public FinanceFileResponse uploadReport(Integer year, Integer month, MultipartFile file, String remark) throws IOException {
        permissionChecker.requireFinanceAccess();
        FinancePeriod period = getOrCreatePeriod(year, month);
        String subDir = "finance/" + year + "/" + month + "/report";
        // Soft-delete existing REPORT
        Optional<FinanceFile> existingReport = financeFileRepository
                .findByPeriodIdAndFileTypeAndDeletedAtIsNull(period.getId(), "REPORT");
        existingReport.ifPresent(old -> {
            old.setDeletedAt(OffsetDateTime.now());
            financeFileRepository.save(old);
            fileStorageService.softDeleteFileRecord(old.getFileId());
            logService.log("finance", "DELETE", String.valueOf(old.getId()), "替换财务负责人 REPORT");
        });
        FileRecord fileRecord = fileStorageService.saveFile(file, "finance", subDir, WORD_EXTS);
        FinanceFile financeFile = new FinanceFile();
        financeFile.setPeriodId(period.getId());
        financeFile.setFileId(fileRecord.getId());
        financeFile.setFileType("REPORT");
        financeFile.setRemark(remark);
        financeFile.setCreatedBy(ActorHolder.get().getName());
        FinanceFile saved = financeFileRepository.save(financeFile);
        logService.log("finance", "UPLOAD", String.valueOf(saved.getId()),
                "上传财务报表: " + year + "-" + month);
        return FinanceFileResponse.from(saved);
    }

    @Transactional
    public List<FinanceFileResponse> uploadVouchers(Integer year, Integer month,
                                                      List<MultipartFile> files, String remark) throws IOException {
        permissionChecker.requireFinanceAccess();
        FinancePeriod period = getOrCreatePeriod(year, month);
        String subDir = "finance/" + year + "/" + month + "/vouchers";
        List<FinanceFileResponse> results = new ArrayList<>();
        for (MultipartFile file : files) {
            FileRecord fileRecord = fileStorageService.saveFile(file, "finance", subDir, VOUCHER_EXTS);
            FinanceFile financeFile = new FinanceFile();
            financeFile.setPeriodId(period.getId());
            financeFile.setFileId(fileRecord.getId());
            financeFile.setFileType("VOUCHER");
            financeFile.setRemark(remark);
            financeFile.setCreatedBy(ActorHolder.get().getName());
            results.add(FinanceFileResponse.from(financeFileRepository.save(financeFile)));
        }
        logService.log("finance", "UPLOAD", null,
                "上传凭据文件 " + files.size() + "个: " + year + "-" + month);
        return results;
    }

    public void download(Long financeFileId, HttpServletResponse response) throws IOException {
        permissionChecker.requireFinanceAccess();
        FinanceFile financeFile = financeFileRepository.findByIdAndDeletedAtIsNull(financeFileId)
                .orElseThrow(() -> BizException.of("财务文件不存在"));
        fileStorageService.downloadFile(financeFile.getFileId(), response);
    }

    public void view(Long financeFileId, HttpServletResponse response) throws IOException {
        permissionChecker.requireFinanceAccess();
        FinanceFile financeFile = financeFileRepository.findByIdAndDeletedAtIsNull(financeFileId)
                .orElseThrow(() -> BizException.of("财务文件不存在"));
        fileStorageService.viewFile(financeFile.getFileId(), response);
    }

    @Transactional
    public void delete(Long financeFileId) {
        permissionChecker.requireFinanceAccess();
        FinanceFile financeFile = financeFileRepository.findByIdAndDeletedAtIsNull(financeFileId)
                .orElseThrow(() -> BizException.of("财务文件不存在"));
        financeFile.setDeletedAt(OffsetDateTime.now());
        financeFileRepository.save(financeFile);
        fileStorageService.softDeleteFileRecord(financeFile.getFileId());
        logService.log("finance", "DELETE", String.valueOf(financeFileId), "删除财务文件 #" + financeFileId);
    }

    private FinancePeriod getOrCreatePeriod(Integer year, Integer month) {
        return periodRepository.findByFinanceYearAndFinanceMonth(year, month)
                .orElseGet(() -> {
                    FinancePeriod p = new FinancePeriod();
                    p.setFinanceYear(year);
                    p.setFinanceMonth(month);
                    return periodRepository.save(p);
                });
    }
}
