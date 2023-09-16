package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;
import com.sky.result.Result;

public interface EmployeeService extends IService<Employee> {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);


    /**
     * @Author LXY
     * @Description 新增员工
     * @Date 2023/9/12
     * @param employee
     * @return Result<String>
     **/
    Result saveEmp(Employee employee);

    /**
     * @Author LXY
     * @Description 员工信息分页查询
     * @Date 2023/9/12
     * @param page
     * @param pageSize
     * @param name
     * @return PageResult
     **/
    Result<PageResult> page(int page, int pageSize, String name);

    /**
     * @Author LXY
     * @Description 启用/禁用员工
     * @Date 2023/9/12
     * @param status
     * @param id
     * @return Result
     **/
    Result startOrStop(int status, long id);

    /**
     * @Author LXY
     * @Description 根据id查询员工信息（修改员工信息时的数据信息回显）
     * @Date 2023/9/12
     * @param id
     * @return Result<Employee>
     **/
    Result<Employee> getEmpById(Long id);

    /**
     * 修改员工信息
     * @param employee
     * @return
     */
    Result updateEmp(Employee employee);
}
