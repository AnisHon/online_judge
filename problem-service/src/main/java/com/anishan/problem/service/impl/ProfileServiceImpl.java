package com.anishan.problem.service.impl;

import com.anishan.problem.domain.vo.ProfileActivityVo;
import com.anishan.problem.service.ProfileActivityCacheService;
import com.anishan.problem.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    private final ProfileActivityCacheService cacheService;

    @Override
    public ProfileActivityVo getActivity(Long userId, boolean owner) {
        return ProfileActivityVo.fromBase(cacheService.getBase(userId), owner);
    }
}
