package com.anishan.content.service;

import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.content.domain.dto.CommentCreateRequest;
import com.anishan.content.domain.dto.CommentPageQuery;
import com.anishan.content.domain.dto.CommentReplyRequest;
import com.anishan.content.domain.vo.CommentVo;

public interface CommentService {
    PagedResult<CommentVo> pageRoots(Long solutionId, CommentPageQuery query);

    PagedResult<CommentVo> pageReplies(Long rootId, CommentPageQuery query);

    CommentVo createRoot(Long solutionId, CommentCreateRequest request);

    CommentVo createReply(Long parentId, CommentReplyRequest request);
}
