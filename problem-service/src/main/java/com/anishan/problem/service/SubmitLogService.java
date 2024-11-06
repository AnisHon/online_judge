package com.anishan.problem.service;

import com.anishan.api.client.problem.domain.dto.SubmitLogDto;
import com.anishan.commons.e.JudgeResult;
import com.anishan.problem.domain.entity.SubmitLog;
import com.anishan.api.client.problem.domain.vo.SubmitLogVo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author happy
* @description 针对表【submit_log(OJ判题提交记录)】的数据库操作Service
* @createDate 2024-10-16 22:39:16
*/
public interface SubmitLogService extends IService<SubmitLog> {

    Long logQueue(Long userId, Long problemId, String language);

    Long logJudge(SubmitLogDto log);

    boolean changeStatus(Long userId, Long id, JudgeResult result);

    SubmitLogVo getLog(Long id, Long userId);

    boolean update(SubmitLogDto submitLog);
}
