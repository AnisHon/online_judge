package com.anishan.problem.service;

import com.anishan.api.client.problem.domain.dto.ContentProblemReadRequest;
import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.api.domain.LoginUser;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.enumeration.ProblemAuth;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.entity.Problem;
import com.anishan.problem.mapper.ProblemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Supplies content-service with minimal, current problem metadata and eligibility. */
@Service
@RequiredArgsConstructor
public class ProblemContentReadService {
    private static final String PROBLEM_LIST_PERMISSION = "problem:problem:list";
    private static final Pattern DECIMAL_ID = Pattern.compile("[0-9]{1,19}");

    private final ProblemMapper problemMapper;

    public List<ContentProblemReadVo> read(ContentProblemReadRequest request) {
        if (request == null || request.hasUnsupportedProperties()) {
            throw new ApiStatusException(400, "请求参数有误");
        }
        List<Long> problemIds = parseProblemIds(request.getProblemIds());

        LoginUser caller = AuthUtil.getNonThrowUser();
        Long callerId = caller == null || caller.getUser() == null ? null : caller.getUser().getUserId();
        boolean authenticated = callerId != null && callerId > 0;
        Set<String> permissions = caller == null || caller.getAuths() == null
                ? Collections.emptySet()
                : new LinkedHashSet<>(caller.getAuths());
        boolean canReadManagedProblem = authenticated && permissions.contains(PROBLEM_LIST_PERMISSION);

        Map<Long, Problem> problemsById = problemMapper.selectContentReadByProblemIds(problemIds).stream()
                .collect(Collectors.toMap(Problem::getProblemId, problem -> problem, (first, ignored) -> first,
                        LinkedHashMap::new));

        List<ContentProblemReadVo> result = new ArrayList<>(problemIds.size());
        for (Long problemId : problemIds) {
            Problem problem = problemsById.get(problemId);
            if (problem == null) {
                result.add(new ContentProblemReadVo(String.valueOf(problemId), false, true,
                        null, false, false, null));
                continue;
            }

            boolean deleted = problem.getDelFlag() == null || problem.getDelFlag() != 0;
            ProblemAuth auth = problem.getAuth();
            boolean publicReadable = !deleted && ProblemAuth.PUBLIC.equals(auth);
            boolean privateWritable = authenticated && !deleted
                    && (ProblemAuth.PUBLIC.equals(auth)
                    || (ProblemAuth.CONTEST.equals(auth) && canReadManagedProblem));
            String title = publicReadable || canReadManagedProblem ? problem.getTitle() : null;
            Integer authValue = auth == null ? null : auth.value();

            result.add(new ContentProblemReadVo(String.valueOf(problemId), true, deleted,
                    authValue, publicReadable, privateWritable, title));
        }
        return result;
    }

    private List<Long> parseProblemIds(List<String> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty() || requestedIds.size() > 100) {
            throw new IllegalArgumentException("problemIds must contain between 1 and 100 IDs");
        }

        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (String requestedId : requestedIds) {
            if (requestedId == null || !DECIMAL_ID.matcher(requestedId).matches()) {
                throw new IllegalArgumentException("problemIds must contain positive decimal IDs");
            }
            try {
                long problemId = Long.parseLong(requestedId);
                if (problemId <= 0) {
                    throw new IllegalArgumentException("problemIds must contain positive IDs");
                }
                uniqueIds.add(problemId);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("problemId is outside the supported Long range");
            }
        }
        return new ArrayList<>(uniqueIds);
    }
}
