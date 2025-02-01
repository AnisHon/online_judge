package com.anishan.content.controller;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.domain.R;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.content.domain.dto.NoticeDto;
import com.anishan.content.domain.entity.Notice;
import com.anishan.content.domain.entity.NoticeContent;
import com.anishan.content.service.NoticeContentService;
import com.anishan.content.service.NoticeService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NoticeController {

    private final NoticeService noticeService;
    private final NoticeContentService noticeContentService;



    @GetMapping("/recent")
    public R<List<NoticeDto>> recent() {
        LambdaUpdateWrapper<Notice> wrapper = Wrappers.lambdaUpdate(Notice.class)
                .orderByDesc(Notice::getCreateTime);
        Page<Notice> page = new Page<>(1, 10);
        List<Notice> list = noticeService.list(page, wrapper);
        List<NoticeDto> dto = BeanUtil.copyToList(list, NoticeDto.class);
        return R.success(dto);
    }

    @GetMapping("/{id}")
    public R<NoticeDto> getNotice(@PathVariable Long id) {
        Notice notice = noticeService.getById(id);
        NoticeContent content = noticeContentService.getById(id);

        if (Objects.nonNull(notice) && Objects.nonNull(content)) {
            NoticeDto noticeDto = BeanUtil.copyProperties(notice, NoticeDto.class);
            noticeDto.setContent(content.getContent());
            return R.success(noticeDto);
        } else {
            return R.success(null);
        }
    }



    @GetMapping("/list")
    public R<PagedResult<NoticeDto>> getNoticeList(PagedQuery<Notice> query) {
        Page<Notice> page = query.page();
        LambdaUpdateWrapper<Notice> wrapper = Wrappers.lambdaUpdate(Notice.class)
                .orderByDesc(Notice::getCreateTime);

        page = noticeService.page(page, wrapper);

        PagedResult<NoticeDto> result = PagedResult.build(page, NoticeDto.class);

        return R.success(result);
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('content:notice:remove')")
    @Transactional
    public R<Boolean> deleteNotice(@PathVariable List<Long> ids) {
        return R.success(noticeService.removeBatchByIds(ids));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('content:notice:add')")
    @Transactional
    public R<Boolean> addNotice(@RequestBody NoticeDto noticeDto) {
        Notice notice = BeanUtil.copyProperties(noticeDto, Notice.class);
        NoticeContent noticeContent = new NoticeContent().setContent(noticeDto.getContent());

        Long userId = AuthUtil.getUserId();
        notice.setUserId(userId);

        boolean save = noticeService.save(notice);

        noticeContent.setNoticeId(notice.getNoticeId());

        save &= noticeContentService.save(noticeContent);
        return R.success(save);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('content:notice:edit')")
    @Transactional
    public R<Boolean> updateNotice(@RequestBody NoticeDto noticeDto) {
        Notice notice = BeanUtil.copyProperties(noticeDto, Notice.class);
        NoticeContent noticeContent = new NoticeContent()
                .setContent(noticeDto.getContent())
                .setNoticeId(notice.getNoticeId());


        noticeDto.setUserId(null);
        notice.setCreateTime(null);

        boolean save = noticeService.updateById(notice);
        save &= noticeContentService.updateById(noticeContent);
        return R.success(save);
    }
}
