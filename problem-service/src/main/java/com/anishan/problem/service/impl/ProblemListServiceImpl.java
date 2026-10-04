package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.anishan.api.config.ConstConfig;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.enumeration.ProblemAuth;
import com.anishan.commons.exception.BusinessException;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.dto.PagedProblemList;
import com.anishan.problem.domain.dto.ProblemListDto;
import com.anishan.problem.domain.dto.ProblemListOrderBatchDto;
import com.anishan.problem.domain.dto.ProblemListOrderItemDto;
import com.anishan.problem.domain.dto.ProblemListRelationDto;
import com.anishan.problem.domain.entity.Problem;
import com.anishan.problem.domain.entity.ProblemList;
import com.anishan.problem.domain.entity.ProblemProblemListRelation;
import com.anishan.problem.domain.vo.ProblemInListVo;
import com.anishan.problem.domain.vo.ProblemListVo;
import com.anishan.problem.domain.vo.ProblemVo;
import com.anishan.problem.mapper.ProblemListMapper;
import com.anishan.problem.mapper.ProblemProblemListMapper;
import com.anishan.problem.service.ProblemListService;
import com.anishan.problem.service.ProblemProblemListService;
import com.anishan.problem.service.ContestProblemSnapshotService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
* @author happy
* @description 针对表【problem_list(题单表)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProblemListServiceImpl extends ServiceImpl<ProblemListMapper, ProblemList>
    implements ProblemListService{

    private final ProblemProblemListService problemProblemListService;
    private final ProblemListMapper problemListMapper;
    private final ProblemProblemListMapper problemProblemListMapper;
    private final ContestProblemSnapshotService snapshotService;
    private final ConstConfig constConfig;

    @Override
    public List<ProblemVo> getProblemListByListId(Long id) {
        MPJLambdaWrapper<ProblemList> wrapper = new MPJLambdaWrapper<ProblemList>()
                .selectAll(ProblemVo.class)
                .leftJoin(ProblemProblemListRelation.class, ProblemProblemListRelation::getListId, ProblemList::getListId)
                .leftJoin(Problem.class, Problem::getProblemId, ProblemProblemListRelation::getProblemId)
                .eq(Problem::getAuth, ProblemAuth.PUBLIC)
                .eq(ProblemList::getListId, id);

        return problemListMapper.selectJoinList(ProblemVo.class, wrapper);
    }

    @Override
    @Transactional
    public boolean addProblemList(ProblemListDto pl) {
        ProblemList problemList = BeanUtil.copyProperties(pl, ProblemList.class);
        return this.save(problemList);
    }

    @Override
    @Transactional
    public void delProblemList(List<Long> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return;
        }
        lockLists(ids);
        if (problemListMapper.countContestReferencesByListIds(ids) > 0) {
            throw new ApiStatusException(409, "题单仍被比赛或作业引用，不能删除；请先调整活动题单");
        }
        this.remove(new LambdaQueryWrapper<ProblemList>()
                .in(ProblemList::getListId, ids)
        );
        // Do not use delByListIds here: its legacy implementation uses an empty wrapper.
        // Keep this deletion scoped while the corresponding list rows are locked.
        problemProblemListService.remove(new LambdaQueryWrapper<ProblemProblemListRelation>()
                .in(ProblemProblemListRelation::getListId, ids));
    }

    @Override
    public LocalDateTime getDataTime(Long id) {
        return this.getObj(
                new LambdaQueryWrapper<ProblemList>()
                        .select(ProblemList::getUpdateTime)
                        .eq(ProblemList::getListId, id),
                x -> (LocalDateTime) x
        );
    }

    @Override
    @Transactional
    public boolean updateProblemList(ProblemListDto problemListDto) {
        LocalDateTime dataTime = lockList(problemListDto.getListId());
        if (dataTime == null) {
            throw new BusinessException("题单不存在或已删除");
        }
        ProblemList problemList = BeanUtil.copyProperties(problemListDto, ProblemList.class);
        problemList.setUpdateTime(dataTime);

        return this.updateById(problemList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addProblemList(List<ProblemListRelationDto> relations) {
        if (CollectionUtil.isEmpty(relations)) {
            return true;
        }
        Map<Long, LocalDateTime> listVersions = lockLists(relations.stream()
                .map(ProblemListRelationDto::getListId).collect(Collectors.toList()));
        List<ProblemProblemListRelation> list = BeanUtil.copyToList(relations, ProblemProblemListRelation.class);
        BigDecimal score = constConfig.getListDefaultScore();
        list.forEach(x -> x.setScore(score));
        if (!problemProblemListService.saveBatch(list)) {
            throw new BusinessException("题单添加题目失败");
        }
        touchLists(listVersions);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delProblem(List<ProblemListRelationDto> relations) {
        if (CollectionUtil.isEmpty(relations)) {
            return true;
        }
        Map<Long, LocalDateTime> listVersions = lockLists(relations.stream()
                .map(ProblemListRelationDto::getListId).collect(Collectors.toList()));
        int deleted = problemProblemListMapper.deleteBatch(relations);
        touchLists(listVersions);
        return deleted > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProblemRelation(ProblemProblemListRelation relation) {
        if (relation == null || relation.getListId() == null || relation.getProblemId() == null) {
            throw new BusinessException("题单题目参数无效");
        }
        if (relation.getScore() == null && relation.getProblemOrder() == null) {
            return false;
        }
        LocalDateTime expectedUpdateTime = lockList(relation.getListId());
        if (expectedUpdateTime == null) {
            throw new BusinessException("题单不存在或已删除");
        }
        boolean updated = problemProblemListService.update(
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProblemProblemListRelation>()
                        .set(relation.getScore() != null,
                                ProblemProblemListRelation::getScore, relation.getScore())
                        .set(relation.getProblemOrder() != null,
                                ProblemProblemListRelation::getProblemOrder, relation.getProblemOrder())
                        .eq(ProblemProblemListRelation::getProblemId, relation.getProblemId())
                        .eq(ProblemProblemListRelation::getListId, relation.getListId()));
        if (updated && problemListMapper.touchUpdateTime(relation.getListId(), expectedUpdateTime) != 1) {
            throw new BusinessException("题单已被其他管理员修改，请刷新后重试");
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProblemOrder(ProblemListOrderBatchDto request) {
        if (request.getExpectedUpdateTime() == null) {
            throw new BusinessException("题单版本已失效，请刷新后重试");
        }

        // 锁住题单版本，再校验完整的题目集合，避免两个管理员互相覆盖排序。
        LocalDateTime currentUpdateTime = problemListMapper.selectUpdateTimeForUpdate(request.getListId());
        if (currentUpdateTime == null || !currentUpdateTime.equals(request.getExpectedUpdateTime())) {
            throw new BusinessException("题单已被其他管理员修改，请刷新后重试");
        }

        List<ProblemProblemListRelation> currentRelations = problemProblemListService.list(
                new LambdaQueryWrapper<ProblemProblemListRelation>()
                        .select(ProblemProblemListRelation::getProblemId)
                        .eq(ProblemProblemListRelation::getListId, request.getListId())
        );

        Set<Long> currentProblemIds = currentRelations.stream()
                .map(ProblemProblemListRelation::getProblemId)
                .collect(Collectors.toSet());
        Set<Long> requestedProblemIds = request.getItems().stream()
                .map(ProblemListOrderItemDto::getProblemId)
                .collect(Collectors.toSet());
        Set<Integer> requestedOrders = request.getItems().stream()
                .map(ProblemListOrderItemDto::getProblemOrder)
                .collect(Collectors.toSet());
        Set<Integer> expectedOrders = IntStream.rangeClosed(1, currentProblemIds.size())
                .boxed()
                .collect(Collectors.toSet());

        if (currentProblemIds.isEmpty()
                || currentProblemIds.size() != request.getItems().size()
                || currentProblemIds.size() != requestedProblemIds.size()
                || !currentProblemIds.equals(requestedProblemIds)
                || !expectedOrders.equals(requestedOrders)) {
            throw new IllegalArgumentException("题单题目顺序已发生变化，请刷新后重试");
        }

        // 不依赖 MySQL 是否开启 useAffectedRows：部分顺序本来就相同的时候，更新行数可能小于题目总数。
        // 即使排序结果与数据库部分相同，也要推进题单版本；不能依赖 MySQL 的 affected rows。
        problemProblemListMapper.updateProblemOrderBatch(request.getListId(), request.getItems());
        if (problemListMapper.touchUpdateTime(request.getListId(), request.getExpectedUpdateTime()) != 1) {
            throw new BusinessException("题单已被其他管理员修改，请刷新后重试");
        }
        return true;
    }

    private Map<Long, LocalDateTime> lockLists(List<Long> listIds) {
        TreeSet<Long> orderedIds = new TreeSet<>();
        for (Long listId : listIds) {
            if (listId == null || listId <= 0) {
                throw new BusinessException("题单ID无效");
            }
            orderedIds.add(listId);
        }
        Map<Long, LocalDateTime> versions = new LinkedHashMap<>();
        for (Long listId : orderedIds) {
            LocalDateTime updateTime = lockList(listId);
            if (updateTime == null) {
                throw new BusinessException("题单不存在或已删除");
            }
            versions.put(listId, updateTime);
        }
        return versions;
    }

    private LocalDateTime lockList(Long listId) {
        if (listId == null || listId <= 0) {
            return null;
        }
        return problemListMapper.selectUpdateTimeForUpdate(listId);
    }

    private void touchLists(Map<Long, LocalDateTime> listVersions) {
        for (Map.Entry<Long, LocalDateTime> entry : listVersions.entrySet()) {
            if (problemListMapper.touchUpdateTime(entry.getKey(), entry.getValue()) != 1) {
                throw new BusinessException("题单已被其他管理员修改，请刷新后重试");
            }
        }
    }

    @Override
    public List<ProblemInListVo> getProblems(Long id) {
        return problemListMapper.selectAdminProblems(id);
    }

    @Override
    public PagedResult<ProblemListVo> listPage(PagedProblemList query) {
        Page<ProblemList> page = query.page();
        Wrapper<ProblemList> wrapper = query.wrapper();

        page = this.page(page, wrapper);

        return PagedResult.build(page, ProblemListVo.class);
    }

    @Override
    public List<ProblemInListVo> getContestProblemsByListId(Long listId, Long contestId) {
        Long userId = AuthUtil.getUserId();
        snapshotService.createAfterStart(contestId);
        return problemListMapper.selectContestSnapshotProblems(contestId, userId);
    }

    @Override
    public List<ProblemInListVo> getProblemByListId(Long id) {
        Long userId = AuthUtil.getUserId();
        return problemListMapper.selectProblemByListId(userId, id);
    }
}
