package com.anishan.problem.service.impl;

import com.anishan.api.client.user.client.UserInternalClient;
import com.anishan.api.client.user.domain.dto.UserSummaryRequest;
import com.anishan.api.client.user.domain.vo.UserSummaryVo;
import com.anishan.commons.domain.R;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.enumeration.ContestAuth;
import com.anishan.commons.enumeration.ContestType;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.dto.FinalRankPageQuery;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.ContestRankEntry;
import com.anishan.problem.domain.entity.ContestRankSnapshot;
import com.anishan.problem.domain.enumeration.ContestRankState;
import com.anishan.problem.domain.vo.FinalRankVo;
import com.anishan.problem.domain.vo.RankEntryVo;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ContestRankEntryMapper;
import com.anishan.problem.mapper.ContestRankSnapshotMapper;
import com.anishan.problem.mapper.UserContestMapper;
import com.anishan.problem.service.ContestFinalRankService;
import com.anishan.problem.service.ContestRankPageCache;
import com.anishan.problem.service.ContestRankQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ContestRankQueryServiceImpl implements ContestRankQueryService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ContestMapper contestMapper;
    private final UserContestMapper userContestMapper;
    private final ContestRankSnapshotMapper snapshotMapper;
    private final ContestRankEntryMapper entryMapper;
    private final ContestFinalRankService finalRankService;
    private final ContestRankPageCache pageCache;
    private final UserInternalClient userInternalClient;
    private final Clock clock;

    public ContestRankQueryServiceImpl(ContestMapper contestMapper,
                                       UserContestMapper userContestMapper,
                                       ContestRankSnapshotMapper snapshotMapper,
                                       ContestRankEntryMapper entryMapper,
                                       ContestFinalRankService finalRankService,
                                       ContestRankPageCache pageCache,
                                       UserInternalClient userInternalClient,
                                       @Qualifier("contestSubmissionClock") Clock clock) {
        this.contestMapper = contestMapper;
        this.userContestMapper = userContestMapper;
        this.snapshotMapper = snapshotMapper;
        this.entryMapper = entryMapper;
        this.finalRankService = finalRankService;
        this.pageCache = pageCache;
        this.userInternalClient = userInternalClient;
        this.clock = clock;
    }

    @Override
    public FinalRankVo getPublicRank(Long contestId, Long viewerId, FinalRankPageQuery query) {
        requirePositive(contestId, "比赛编号无效");
        if (viewerId == null || viewerId <= 0) {
            throw new ApiStatusException(401, "请先登录");
        }
        validatePage(query);
        Contest contest = loadContest(contestId);
        if (contest.getAuth() != ContestAuth.PUBLIC && !isJoined(contestId, viewerId)) {
            // Do not reveal whether a private/white-list contest exists to a non-member.
            throw new ApiStatusException(404, "比赛不存在或无权访问");
        }
        requireEnded(contest);
        return buildResponse(contest, query, false);
    }

    @Override
    public FinalRankVo getAdminRank(Long contestId, FinalRankPageQuery query) {
        requirePositive(contestId, "比赛编号无效");
        validatePage(query);
        Contest contest = loadContest(contestId);
        requireEnded(contest);
        return buildResponse(contest, query, true);
    }

    @Override
    public boolean requestRebuild(Long contestId) {
        requirePositive(contestId, "比赛编号无效");
        return finalRankService.requestRebuild(contestId);
    }

    private Contest loadContest(Long contestId) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null || !Integer.valueOf(0).equals(contest.getDelFlag())) {
            throw new ApiStatusException(404, "比赛不存在");
        }
        if (contest.getType() != ContestType.CONTEST) {
            throw new ApiStatusException(400, "作业不提供最终比赛榜单");
        }
        return contest;
    }

    private void requireEnded(Contest contest) {
        if (contest.getEndTime() == null || contest.getEndTime().isAfter(LocalDateTime.now(clock))) {
            throw new ApiStatusException(409, "比赛尚未结束");
        }
    }

    private boolean isJoined(Long contestId, Long userId) {
        return userContestMapper.selectCount(new LambdaQueryWrapper<com.anishan.problem.domain.entity.UserContestRelation>()
                .eq(com.anishan.problem.domain.entity.UserContestRelation::getContestId, contestId)
                .eq(com.anishan.problem.domain.entity.UserContestRelation::getUserId, userId)) > 0;
    }

    private FinalRankVo buildResponse(Contest contest, FinalRankPageQuery query, boolean admin) {
        Long contestId = contest.getContestId();
        ContestRankSnapshot header = snapshotMapper.selectByContestId(contestId);
        if (header == null) {
            // Older valid contests may have missed initialization; this only registers a due header.
            finalRankService.ensureScheduled(contestId);
            header = snapshotMapper.selectByContestId(contestId);
        }
        if (header == null || header.getState() == null || header.getVersion() == null
                || header.getVersion() < 0) {
            throw new ApiStatusException(503, "最终榜单暂不可用，请稍后重试");
        }

        FinalRankVo response = new FinalRankVo();
        response.setContestId(contestId);
        response.setState(header.getState().name());
        response.setVersion(header.getVersion());
        response.setRuleVersion(publicRuleVersion(header));
        response.setSourceMode(publicSourceMode(header));
        response.setGeneratedAt(header.getGeneratedAt());
        response.setPendingCount(valueOrZero(header.getPendingCount()));
        response.setMessage(statusMessage(header));
        if (admin) {
            response.setAdminBuildInfo(adminBuildInfo(header));
        }

        if (header.getVersion() > 0) {
            response.setRanking(readPublishedPage(contestId, header, query));
        }
        return response;
    }

    private PagedResult<RankEntryVo> readPublishedPage(Long contestId,
                                                        ContestRankSnapshot header,
                                                        FinalRankPageQuery query) {
        long currentPage = query.getCurrentPage();
        int pageSize = Math.toIntExact(query.getPageSize());
        long pageIndex = currentPage - 1;
        if (pageIndex > Long.MAX_VALUE / pageSize) {
            throw new ApiStatusException(400, "分页参数超出范围");
        }
        long offset = pageIndex * pageSize;
        long version = header.getVersion();
        long totalRecords = valueOrZero(header.getTotalUsers());
        String key = pageCache.key(contestId, version, currentPage, pageSize);
        ContestRankPageCache.PageSnapshot page = pageCache.get(
                key, contestId, version, currentPage, pageSize, totalRecords);
        if (page == null) {
            List<ContestRankEntry> rows = entryMapper.selectPageByVersion(contestId, version, offset, pageSize);
            page = pageCache.snapshot(contestId, version, currentPage, pageSize, totalRecords, rows);
            pageCache.put(key, page);
        }

        Map<Long, UserSummaryVo> summaries = loadUserSummaries(page.getEntries());
        List<RankEntryVo> entries = new ArrayList<>(page.getEntries().size());
        for (ContestRankPageCache.CachedRankEntry entry : page.getEntries()) {
            RankEntryVo item = new RankEntryVo();
            item.setRank(entry.getRank());
            item.setRowPosition(entry.getRowPosition());
            item.setUserId(entry.getUserId());
            item.setScore(new BigDecimal(entry.getScore()).toPlainString());
            item.setCorrectCount(entry.getCorrectCount());
            item.setAnsweredCount(entry.getAnsweredCount());
            item.setHandedIn(entry.getHandedIn());
            item.setUser(summaries.get(entry.getUserId()));
            entries.add(item);
        }

        PagedResult<RankEntryVo> result = new PagedResult<>();
        result.setCurrentPage(currentPage);
        result.setPageSize((long) pageSize);
        result.setTotalRecords(page.getTotalRecords());
        result.setData(entries);
        return result;
    }

    private Map<Long, UserSummaryVo> loadUserSummaries(List<ContestRankPageCache.CachedRankEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> ids = entries.stream().map(ContestRankPageCache.CachedRankEntry::getUserId)
                .distinct().map(String::valueOf).collect(Collectors.toList());
        UserSummaryRequest request = new UserSummaryRequest();
        request.setUserIds(ids);
        final R<List<UserSummaryVo>> response;
        try {
            response = userInternalClient.userSummaries(request);
        } catch (RuntimeException unavailable) {
            log.warn("Final-rank user summary lookup unavailable errorType={}",
                    unavailable.getClass().getSimpleName());
            throw new ApiStatusException(503, "用户信息暂不可用，请稍后重试");
        }
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            throw new ApiStatusException(503, "用户信息暂不可用，请稍后重试");
        }
        Map<Long, UserSummaryVo> summaries = new LinkedHashMap<>();
        for (UserSummaryVo summary : response.getData()) {
            if (summary != null && summary.getUserId() != null && ids.contains(String.valueOf(summary.getUserId()))) {
                summaries.put(summary.getUserId(), summary);
            }
        }
        return summaries;
    }

    private FinalRankVo.AdminBuildInfo adminBuildInfo(ContestRankSnapshot header) {
        FinalRankVo.AdminBuildInfo info = new FinalRankVo.AdminBuildInfo();
        info.setBuildAttempts(header.getBuildAttempts());
        info.setNextBuildAt(header.getNextBuildAt());
        info.setLeaseUntil(header.getLeaseUntil());
        info.setLastError(header.getLastError());
        return info;
    }

    private String statusMessage(ContestRankSnapshot header) {
        long version = valueOrZero(header.getVersion());
        long pending = valueOrZero(header.getPendingCount());
        switch (header.getState()) {
            case WAITING:
                if (pending > 0) return "判题结果尚未全部完成，榜单仍在结算";
                return version > 0 ? "新版榜单待更新，当前展示最近已发布结果" : "最终榜单正在准备中";
            case BUILDING:
                return version > 0 ? "榜单更新中，当前展示最近已发布结果" : "最终榜单正在生成，请稍后刷新";
            case ERROR:
                return version > 0 ? "新版榜单生成失败，当前展示最近已发布结果" : "最终榜单暂不可用，请稍后重试";
            case READY:
            default:
                return null;
        }
    }

    private String publicSourceMode(ContestRankSnapshot header) {
        if ("CURRENT".equals(header.getSourceMode()) || "LEGACY".equals(header.getSourceMode())) {
            return header.getSourceMode();
        }
        return null;
    }

    private String publicRuleVersion(ContestRankSnapshot header) {
        if ("WEIGHTED_LATEST_V1".equals(header.getRuleVersion())
                || "LEGACY_RECORD_V1".equals(header.getRuleVersion())) {
            return header.getRuleVersion();
        }
        return null;
    }

    private void validatePage(FinalRankPageQuery query) {
        if (query == null || query.getCurrentPage() == null || query.getCurrentPage() < 1
                || query.getPageSize() == null || query.getPageSize() < 1 || query.getPageSize() > MAX_PAGE_SIZE) {
            throw new ApiStatusException(400, "分页参数无效，每页最多50条");
        }
        if (query.getCurrentPage() - 1 > Long.MAX_VALUE / query.getPageSize()) {
            throw new ApiStatusException(400, "分页参数超出范围");
        }
    }

    private void requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new ApiStatusException(400, message);
        }
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
