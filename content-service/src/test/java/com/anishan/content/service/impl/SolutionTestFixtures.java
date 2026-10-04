package com.anishan.content.service.impl;

import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.content.domain.dto.DetailSolutionDto;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.*;
import java.util.List;

final class SolutionTestFixtures {
    static final long ID = 2098755579163770881L;
    static final long AUTHOR = 42L;
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"), ZoneId.of("Asia/Shanghai"));
    static SolutionRecord row() {
        SolutionRecord row = new SolutionRecord();
        row.setSolutionId(ID); row.setUserId(AUTHOR); row.setProblemId(77L);
        row.setPrivate_(false); row.setDelFlag(false); row.setModerationState(SolutionModerationState.NORMAL);
        row.setVersion(3L); row.setTitle("title"); row.setContent("body");
        row.setLikeCount(2L); row.setCommentCount(1L); row.setCommentsOpen(true);
        row.setCreateTime(LocalDateTime.now(CLOCK));
        return row;
    }
    static ContentProblemReadVo problem(boolean publicValue) {
        ContentProblemReadVo problem = new ContentProblemReadVo("77", true, false, publicValue ? 1 : 2, publicValue, false, null);
        problem.setProblemId("77"); problem.setExists(true); problem.setDeleted(false);
        problem.setAuth(publicValue ? 1 : 2); problem.setPublicReadable(publicValue);
        problem.setTitle(publicValue ? "公开题" : null);
        return problem;
    }
    static DetailSolutionDto dto(boolean privateValue) {
        return new DetailSolutionDto().setTitle("edited").setContent("body").setProblemId(77L).setPrivate_(privateValue);
    }
    static void login(Long id, String... authorities) {
        LoginUser login = new LoginUser();
        login.setUser(new SysUser().setUserId(id)); login.setAuths(List.of(authorities));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login, null, login.getAuthorities()));
    }
    private SolutionTestFixtures() {}
}
