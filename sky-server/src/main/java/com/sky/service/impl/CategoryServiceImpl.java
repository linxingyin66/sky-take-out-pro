package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分类业务层
 */
@Service
@Slf4j
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper,Category> implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 新增分类
     * @param categoryDTO
     */
    public void saveCategory(CategoryDTO categoryDTO) {
        Category category = new Category();
        //属性拷贝
        BeanUtils.copyProperties(categoryDTO, category);

        //分类状态默认为禁用状态0
        category.setStatus(StatusConstant.DISABLE);

        categoryMapper.insert(category);
    }

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {

        int page = categoryPageQueryDTO.getPage();
        int pageSize = categoryPageQueryDTO.getPageSize();
        String name = categoryPageQueryDTO.getName();
        Integer type = categoryPageQueryDTO.getType();
        //构造分页构造器
        Page<Category> pageInfo = new Page<>(page,pageSize);
        //构造条件构造器
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(StringUtils.isNotEmpty(name),Category::getName,name);
        queryWrapper.eq(type != null,Category::getType,type);
        //添加排序条件
        queryWrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);//按照降序

        //执行查询
        Page<Category> selectPage = categoryMapper.selectPage(pageInfo, queryWrapper);

        long total = selectPage.getTotal();
        List<Category> records = selectPage.getRecords();

        return new PageResult(total,records);

    }

    /**
     * 根据id删除分类
     * @param id
     */
    public void deleteCategoryById(Long id) {
        //查询当前分类是否关联了菜品，如果关联了就抛出业务异常
        LambdaQueryWrapper<Dish> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(Dish::getCategoryId,id);
        long count = dishMapper.selectCount(queryWrapper1);
        if(count > 0){
            //当前分类下有菜品，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }
        //查询当前分类是否关联了套餐，如果关联了就抛出业务异常
        LambdaQueryWrapper<Setmeal> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.eq(Setmeal::getCategoryId,id);
        count = setmealMapper.selectCount(queryWrapper2);
        if(count > 0){
            //当前分类下有菜品，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }

        //删除分类数据
        categoryMapper.deleteById(id);
    }

    /**
     * 修改分类
     * @param categoryDTO
     */
    public void updateCategory(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO,category);

        categoryMapper.updateById(category);
    }

    /**
     * 启用、禁用分类
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        Category category = Category.builder()
                .id(id)
                .status(status)
                .build();
        categoryMapper.updateById(category);
    }

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    public List<Category> listCategory(Integer type) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(type != null,Category::getType,type);
        List<Category> categoryList = categoryMapper.selectList(queryWrapper);
        return categoryList;
    }
}
