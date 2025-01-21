package com.anishan.problem.service;

import com.anishan.api.client.judgeserver.domain.OjProblemCaseDto;
import com.anishan.api.client.problem.domain.vo.OjProblemCaseVo;
import com.anishan.api.domain.entity.OjProblemCase;
import com.baomidou.mybatisplus.extension.service.IService;
import lombok.SneakyThrows;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;

/**
* @author happy
* @description 针对表【oj_problem_case(OJ判题测试用例)】的数据库操作Service
* @createDate 2024-10-16 22:39:16
*/
public interface OjProblemCaseService extends IService<OjProblemCase> {

    LocalDateTime selectTime(Long id);

    List<OjProblemCaseVo> getByProblemId(Long problemId);

    List<OjProblemCaseVo> getCaseVoById(Long problemId);

    boolean addOjProblemCase(OjProblemCaseDto ojProblemCase);

    boolean removeCase(List<Long> caseId);

    void download(String path, HttpServletResponse response);
}
