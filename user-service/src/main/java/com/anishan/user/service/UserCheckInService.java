package com.anishan.user.service;

import com.anishan.user.domain.dto.UserCheckInDto;
import com.anishan.user.domain.entity.UserCheckIn;
import com.anishan.user.domain.vo.UserCheckInInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author happy
* @description 针对表【user_check_in】的数据库操作Service
* @createDate 2024-10-26 10:27:40
*/
public interface UserCheckInService extends IService<UserCheckIn> {

    boolean isCheckedIn(Long userId);

    List<UserCheckInDto> getUserCheckInList();

    UserCheckInInfo checkIn(Long userId);

    Long todayCount();
}
