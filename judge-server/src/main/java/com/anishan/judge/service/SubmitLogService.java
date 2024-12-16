package com.anishan.judge.service;

import com.anishan.api.client.problem.domain.dto.SubmitLogDto;
import com.anishan.api.client.problem.domain.vo.SubmitLogVo;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.judge.domain.entity.SubmitLog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author happy
* @description 针对表【submit_log(OJ判题提交记录)】的数据库操作Service
* @createDate 2024-10-16 22:39:16
*/
public interface SubmitLogService extends IService<SubmitLog> {

}
