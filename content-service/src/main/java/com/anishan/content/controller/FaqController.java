package com.anishan.content.controller;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.commons.domain.R;
import com.anishan.content.domain.dto.FaqDto;
import com.anishan.content.domain.entity.Faq;
import com.anishan.content.service.FaqService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/faq")
@Validated
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FaqController {

    private final FaqService faqService;

    /** Public read endpoint; FAQ visibility is not tied to backend management permissions. */
    @GetMapping("/list")
    public R<List<FaqDto>> listPublicFaqs() {
        return R.success(loadFaqDtos());
    }

    /** Separate management endpoint, guarded independently from public reading. */
    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('content:faq:list')")
    public R<List<FaqDto>> listAdminFaqs() {
        return R.success(loadFaqDtos());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('content:faq:add')")
    public R<Boolean> addFaq(@Valid @RequestBody FaqDto dto) {
        Faq faq = new Faq()
                .setQuestion(dto.getQuestion().trim())
                .setAnswer(dto.getAnswer());
        return R.success(faqService.save(faq));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('content:faq:edit')")
    public R<Boolean> updateFaq(@Valid @RequestBody FaqDto dto) {
        if (dto.getFaqId() == null) {
            return R.badRequest("FAQ编号不能为空");
        }
        Faq faq = new Faq()
                .setFaqId(dto.getFaqId())
                .setQuestion(dto.getQuestion().trim())
                .setAnswer(dto.getAnswer());
        return R.success(faqService.updateById(faq));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('content:faq:remove')")
    public R<Boolean> removeFaqs(@PathVariable List<Long> ids) {
        return R.success(ids != null && !ids.isEmpty() && faqService.removeByIds(ids));
    }

    private List<FaqDto> loadFaqDtos() {
        List<Faq> faqs = faqService.list(Wrappers.lambdaQuery(Faq.class).orderByAsc(Faq::getFaqId));
        return BeanUtil.copyToList(faqs, FaqDto.class);
    }
}
