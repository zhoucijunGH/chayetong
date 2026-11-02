package com.chayetong.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chayetong.common.BizException;
import com.chayetong.controller.dto.CategoryRequest;
import com.chayetong.entity.Category;
import com.chayetong.entity.CategoryGrade;
import com.chayetong.mapper.CategoryGradeMapper;
import com.chayetong.mapper.CategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 品类档案服务：查询 / 维护（维护仅管理员）。
 */
@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final CategoryGradeMapper gradeMapper;

    public CategoryService(CategoryMapper categoryMapper, CategoryGradeMapper gradeMapper) {
        this.categoryMapper = categoryMapper;
        this.gradeMapper = gradeMapper;
    }

    /** 品类列表：名称模糊 + 启停过滤 */
    public List<Category> list(String keyword, Integer enabled) {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(enabled != null, Category::getEnabled, enabled)
                .like(StringUtils.hasText(keyword), Category::getName, keyword)
                .orderByDesc(Category::getId));
    }

    @Transactional
    public Category create(CategoryRequest req) {
        Long exists = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getName, req.getName().trim()));
        if (exists != null && exists > 0) {
            throw new BizException(400, "品类已存在");
        }
        Category c = new Category();
        apply(c, req);
        c.setEnabled(req.getEnabled() == null ? 1 : req.getEnabled());
        categoryMapper.insert(c);
        return c;
    }

    @Transactional
    public Category update(Long id, CategoryRequest req) {
        Category c = get(id);
        Long exists = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getName, req.getName().trim())
                .ne(Category::getId, id));
        if (exists != null && exists > 0) {
            throw new BizException(400, "品类已存在");
        }
        apply(c, req);
        if (req.getEnabled() != null) {
            c.setEnabled(req.getEnabled());
        }
        categoryMapper.updateById(c);
        return c;
    }

    /** 删除品类（连带删除其等级价格；历史单据存快照不受影响） */
    @Transactional
    public void delete(Long id) {
        get(id);
        gradeMapper.delete(new LambdaQueryWrapper<CategoryGrade>().eq(CategoryGrade::getCategoryId, id));
        categoryMapper.deleteById(id);
    }

    public Category get(Long id) {
        Category c = categoryMapper.selectById(id);
        if (c == null) {
            throw new BizException(404, "品类不存在");
        }
        return c;
    }

    private void apply(Category c, CategoryRequest req) {
        c.setName(req.getName().trim());
        c.setRemark(req.getRemark());
    }
}
