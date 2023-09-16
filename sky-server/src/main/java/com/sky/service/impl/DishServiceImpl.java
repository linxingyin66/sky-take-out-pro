package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import com.sky.service.DishFlavorService;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName DishServiceImpl
 * @Description
 * @Author LXY
 * @Date 2023/9/12 17:07
 **/
@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper,Dish> implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SetmealDishMapper setmealDishMapper;


    /**
     * 新增菜品和对应的口味
     *
     * @param dishDTO
     */
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        //向菜品表插入1条数据
        dishMapper.insert(dish);

        //获取insert语句生成的主键值
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            //向口味表插入n条数据
            dishFlavorService.saveBatch(flavors);
        }
    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {

        int page = dishPageQueryDTO.getPage();
        int pageSize = dishPageQueryDTO.getPageSize();
        String name = dishPageQueryDTO.getName();
        Integer categoryId = dishPageQueryDTO.getCategoryId();
        Integer status = dishPageQueryDTO.getStatus();

        //构造分页构造器对象
        Page<Dish> DishPage = new Page<>(page, pageSize);
        Page<DishVO> DishVOPage = new Page<>();

        //条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(name != null, Dish::getName, name);
        queryWrapper.eq(status != null, Dish::getStatus, status);
        queryWrapper.eq(categoryId != null, Dish::getCategoryId, categoryId);
        //添加排序条件
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        //执行分页查询
        dishMapper.selectPage(DishPage, queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(DishPage, DishVOPage, "records");

        List<Dish> records = DishPage.getRecords();

        List<DishVO> list = records.stream().map(item -> {
            DishVO dishVO = new DishVO();

            BeanUtils.copyProperties(item, dishVO);

            Long categoryIdd = item.getCategoryId();//分类id
            //根据id查询分类对象
            Category category = categoryService.getById(categoryIdd);

            if (category != null) {
                String categoryName = category.getName();
                dishVO.setCategoryName(categoryName);
            }
            return dishVO;
        }).collect(Collectors.toList());

        DishVOPage.setRecords(list);

        return new PageResult(DishVOPage.getTotal(), DishVOPage.getRecords());
    }


    /**
     * 菜品批量删除
     *
     * @param ids
     */
    @Transactional//事务
    public void deleteBatch(List<Long> ids) {
        //判断当前菜品是否能够删除---是否存在起售中的菜品？？
        for (Long id : ids) {
            Dish dish = dishMapper.selectById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                //当前菜品处于起售中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断当前菜品是否能够删除---是否被套餐关联了？？
        for (Long id : ids) {
            LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SetmealDish::getDishId, id);
            List<SetmealDish> setmealDishList = setmealDishMapper.selectList(queryWrapper);
            if (setmealDishList.size() != 0) {
                //当前菜品被套餐关联了，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL_DELETE);
            }
        }
        //删除菜品表中的菜品数据
        for (Long id : ids) {
            dishMapper.deleteById(id);
            //删除菜品关联的口味数据
            dishFlavorMapper.deleteById(id);
        }
    }

    /**
     * 根据id查询菜品和对应的口味数据
     *
     * @param id
     * @return
     */
    public DishVO getByIdWithFlavor(Long id) {
        //根据id查询菜品数据
        Dish dish = dishMapper.selectById(id);

        //根据菜品id查询口味数据
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, id);
        List<DishFlavor> dishFlavors = dishFlavorMapper.selectList(queryWrapper);

        //将查询到的数据封装到VO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);

        return dishVO;
    }

    /**
     * 根据id修改菜品基本信息和对应的口味信息
     *
     * @param dishDTO
     */
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        //修改菜品表基本信息
        dishMapper.updateById(dish);

        //删除原有的口味数据
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dishDTO.getId());
        dishFlavorMapper.delete(queryWrapper);

        //重新插入口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            //向口味表插入n条数据
            dishFlavorService.saveBatch(flavors);
        }
    }

    /**
     * 菜品起售停售
     *
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {

        Dish dish = new Dish();
        dish.setId(id);
        dish.setStatus(status);

        if(status == 0){
            LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SetmealDish::getDishId, id);
            List<SetmealDish> setmealDishList = setmealDishMapper.selectList(queryWrapper);
            if (setmealDishList.size() != 0) {
                //当前菜品被套餐关联了，不能停售
                throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL_STOP);
            }
            dishMapper.updateById(dish);
            return;
        }

        dishMapper.updateById(dish);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        LambdaQueryWrapper<Dish> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(Dish::getCategoryId, dish.getCategoryId());
        queryWrapper1.eq(Dish::getStatus,dish.getStatus());
        List<Dish> dishList = dishMapper.selectList(queryWrapper1);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            LambdaQueryWrapper<DishFlavor> queryWrapper2 = new LambdaQueryWrapper<>();
            queryWrapper2.eq(DishFlavor::getDishId, d.getId());
            List<DishFlavor> flavors = dishFlavorMapper.selectList(queryWrapper2);

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }
    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<Dish> list(Long categoryId) {
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Dish::getCategoryId,categoryId)
                        .eq(Dish::getStatus,StatusConstant.ENABLE);
        return dishService.list(queryWrapper);
    }
}