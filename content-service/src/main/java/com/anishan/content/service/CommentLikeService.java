package com.anishan.content.service;

import com.anishan.content.domain.vo.LikeResultVo;

public interface CommentLikeService {
    LikeResultVo like(Long userId, Long commentId);

    LikeResultVo unlike(Long userId, Long commentId);
}
