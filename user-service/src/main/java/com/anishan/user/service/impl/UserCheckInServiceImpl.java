package com.anishan.user.service.impl;

import com.anishan.api.domain.entity.SysUser;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.user.config.UserConfig;
import com.anishan.user.domain.dto.UserCheckInDto;
import com.anishan.user.domain.entity.UserCheckIn;
import com.anishan.user.domain.vo.UserCheckInInfo;
import com.anishan.user.mapper.UserCheckInMapper;
import com.anishan.user.service.SysUserService;
import com.anishan.user.service.UserCheckInService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
* @author happy
* @description 针对表【user_check_in】的数据库操作Service实现
* @createDate 2024-10-26 10:27:40
*/
@Service
@RequiredArgsConstructor
public class UserCheckInServiceImpl extends ServiceImpl<UserCheckInMapper, UserCheckIn>
    implements UserCheckInService{

    private final UserConfig config;
    private final SysUserService sysUserService;
    private final UserCheckInMapper userCheckInMapper;

    @Override
    public boolean isCheckedIn(Long userId) {
        LocalDate now = LocalDate.now();
        return this.exists(new LambdaQueryWrapper<UserCheckIn>()
                .eq(UserCheckIn::getUserId, userId)
                .eq(UserCheckIn::getSignTime, now)
        );
    }

    /**
     * 获取用户签到信息
     */
    @Override
    public List<UserCheckInDto> getUserCheckInList() {
        MPJLambdaWrapper<UserCheckIn> wrapper = new MPJLambdaWrapper<UserCheckIn>()
                .selectAll(UserCheckIn.class)
                .select(SysUser::getNikeName)
                .leftJoin(SysUser.class, SysUser::getUserId, UserCheckIn::getUserId)
                .orderByDesc(UserCheckIn::getSignTime)
                .last("limit 30");

        return userCheckInMapper.selectJoinList(UserCheckInDto.class, wrapper);
    }

    private Integer continueDays(Long userId) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return this.getObj(
                new LambdaQueryWrapper<UserCheckIn>()
                        .select(UserCheckIn::getContinuityDays)
                        .eq(UserCheckIn::getUserId, userId)
                        .eq(UserCheckIn::getSignTime, yesterday),
                x -> (Integer) x
        );
    }

    private UserCheckIn getNewCheckIn(Integer continuityDays, Long userId) {
        final Long checkInAward = config.getCheckInAward();
        final Long checkMaxAward = config.getCheckMaxAward();

        long temp = checkInAward + continuityDays;
        BigDecimal award = BigDecimal.valueOf(temp > checkMaxAward ? checkMaxAward : temp);

        LocalDate now = LocalDate.now();
        UserCheckIn userCheckIn = new UserCheckIn();
        userCheckIn.setRewardPoint(award);
        userCheckIn.setUserId(userId);
        userCheckIn.setSignTime(now);
        userCheckIn.setContinuityDays(continuityDays + 1);

        return userCheckIn;
    }

    @Override
    @Transactional
    public UserCheckInInfo checkIn(Long userId) {
        if (isCheckedIn(userId)) {
            return UserCheckInInfo.alreadyChecked();
        }


        Integer days = continueDays(userId);
        UserCheckIn newCheckIn = getNewCheckIn(days == null ? 0 : days, userId);

        BigDecimal rewardPoint = newCheckIn.getRewardPoint();

        boolean success = this.save(newCheckIn);
        success = success && sysUserService.addPoint(userId, rewardPoint);

        ThrowUtil.businessError(!success, "签到失败，发生未知错误");

        return UserCheckInInfo.successChecked(rewardPoint, newCheckIn);
    }

    @Override
    public Long todayCount() {
        LocalDate now = LocalDate.now();
        return this.count(new LambdaQueryWrapper<UserCheckIn>()
                .eq(UserCheckIn::getSignTime, now)
        );
    }


}




