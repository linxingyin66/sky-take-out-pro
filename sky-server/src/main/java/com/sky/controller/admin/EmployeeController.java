package com.sky.controller.admin;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success();
    }

    /**
     * 新增员工
     * @param employee
     * @return
     */
    @PostMapping
    public Result saveEmp(@RequestBody Employee employee){

        log.info("新增员工，员工信息：{}",employee.toString());

        Result result = employeeService.saveEmp(employee);

        return result;
    }

    /**
     * 员工信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> page(int page, int pageSize, String name){
        log.info("page = {},pageSize = {},name = {}" ,page,pageSize,name);

        Result<PageResult> result = employeeService.page(page, pageSize, name);
        return result;
    }

    /**
     * @Author LXY
     * @Description 根据id修改员工信息、启用/禁用员工
     * @Date 2023/9/12
     * @param status
     * @param id
     * @return Result
     **/
    @PostMapping ("/status/{status}")
    public Result startOrStop(@PathVariable int status,long id){

        Result result= employeeService.startOrStop(status,id);

        return result;
    }


    /**
     * @Author LXY
     * @Description 根据id查询员工信息（修改员工信息时的数据信息回显）
     * @Date 2023/9/12
     * @param id
     * @return Result<Employee>
     **/
    @GetMapping("/{id}")
    public Result<Employee> getEmpById(@PathVariable Long id){
        log.info("根据id查询员工信息...");

        Result<Employee> result = employeeService.getEmpById(id);

        return result;
    }

    /**
     * 修改员工信息
     * @param employee
     * @return
     */
    @PutMapping
    public Result updateEmp(@RequestBody Employee employee){

        Result result = employeeService.updateEmp(employee);

        return result;
    }


}
