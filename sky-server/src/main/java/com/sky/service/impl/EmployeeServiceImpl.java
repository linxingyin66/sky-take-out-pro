package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;

@Service
@Slf4j
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper,Employee> implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername,username);
        Employee employee = employeeMapper.selectOne(queryWrapper);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        //需要进行md5加密，然后再进行比对
        //123456 -> md5: e10adc3949ba59abbe56e057f20f883e
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * @param employee
     * @return Result<String>
     * @Author LXY
     * @Description 新增员工
     * @Date 2023/9/12
     **/
    @Override
    public Result saveEmp(Employee employee) {
        employeeMapper.insert(employee);
        return Result.success();
    }

    /**
     * @param page
     * @param pageSize
     * @param name
     * @return PageResult
     * @Author LXY
     * @Description 员工信息分页查询
     * @Date 2023/9/12
     **/
    @Override
    public Result<PageResult> page(int page, int pageSize, String name) {
        log.info("page = {},pageSize = {},name = {}" ,page,pageSize,name);

        //构造分页构造器
        Page<Employee> pageInfo = new Page<>(page,pageSize);

        //构造条件构造器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper();
        //添加过滤条件
        queryWrapper.like(StringUtils.isNotEmpty(name),Employee::getName,name);
        //添加排序条件
        queryWrapper.orderByDesc(Employee::getUpdateTime);//按照降序

        //执行查询
        Page<Employee> selectPage = employeeMapper.selectPage(pageInfo, queryWrapper);

        long total = selectPage.getTotal();
        List<Employee> records = selectPage.getRecords();

        return Result.success(new PageResult(total,records));
    }

    /**
     * @param status
     * @param id
     * @return Result
     * @Author LXY
     * @Description 启用/禁用员工
     * @Date 2023/9/12
     **/
    @Override
    public Result startOrStop(int status, long id) {

        Employee employee = new Employee();
        employee.setId(id);
        employee.setStatus(status);

        employeeMapper.updateById(employee);
        return Result.success();
    }

    /**
     * @param id
     * @return Result<Employee>
     * @Author LXY
     * @Description 根据id查询员工信息（修改员工信息时的数据信息回显）
     * @Date 2023/9/12
     **/
    @Override
    public Result<Employee> getEmpById(Long id) {

        Employee employee = employeeMapper.selectById(id);
        return Result.success(employee);
    }

    /**
     * 修改员工信息
     *
     * @param employee
     * @return
     */
    @Override
    public Result updateEmp(Employee employee) {
        employeeMapper.updateById(employee);
        return Result.success();
    }

}
