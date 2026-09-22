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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

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
        LambdaQueryWrapper<Notice> wrapper = Wrappers.lambdaQuery(Notice.class)
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



    /**
     * 前台公开公告列表。这个接口不能绑定后台管理权限，否则未登录用户无法浏览公告。
     */
    @GetMapping("/list")
    public R<PagedResult<NoticeDto>> getPublicNoticeList(PagedQuery<Notice> query,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) Boolean topUp) {
        return listNotice(query, keyword, topUp);
    }

    /**
     * 后台公告管理列表。管理端与前台读取使用不同入口，避免把公开阅读权限误当成管理权限。
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('content:notice:list')")
    public R<PagedResult<NoticeDto>> getAdminNoticeList(PagedQuery<Notice> query,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) Boolean topUp) {
        return listNotice(query, keyword, topUp);
    }

    private R<PagedResult<NoticeDto>> listNotice(PagedQuery<Notice> query,
                                                 String keyword,
                                                 Boolean topUp) {
        Page<Notice> page = query.page();
        LambdaQueryWrapper<Notice> wrapper = Wrappers.lambdaQuery(Notice.class)
                .orderByDesc(Notice::getCreateTime);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Notice::getTitle, keyword.trim());
        }
        if (topUp != null) {
            wrapper.eq(Notice::getTopUp, topUp);
        }

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
