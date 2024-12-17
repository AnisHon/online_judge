package com.anishan.judge.service.impl;

import com.anishan.judge.domain.entity.SubmitLog;
import com.anishan.judge.mapper.SubmitLogMapper;
import com.anishan.judge.service.SubmitLogService;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
* @author happy
* @description 针对表【submit_log(OJ判题提交记录)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SubmitLogServiceImpl extends ServiceImpl<SubmitLogMapper, SubmitLog>
    implements SubmitLogService {


}




