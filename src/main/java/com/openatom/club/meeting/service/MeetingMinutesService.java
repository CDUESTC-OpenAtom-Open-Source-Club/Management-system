package com.openatom.club.meeting.service;

import com.openatom.club.common.exception.BizException;
import com.openatom.club.common.response.PageResult;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.file.entity.FileRecord;
import com.openatom.club.file.service.FileStorageService;
import com.openatom.club.log.service.OperationLogService;
import com.openatom.club.meeting.dto.MeetingMinutesResponse;
import com.openatom.club.meeting.entity.MeetingMinutes;
import com.openatom.club.meeting.repository.MeetingMinutesRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MeetingMinutesService {
    private static final Set<String> ALLOWED_EXTS = Set.of("doc", "docx");
    private final MeetingMinutesRepository meetingMinutesRepository;
    private final FileStorageService fileStorageService;
    private final PermissionChecker permissionChecker;
    private final OperationLogService logService;

    public PageResult<MeetingMinutesResponse> list(Integer year, Integer month, String keyword, int page, int size) {
        Page<MeetingMinutes> meetingPage = meetingMinutesRepository.search(
                year, month,
                StringUtils.hasText(keyword) ? keyword : null,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "meetingDate"))
        );
        List<MeetingMinutesResponse> list = meetingPage.getContent().stream()
                .map(MeetingMinutesResponse::from).toList();
        return new PageResult<>(list, meetingPage.getTotalElements(), page, size);
    }

    @Transactional
    public MeetingMinutesResponse create(String title, String meetingDateStr,
                                          String remark, MultipartFile file) throws IOException {
        permissionChecker.requireManage();
        LocalDate date = LocalDate.parse(meetingDateStr);
        String subDir = "meeting-minutes/" + date.getYear() + "/" + date.getMonthValue();
        FileRecord fileRecord = fileStorageService.saveFile(file, "meeting", subDir, ALLOWED_EXTS);

        MeetingMinutes minutes = new MeetingMinutes();
        minutes.setTitle(title);
        minutes.setMeetingDate(date);
        minutes.setMeetingYear(date.getYear());
        minutes.setMeetingMonth(date.getMonthValue());
        minutes.setFileId(fileRecord.getId());
        minutes.setRemark(remark);
        minutes.setCreatedBy(ActorHolder.get().getName());
        minutes.setUpdatedBy(ActorHolder.get().getName());
        MeetingMinutes saved = meetingMinutesRepository.save(minutes);
        logService.log("meeting", "UPLOAD", String.valueOf(saved.getId()),
                "上传会议纪要: " + title);
        return MeetingMinutesResponse.from(saved);
    }

    public void download(Long id, HttpServletResponse response) throws IOException {
        MeetingMinutes minutes = meetingMinutesRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("会议纪要不存在"));
        if (minutes.getFileId() == null) throw BizException.of("该纪要没有关联文件");
        fileStorageService.downloadFile(minutes.getFileId(), response);
    }

    @Transactional
    public MeetingMinutesResponse update(Long id, String title, String meetingDateStr,
                                          String remark, MultipartFile file) throws IOException {
        permissionChecker.requireManage();
        MeetingMinutes minutes = meetingMinutesRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("会议纪要不存在"));
        if (StringUtils.hasText(title)) minutes.setTitle(title);
        if (StringUtils.hasText(meetingDateStr)) {
            LocalDate date = LocalDate.parse(meetingDateStr);
            minutes.setMeetingDate(date);
            minutes.setMeetingYear(date.getYear());
            minutes.setMeetingMonth(date.getMonthValue());
        }
        if (remark != null) minutes.setRemark(remark);
        if (file != null && !file.isEmpty()) {
            LocalDate date = minutes.getMeetingDate();
            String subDir = "meeting-minutes/" + date.getYear() + "/" + date.getMonthValue();
            FileRecord newFile = fileStorageService.saveFile(file, "meeting", subDir, ALLOWED_EXTS);
            if (minutes.getFileId() != null) {
                fileStorageService.softDeleteFileRecord(minutes.getFileId());
            }
            minutes.setFileId(newFile.getId());
        }
        minutes.setUpdatedBy(ActorHolder.get().getName());
        MeetingMinutes saved = meetingMinutesRepository.save(minutes);
        logService.log("meeting", "UPDATE", String.valueOf(id), "修改会议纪要: " + minutes.getTitle());
        return MeetingMinutesResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        permissionChecker.requireManage();
        MeetingMinutes minutes = meetingMinutesRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("会议纪要不存在"));
        minutes.setDeletedAt(OffsetDateTime.now());
        meetingMinutesRepository.save(minutes);
        if (minutes.getFileId() != null) {
            fileStorageService.softDeleteFileRecord(minutes.getFileId());
        }
        logService.log("meeting", "DELETE", String.valueOf(id), "删除会议纪要: " + minutes.getTitle());
    }
}
