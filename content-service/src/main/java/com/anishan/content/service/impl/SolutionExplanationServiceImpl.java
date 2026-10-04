package com.anishan.content.service.impl;

import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.api.client.user.client.UserInternalClient;
import com.anishan.api.util.AccountPolicy;
import com.anishan.content.domain.dto.DetailSolutionDto;
import com.anishan.content.domain.dto.PagedSolution;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.anishan.content.domain.vo.DetailSolutionVo;
import com.anishan.content.domain.vo.SolutionCandidate;
import com.anishan.content.domain.vo.SolutionCandidatePage;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.domain.vo.SolutionVo;
import com.anishan.content.service.SolutionAccessService;
import com.anishan.content.service.SolutionDomainMigrationGate;
import com.anishan.content.service.SolutionExplanationService;
import com.anishan.content.service.SolutionLocalWriteService;
import com.anishan.content.service.SolutionLikeService;
import com.anishan.content.service.SolutionProblemReferenceService;
import com.anishan.content.service.SolutionQueryService;
import com.anishan.commons.domain.R;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.exception.ApiStatusException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SolutionExplanationServiceImpl implements SolutionExplanationService {
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_REPAIR_ROUNDS = 3;
    private static final String UNAVAILABLE_PROBLEM_TITLE = "关联题目不可用";

    private final SolutionQueryService queryService;
    private final SolutionAccessService accessService;
    private final SolutionProblemReferenceService referenceService;
    private final SolutionDomainMigrationGate migrationGate;
    private final SolutionLocalWriteService writeService;
    private final SolutionLikeService likeService;
    private final UserInternalClient userInternalClient;

    @Override
    public DetailSolutionVo get(Long id, Long userId) {
        return detail(id, userId, false);
    }

    @Override
    public DetailSolutionVo adminGet(Long id) {
        return detail(id, null, true);
    }

    @Override
    public DetailSolutionVo adminGet(Long id, Long viewerId) {
        return detail(id, viewerId, true);
    }

    private DetailSolutionVo detail(Long solutionId, Long viewerId, boolean admin) {
        if (solutionId == null || solutionId <= 0) throw new ApiStatusException(404, "题解不存在");
        for (int attempt = 0; attempt < MAX_REPAIR_ROUNDS; attempt++) {
            SolutionRecord before = queryService.record(solutionId);
            if (before == null || Boolean.TRUE.equals(before.getDelFlag())) {
                throw new ApiStatusException(404, "题解不存在或无权访问");
            }
            Map<Long, ContentProblemReadVo> fresh = referenceService.refresh(Collections.singletonList(before.getProblemId()));
            SolutionRecord current = queryService.record(solutionId);
            if (!sameVersionAndProblem(before, current)) continue;
            ContentProblemReadVo problem = fresh.get(current.getProblemId());
            if (!visibleTo(current, viewerId, admin, problem)) {
                throw new ApiStatusException(404, "题解不存在或无权访问");
            }
            DetailSolutionVo result = toDetail(current, problem);
            fillDetailName(result);
            result.setLikedByMe(likeService.isLiked(viewerId, solutionId));
            return result;
        }
        throw new ApiStatusException(503, "题解正在变化，请稍后重试");
    }

    @Override
    public List<SolutionVo> recent() {
        return recent(null);
    }

    @Override
    public List<SolutionVo> recent(Long viewerId) {
        PagedSolution query = new PagedSolution();
        query.setPageSize(20L);
        query.setCurrentPage(1L);
        return queryPage(viewerId, false, true, query, null).getData();
    }

    @Override
    public boolean delete(List<Long> ids, Long userId) {
        AccountPolicy.requireAllowedActor(AccountPolicy.SOLUTION_DENY, userId);
        migrationGate.requireCutover();
        return writeService.deleteOwned(ids, userId);
    }

    @Override
    public boolean adminDelete(List<Long> ids) {
        AccountPolicy.requireAllowed(AccountPolicy.SOLUTION_DENY);
        migrationGate.requireCutover();
        throw new ApiStatusException(400, "管理员删除题解需要填写处理原因");
    }

    @Override
    public PagedResult<SolutionVo> pagedQuery(Long userId, PagedSolution query) {
        return queryPage(userId, false, false, query, query == null ? null : query.getUserId());
    }

    @Override
    public PagedResult<SolutionVo> adminPagedQuery(Long userId, PagedSolution query) {
        return queryPage(userId, true, false, query, query.getUserId());
    }

    @Override
    public boolean update(Long userId, DetailSolutionDto dto) {
        AccountPolicy.requireAllowedActor(AccountPolicy.SOLUTION_DENY, userId);
        migrationGate.requireCutover();
        validateUpdateId(dto);
        SolutionRecord before = requireOwnedActive(dto.getSolutionId(), userId);
        validateImmutableProblem(before, dto);
        ContentProblemReadVo problem = refreshExistingProblem(before.getProblemId());
        validateExistingVisibilityChange(dto, before, problem);
        return writeService.update(userId, dto, before, false);
    }

    @Override
    public boolean adminUpdate(DetailSolutionDto dto) {
        AccountPolicy.requireAllowed(AccountPolicy.SOLUTION_DENY);
        migrationGate.requireCutover();
        validateUpdateId(dto);
        SolutionRecord before = requireActive(dto.getSolutionId());
        validateImmutableProblem(before, dto);
        ContentProblemReadVo problem = refreshExistingProblem(before.getProblemId());
        validateExistingVisibilityChange(dto, before, problem);
        return writeService.update(before.getUserId(), dto, before, true);
    }

    @Override
    public boolean add(Long userId, DetailSolutionDto dto) {
        AccountPolicy.requireAllowedActor(AccountPolicy.SOLUTION_DENY, userId);
        migrationGate.requireCutover();
        validateNew(dto);
        ContentProblemReadVo problem = refreshProblem(dto.getProblemId());
        validateNewVisibility(dto, problem);
        return writeService.create(userId, dto, false);
    }

    @Override
    public boolean adminAdd(Long userId, DetailSolutionDto dto) {
        AccountPolicy.requireAllowedActor(AccountPolicy.SOLUTION_DENY, userId);
        migrationGate.requireCutover();
        validateNew(dto);
        ContentProblemReadVo problem = refreshProblem(dto.getProblemId());
        validateNewVisibility(dto, problem);
        return writeService.create(userId, dto, true);
    }

    @Override
    public boolean setTopUp(Long solutionId, boolean topUp) {
        AccountPolicy.requireAllowed(AccountPolicy.SOLUTION_DENY);
        migrationGate.requireCutover();
        return writeService.setTopUp(solutionId, topUp);
    }

    private PagedResult<SolutionVo> queryPage(Long viewerId, boolean admin, boolean publicOnly,
                                               PagedSolution query, Long filterUserId) {
        if (query == null || query.getCurrentPage() == null || query.getPageSize() == null
                || query.getCurrentPage() < 1 || query.getPageSize() < 1 || query.getPageSize() > MAX_PAGE_SIZE) {
            throw new ApiStatusException(400, "分页参数有误");
        }
        final long offset;
        try {
            offset = Math.multiplyExact(query.getCurrentPage() - 1, query.getPageSize());
        } catch (ArithmeticException exception) {
            throw new ApiStatusException(400, "分页参数有误");
        }

        // A problem filter is an explicit refresh trigger so CONTEST -> PUBLIC can reappear
        // without waiting for the periodic projection worker.
        if (query.getProblemId() != null) {
            referenceService.refresh(Collections.singletonList(query.getProblemId()));
        }
        SolutionCandidatePage firstPage = queryService.firstPage(viewerId, admin, publicOnly,
                filterUserId, query.getProblemId(), offset);
        List<SolutionVo> visible = new ArrayList<>();
        List<SolutionCandidate> batch = firstPage.getCandidates();
        SolutionCandidate cursor = null;
        int round = 0;
        boolean exhausted = batch.isEmpty();

        while (!batch.isEmpty() && round < MAX_REPAIR_ROUNDS && visible.size() < query.getPageSize()) {
            round++;
            cursor = batch.get(batch.size() - 1);
            List<Long> problemIds = batch.stream().map(SolutionCandidate::getProblemId)
                    .filter(Objects::nonNull).distinct().collect(Collectors.toList());
            Map<Long, ContentProblemReadVo> fresh = referenceService.refresh(problemIds);
            Map<Long, SolutionRecord> records = queryService.records(batch.stream()
                            .map(SolutionCandidate::getSolutionId).collect(Collectors.toList()))
                    .stream().collect(Collectors.toMap(SolutionRecord::getSolutionId, item -> item,
                            (first, ignored) -> first, LinkedHashMap::new));

            for (SolutionCandidate candidate : batch) {
                SolutionRecord record = records.get(candidate.getSolutionId());
                if (!sameVersionAndProblem(candidate, record)) continue;
                ContentProblemReadVo problem = fresh.get(record.getProblemId());
                if (!visibleTo(record, viewerId, admin, problem)) continue;
                visible.add(toSummary(record, problem));
                if (visible.size() >= query.getPageSize()) break;
            }
            exhausted = batch.size() < 100;
            if (visible.size() < query.getPageSize() && !exhausted && round < MAX_REPAIR_ROUNDS) {
                batch = queryService.nextBatch(viewerId, admin, publicOnly, filterUserId,
                        query.getProblemId(), cursor);
                exhausted = batch.isEmpty();
            } else {
                batch = Collections.emptyList();
            }
        }

        if (visible.size() < query.getPageSize() && !exhausted && cursor != null && round >= MAX_REPAIR_ROUNDS) {
            List<SolutionCandidate> remaining = queryService.nextBatch(viewerId, admin, publicOnly,
                    filterUserId, query.getProblemId(), cursor);
            if (!remaining.isEmpty()) {
                throw new ApiStatusException(503, "题解列表正在变化，请稍后重试");
            }
        }
        if (visible.size() > query.getPageSize()) {
            visible = new ArrayList<>(visible.subList(0, query.getPageSize().intValue()));
        }
        fillNames(visible);
        fillLikedByMe(visible, viewerId);
        PagedResult<SolutionVo> result = new PagedResult<>();
        result.setCurrentPage(query.getCurrentPage());
        result.setPageSize(query.getPageSize());
        result.setTotalRecords(firstPage.getTotal());
        result.setData(visible);
        return result;
    }

    private void fillLikedByMe(List<SolutionVo> solutions, Long viewerId) {
        if (solutions.isEmpty() || viewerId == null || viewerId <= 0) return;
        List<Long> solutionIds = solutions.stream().map(SolutionVo::getSolutionId)
                .filter(Objects::nonNull).collect(Collectors.toList());
        Set<Long> likedIds = likeService.likedSolutionIds(viewerId, solutionIds);
        solutions.forEach(solution -> solution.setLikedByMe(likedIds.contains(solution.getSolutionId())));
    }

    private boolean visibleTo(SolutionRecord record, Long viewerId, boolean admin, ContentProblemReadVo problem) {
        if (record == null || Boolean.TRUE.equals(record.getDelFlag())) return false;
        if (admin) return true;
        if (viewerId != null && Objects.equals(viewerId, record.getUserId())) return true;
        return !Boolean.TRUE.equals(record.getPrivate_())
                && record.getModerationState() == SolutionModerationState.NORMAL
                && accessService.isPubliclyReadable(problem);
    }

    private boolean sameVersionAndProblem(SolutionCandidate candidate, SolutionRecord record) {
        return record != null && Objects.equals(candidate.getVersion(), record.getVersion())
                && Objects.equals(candidate.getProblemId(), record.getProblemId())
                && !Boolean.TRUE.equals(record.getDelFlag());
    }

    private boolean sameVersionAndProblem(SolutionRecord before, SolutionRecord current) {
        return current != null && !Boolean.TRUE.equals(current.getDelFlag())
                && Objects.equals(before.getVersion(), current.getVersion())
                && Objects.equals(before.getProblemId(), current.getProblemId());
    }

    private SolutionVo toSummary(SolutionRecord record, ContentProblemReadVo problem) {
        String preview = record.getContent() == null ? "" : record.getContent();
        if (preview.length() > 250) preview = preview.substring(0, 250);
        return new SolutionVo()
                .setSolutionId(record.getSolutionId())
                .setTitle(record.getTitle())
                .setProblemId(record.getProblemId())
                .setProblemTitle(problem != null && problem.getTitle() != null
                        ? problem.getTitle() : UNAVAILABLE_PROBLEM_TITLE)
                .setUserId(record.getUserId())
                .setTopUp(record.getTopUp())
                .setPrivate_(record.getPrivate_())
                .setLikeCount(record.getLikeCount()).setCommentCount(record.getCommentCount())
                .setCommentsOpen(record.getCommentsOpen()).setModerationState(record.getModerationState())
                .setEffectiveVisibility(effectiveVisibility(record, problem))
                .setContent(preview)
                .setCreateTime(record.getCreateTime())
                .setUpdateTime(record.getUpdateTime());
    }

    private DetailSolutionVo toDetail(SolutionRecord record, ContentProblemReadVo problem) {
        return new DetailSolutionVo()
                .setSolutionId(record.getSolutionId())
                .setTitle(record.getTitle())
                .setProblemId(record.getProblemId())
                .setProblemTitle(problem != null && problem.getTitle() != null
                        ? problem.getTitle() : UNAVAILABLE_PROBLEM_TITLE)
                .setUserId(record.getUserId())
                .setTopUp(record.getTopUp())
                .setPrivate_(record.getPrivate_())
                .setLikeCount(record.getLikeCount()).setCommentCount(record.getCommentCount())
                .setCommentsOpen(record.getCommentsOpen()).setModerationState(record.getModerationState())
                .setEffectiveVisibility(effectiveVisibility(record, problem))
                .setContent(record.getContent() == null ? "" : record.getContent())
                .setCreateTime(record.getCreateTime())
                .setUpdateTime(record.getUpdateTime());
    }

    private String effectiveVisibility(SolutionRecord record, ContentProblemReadVo problem) {
        if (Boolean.TRUE.equals(record.getDelFlag())) return "DELETED";
        return !Boolean.TRUE.equals(record.getPrivate_())
                && record.getModerationState() == SolutionModerationState.NORMAL
                && accessService.isPubliclyReadable(problem) ? "PUBLIC" : "AUTHOR_ONLY";
    }

    private void fillNames(List<? extends SolutionVo> solutions) {
        List<Long> ids = solutions.stream().map(SolutionVo::getUserId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return;
        try {
            R<Map<Long, String>> response = userInternalClient.nikeName(ids);
            Map<Long, String> names = response == null || response.getCode() != 200 || response.getData() == null
                    ? Collections.emptyMap() : response.getData();
            solutions.forEach(solution -> solution.setNikeName(names.get(solution.getUserId())));
        } catch (RuntimeException exception) {
            log.warn("Unable to load solution author summaries; returning solution data without names");
        }
    }

    private void fillDetailName(DetailSolutionVo solution) {
        if (solution.getUserId() == null) return;
        try {
            R<Map<Long, String>> response = userInternalClient.nikeName(Collections.singletonList(solution.getUserId()));
            if (response != null && response.getCode() == 200 && response.getData() != null) {
                solution.setNikeName(response.getData().get(solution.getUserId()));
            }
        } catch (RuntimeException exception) {
            log.warn("Unable to load solution author summary; returning solution data without name");
        }
    }

    private SolutionRecord requireActive(Long id) {
        SolutionRecord record = queryService.record(id);
        if (record == null || Boolean.TRUE.equals(record.getDelFlag())) {
            throw new ApiStatusException(404, "题解不存在");
        }
        return record;
    }

    private SolutionRecord requireOwnedActive(Long id, Long userId) {
        SolutionRecord record = requireActive(id);
        if (!Objects.equals(record.getUserId(), userId)) throw new ApiStatusException(404, "题解不存在");
        return record;
    }

    private ContentProblemReadVo refreshProblem(Long problemId) {
        ContentProblemReadVo problem = referenceService.refresh(Collections.singletonList(problemId)).get(problemId);
        if (problem == null || !problem.isExists() || problem.isDeleted()) {
            throw new ApiStatusException(404, "题目不存在或不可用");
        }
        return problem;
    }

    private ContentProblemReadVo refreshExistingProblem(Long problemId) {
        ContentProblemReadVo problem = referenceService.refresh(Collections.singletonList(problemId)).get(problemId);
        if (problem == null) throw new ApiStatusException(503, "题目状态暂时无法确认，请稍后重试");
        return problem;
    }

    private void validateNew(DetailSolutionDto dto) {
        if (dto == null || dto.getSolutionId() != null) {
            throw new ApiStatusException(400, "新增题解不能指定已有题解ID");
        }
        if (dto.getProblemId() == null || dto.getProblemId() <= 0) {
            throw new ApiStatusException(400, "请选择有效题目");
        }
    }

    private void validateNewVisibility(DetailSolutionDto dto, ContentProblemReadVo problem) {
        boolean privateValue = Boolean.TRUE.equals(dto.getPrivate_());
        if (!privateValue && !accessService.isPubliclyReadable(problem)) {
            throw new ApiStatusException(400, "该题目当前不能发布公开题解");
        }
        if (privateValue && !accessService.isPubliclyReadable(problem) && !problem.isPrivateWritable()) {
            throw new ApiStatusException(403, "没有为该非公开题目创建题解的权限");
        }
    }

    private void validateUpdateId(DetailSolutionDto dto) {
        if (dto == null || dto.getSolutionId() == null || dto.getSolutionId() <= 0) {
            throw new ApiStatusException(400, "题解ID有误");
        }
    }

    private void validateImmutableProblem(SolutionRecord current, DetailSolutionDto dto) {
        if (dto.getProblemId() == null || !Objects.equals(current.getProblemId(), dto.getProblemId())) {
            throw new ApiStatusException(400, "不能修改题解关联的题目");
        }
    }

    private void validateExistingVisibilityChange(DetailSolutionDto dto, SolutionRecord current,
                                                  ContentProblemReadVo problem) {
        boolean privateValue = dto.getPrivate_() == null
                ? Boolean.TRUE.equals(current.getPrivate_()) : dto.getPrivate_();
        if (!privateValue && !accessService.isPubliclyReadable(problem)) {
            throw new ApiStatusException(400, "该题目当前不能发布公开题解");
        }
    }
}
