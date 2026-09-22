package com.anishan.problem.service;

import com.anishan.problem.domain.vo.ProfileActivityVo;

public interface ProfileService {
    ProfileActivityVo getActivity(Long userId, boolean includePrivate);
}
