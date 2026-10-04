package com.anishan.content.service;

import com.anishan.api.client.problem.client.ProblemContentReadClient;
import com.anishan.api.client.problem.domain.dto.ContentProblemReadRequest;
import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.commons.domain.R;
import com.anishan.commons.exception.ApiStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SolutionAccessService {
    private static final int MAX_BATCH_SIZE = 100;

    private final ProblemContentReadClient problemContentReadClient;

    /** Always asks problem-service for current status; the local reference is never authorization. */
    public Map<Long, ContentProblemReadVo> readFresh(List<Long> problemIds) {
        LinkedHashSet<Long> unique = new LinkedHashSet<>(problemIds);
        Map<Long, ContentProblemReadVo> result = new LinkedHashMap<>();
        List<Long> ids = new ArrayList<>(unique);
        for (int start = 0; start < ids.size(); start += MAX_BATCH_SIZE) {
            List<Long> batch = ids.subList(start, Math.min(start + MAX_BATCH_SIZE, ids.size()));
            List<String> requested = new ArrayList<>(batch.size());
            for (Long id : batch) {
                if (id == null || id <= 0) {
                    throw new ApiStatusException(400, "题目参数有误");
                }
                requested.add(String.valueOf(id));
            }
            final R<List<ContentProblemReadVo>> response;
            try {
                ContentProblemReadRequest request = new ContentProblemReadRequest();
                request.setProblemIds(requested);
                response = problemContentReadClient.readProblemContent(request);
            } catch (RuntimeException exception) {
                throw new ApiStatusException(503, "题目状态暂时无法确认，请稍后重试");
            }
            if (response == null || response.getCode() != 200 || response.getData() == null) {
                throw new ApiStatusException(503, "题目状态暂时无法确认，请稍后重试");
            }
            for (ContentProblemReadVo item : response.getData()) {
                if (item == null || item.getProblemId() == null) {
                    throw new ApiStatusException(503, "题目状态暂时无法确认，请稍后重试");
                }
                try {
                    Long id = Long.valueOf(item.getProblemId());
                    if (!unique.contains(id) || result.put(id, item) != null) {
                        throw new ApiStatusException(503, "题目状态暂时无法确认，请稍后重试");
                    }
                } catch (NumberFormatException exception) {
                    throw new ApiStatusException(503, "题目状态暂时无法确认，请稍后重试");
                }
            }
        }
        if (result.size() != unique.size()) {
            throw new ApiStatusException(503, "题目状态暂时无法确认，请稍后重试");
        }
        return result;
    }

    public boolean isPubliclyReadable(ContentProblemReadVo problem) {
        return problem != null && problem.isExists() && !problem.isDeleted()
                && Integer.valueOf(1).equals(problem.getAuth()) && problem.isPublicReadable();
    }
}
