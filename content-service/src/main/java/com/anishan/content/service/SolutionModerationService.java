package com.anishan.content.service;

import com.anishan.api.util.AccountPolicy;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.content.domain.dto.ModerationPageQuery;
import com.anishan.content.domain.dto.SolutionModerationRequest;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.anishan.content.domain.vo.ModerationActionVo;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.mapper.SolutionModerationActionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Lock-free orchestration for remote eligibility; the writer owns the atomic local transaction. */
@Service
@RequiredArgsConstructor
public class SolutionModerationService {
    private final SolutionQueryService query;
    private final SolutionProblemReferenceService references;
    private final SolutionAccessService access;
    private final SolutionLocalWriteService writer;
    private final SolutionModerationActionMapper auditMapper;
    private final SolutionDomainMigrationGate gate;

    public SolutionModerationState moderate(Long id, SolutionModerationRequest request) {
        Long operator = operator("problem:solution:edit");
        gate.requireCutover();
        if (request == null || request.getAction() == null) throw new ApiStatusException(400, "处理动作有误");
        String reason = reason(request.getReason());
        SolutionRecord before = requireRecord(id);
        boolean publicReadable = false;
        if (request.getAction() == SolutionModerationRequest.Action.RESTORE
                && before.getModerationState() != SolutionModerationState.NORMAL
                && !Boolean.TRUE.equals(before.getDelFlag())) {
            publicReadable = access.isPubliclyReadable(references.refresh(
                    Collections.singletonList(before.getProblemId())).get(before.getProblemId()));
        }
        return writer.moderate(before, operator, request.getAction().name(), reason, publicReadable);
    }

    public void delete(Long id, String rawReason) {
        Long operator = operator("problem:solution:remove");
        gate.requireCutover();
        String reason = reason(rawReason);
        writer.moderate(requireRecord(id), operator, "DELETE", reason, false);
    }

    public List<ModerationActionVo> ownActions(Long id) {
        SolutionRecord row = requireRecord(id); // Deleted content still has a private processing history.
        Long viewer = AuthUtil.getUserId();
        if (viewer == null || !Objects.equals(row.getUserId(), viewer))
            throw new ApiStatusException(404, "题解不存在或无权访问");
        return auditMapper.selectOwnSolutionActions(id, viewer);
    }

    @Transactional(readOnly = true)
    public PagedResult<ModerationActionVo> history(Long id, ModerationPageQuery page) {
        AccountPolicy.requireAuthority("problem:solution:list");
        requireRecord(id);
        if (page == null || page.getCurrentPage() == null || page.getPageSize() == null
                || page.getCurrentPage() < 1 || page.getPageSize() < 1 || page.getPageSize() > 50)
            throw new ApiStatusException(400, "分页参数有误");
        long offset;
        try { offset = Math.multiplyExact(page.getCurrentPage() - 1, page.getPageSize()); }
        catch (ArithmeticException invalid) { throw new ApiStatusException(400, "分页参数有误"); }
        PagedResult<ModerationActionVo> result = new PagedResult<>();
        result.setCurrentPage(page.getCurrentPage()); result.setPageSize(page.getPageSize());
        result.setTotalRecords(auditMapper.countHistory(id));
        result.setData(auditMapper.selectHistory(id, offset, page.getPageSize()));
        return result;
    }

    private Long operator(String permission) {
        AccountPolicy.requireAuthority(permission);
        Long id = AccountPolicy.requireAllowed(AccountPolicy.SOLUTION_DENY);
        if (id <= 0) throw new ApiStatusException(403, "该账号不能执行社交操作");
        return id;
    }

    private SolutionRecord requireRecord(Long id) {
        if (id == null || id <= 0) throw new ApiStatusException(404, "题解不存在");
        SolutionRecord row = query.record(id);
        if (row == null) throw new ApiStatusException(404, "题解不存在");
        return row;
    }

    private String reason(String raw) {
        if (raw == null) throw new ApiStatusException(400, "请填写处理原因");
        String value = Normalizer.normalize(raw.replace("\r\n", "\n"), Normalizer.Form.NFC).strip();
        if (value.isEmpty() || value.codePointCount(0, value.length()) > 500)
            throw new ApiStatusException(400, "处理原因须为1到500个字符");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isHighSurrogate(ch)) {
                if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i)))
                    throw new ApiStatusException(400, "处理原因包含非法字符");
            } else if (Character.isLowSurrogate(ch) || (Character.isISOControl(ch) && ch != '\n' && ch != '\t')) {
                throw new ApiStatusException(400, "处理原因包含非法字符");
            }
        }
        return value;
    }
}
