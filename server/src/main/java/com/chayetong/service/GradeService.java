package com.chayetong.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chayetong.common.BizException;
import com.chayetong.controller.dto.GradeRequest;
import com.chayetong.entity.Category;
import com.chayetong.entity.CategoryGrade;
import com.chayetong.mapper.CategoryGradeMapper;
import com.chayetong.mapper.CategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 品类等级价格服务：档案维护、状态自动流转、开单取生效价。
 * 同品类同等级的生效区间不允许重叠。
 */
@Service
public class GradeService {

    private final CategoryGradeMapper gradeMapper;
    private final CategoryMapper categoryMapper;

    public GradeService(CategoryGradeMapper gradeMapper, CategoryMapper categoryMapper) {
        this.gradeMapper = gradeMapper;
        this.categoryMapper = categoryMapper;
    }

    /** 价格列表：按品类过滤 + 状态填充 */
    public List<CategoryGrade> list(Long categoryId, String keyword) {
        LocalDate today = LocalDate.now();
        List<CategoryGrade> list = gradeMapper.selectList(new LambdaQueryWrapper<CategoryGrade>()
                .eq(categoryId != null, CategoryGrade::getCategoryId, categoryId)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(CategoryGrade::getCategoryName, keyword)
                        .or().like(CategoryGrade::getGrade, keyword))
                .orderByDesc(CategoryGrade::getEffectiveDate)
                .orderByDesc(CategoryGrade::getId));
        list.forEach(g -> g.setStatus(g.computeStatus(today)));
        return list;
    }

    /** 指定品类在某日期的生效价（无则返回 null），用于开单带价 */
    public CategoryGrade effective(Long categoryId, LocalDate date) {
        if (categoryId == null || date == null) {
            return null;
        }
        List<CategoryGrade> candidates = gradeMapper.selectList(new LambdaQueryWrapper<CategoryGrade>()
                .eq(CategoryGrade::getCategoryId, categoryId)
                .le(CategoryGrade::getEffectiveDate, date)
                .and(w -> w.isNull(CategoryGrade::getExpiryDate)
                        .or().ge(CategoryGrade::getExpiryDate, date)));
        if (candidates.isEmpty()) {
            return null;
        }
        // 取生效日期最近（最精确）的一条
        candidates.sort((a, b) -> b.getEffectiveDate().compareTo(a.getEffectiveDate()));
        CategoryGrade hit = candidates.get(0);
        hit.setStatus(hit.computeStatus(date));
        return hit;
    }

    public CategoryGrade create(GradeRequest req) {
        Category category = categoryMapper.selectById(req.getCategoryId());
        if (category == null) {
            throw new BizException(404, "品类不存在");
        }
        validate(req, null);
        CategoryGrade g = new CategoryGrade();
        g.setCategoryId(category.getId());
        g.setCategoryName(category.getName());
        apply(g, req);
        gradeMapper.insert(g);
        g.setStatus(g.computeStatus(LocalDate.now()));
        return g;
    }

    public CategoryGrade update(Long id, GradeRequest req) {
        CategoryGrade g = gradeMapper.selectById(id);
        if (g == null) {
            throw new BizException(404, "价格记录不存在");
        }
        Category category = categoryMapper.selectById(req.getCategoryId());
        if (category == null) {
            throw new BizException(404, "品类不存在");
        }
        validate(req, id);
        g.setCategoryId(category.getId());
        g.setCategoryName(category.getName());
        apply(g, req);
        gradeMapper.updateById(g);
        g.setStatus(g.computeStatus(LocalDate.now()));
        return g;
    }

    public void delete(Long id) {
        if (gradeMapper.selectById(id) == null) {
            throw new BizException(404, "价格记录不存在");
        }
        gradeMapper.deleteById(id);
    }

    /** 按 id 取价格记录（附状态） */
    public CategoryGrade effectiveById(Long gradeId) {
        if (gradeId == null) {
            return null;
        }
        CategoryGrade g = gradeMapper.selectById(gradeId);
        if (g != null) {
            g.setStatus(g.computeStatus(LocalDate.now()));
        }
        return g;
    }

    /** 同品类等级生效区间重叠校验 */
    private void validate(GradeRequest req, Long selfId) {
        if (req.getExpiryDate() != null && req.getExpiryDate().isBefore(req.getEffectiveDate())) {
            throw new BizException(400, "失效日期不能早于生效日期");
        }
        LocalDate reqEnd = req.getExpiryDate() == null ? LocalDate.of(9999, 12, 31) : req.getExpiryDate();
        List<CategoryGrade> others = gradeMapper.selectList(new LambdaQueryWrapper<CategoryGrade>()
                .eq(CategoryGrade::getCategoryId, req.getCategoryId())
                .eq(CategoryGrade::getGrade, req.getGrade().trim())
                .ne(selfId != null, CategoryGrade::getId, selfId));
        for (CategoryGrade o : others) {
            LocalDate oEnd = o.getExpiryDate() == null ? LocalDate.of(9999, 12, 31) : o.getExpiryDate();
            boolean overlap = !req.getEffectiveDate().isAfter(oEnd) && !reqEnd.isBefore(o.getEffectiveDate());
            if (overlap) {
                throw new BizException(400, "同品类等级的生效期重叠：" + o.getCategoryName()
                        + " / " + o.getGrade() + "（" + o.getEffectiveDate() + " ~ "
                        + (o.getExpiryDate() == null ? "长期" : o.getExpiryDate()) + "）");
            }
        }
    }

    private void apply(CategoryGrade g, GradeRequest req) {
        g.setGrade(req.getGrade().trim());
        g.setPrice(req.getPrice());
        g.setEffectiveDate(req.getEffectiveDate());
        g.setExpiryDate(req.getExpiryDate());
    }
}
