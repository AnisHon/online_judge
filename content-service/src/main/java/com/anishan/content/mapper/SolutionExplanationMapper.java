package com.anishan.content.mapper;

import com.anishan.content.domain.entity.SolutionExplanation;
import com.anishan.content.domain.vo.SolutionCandidate;
import com.anishan.content.domain.vo.SolutionRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SolutionExplanationMapper extends BaseMapper<SolutionExplanation> {
    SolutionRecord selectRecordById(@Param("solutionId") Long solutionId);
    SolutionRecord selectLikeSnapshot(@Param("solutionId") Long solutionId);
    SolutionRecord selectCommentSnapshot(@Param("solutionId") Long solutionId);
    SolutionRecord selectRecordForUpdate(@Param("solutionId") Long solutionId);
    SolutionRecord selectLikeTargetForUpdate(@Param("solutionId") Long solutionId);
    SolutionRecord selectCommentTargetForUpdate(@Param("solutionId") Long solutionId);
    Long selectLikeCount(@Param("solutionId") Long solutionId);
    int adjustLikeCount(@Param("solutionId") Long solutionId, @Param("delta") int delta);
    int adjustCommentCount(@Param("solutionId") Long solutionId, @Param("delta") int delta);
    int setCommentsOpen(@Param("solutionId") Long solutionId, @Param("open") boolean open);
    List<SolutionRecord> selectRecordsByIds(@Param("solutionIds") List<Long> solutionIds);
    List<SolutionRecord> selectProfileRecordsByIds(@Param("solutionIds") List<Long> solutionIds);

    long countCandidates(@Param("viewerId") Long viewerId, @Param("admin") boolean admin,
                         @Param("publicOnly") boolean publicOnly, @Param("filterUserId") Long filterUserId,
                         @Param("problemId") Long problemId);

    List<SolutionCandidate> selectCandidates(@Param("viewerId") Long viewerId, @Param("admin") boolean admin,
                                             @Param("publicOnly") boolean publicOnly,
                                             @Param("filterUserId") Long filterUserId,
                                             @Param("problemId") Long problemId,
                                             @Param("offset") long offset, @Param("limit") int limit,
                                             @Param("cursor") SolutionCandidate cursor);

    int updateFieldsByVersion(@Param("solutionId") Long solutionId, @Param("userId") Long userId,
                              @Param("version") Long version, @Param("title") String title,
                              @Param("privateValue") boolean privateValue, @Param("topUp") Boolean topUp,
                              @Param("firstPublishedAt") java.time.LocalDateTime firstPublishedAt);

    int moderateByVersion(@Param("solutionId") Long solutionId, @Param("version") Long version,
                          @Param("state") String state, @Param("deleted") boolean deleted,
                          @Param("firstPublishedAt") java.time.LocalDateTime firstPublishedAt,
                          @Param("now") java.time.LocalDateTime now);

    int softDeleteOwned(@Param("solutionIds") List<Long> solutionIds, @Param("userId") Long userId);

    int setTopUpByVersion(@Param("solutionId") Long solutionId, @Param("version") Long version,
                          @Param("topUp") boolean topUp);
}
