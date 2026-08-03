package com.dev.springmybatissql.service.impl;

import com.dev.springmybatissql.mapper.AnalysisMapper;
import com.dev.springmybatissql.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 数据分析服务实现
 *
 * 纯查询业务，无事务。职责：
 * 1. 参数校验
 * 2. 调用 AnalysisMapper（XML 中定义的标准 SQL）
 */
@Service
public class AnalysisServiceImpl implements AnalysisService {

    @Autowired
    private AnalysisMapper analysisMapper;

    @Override
    public List<Map<String, Object>> topSalaryByDept() {
        return analysisMapper.topSalaryByDept();
    }

    @Override
    public List<Map<String, Object>> aboveDeptAvgSalary() {
        return analysisMapper.aboveDeptAvgSalary();
    }

    @Override
    public List<Map<String, Object>> deptSalaryGradeStats() {
        return analysisMapper.deptSalaryGradeStats();
    }

    @Override
    public BigDecimal maxSalaryNoGroupFunc() {
        return analysisMapper.maxSalaryNoGroupFunc();
    }

    @Override
    public List<Map<String, Object>> topAvgSalaryDept() {
        return analysisMapper.topAvgSalaryDept();
    }

    @Override
    public List<Map<String, Object>> salaryTopN(int page, int size) {
        if (page < 1) {
            page = 1;
        }
        if (size < 1 || size > 50) {
            size = 5;
        }
        int offset = (page - 1) * size;
        return analysisMapper.salaryTopN(offset, size);
    }

    @Override
    public List<Map<String, Object>> latestHire(int size) {
        if (size < 1 || size > 50) {
            size = 5;
        }
        return analysisMapper.latestHire(size);
    }

    @Override
    public List<Map<String, Object>> employeeWithLeader() {
        return analysisMapper.employeeWithLeader();
    }

    @Override
    public List<Map<String, Object>> earlierThanLeader() {
        return analysisMapper.earlierThanLeader();
    }

    @Override
    public List<Map<String, Object>> allDeptWithEmployee() {
        return analysisMapper.allDeptWithEmployee();
    }

    @Override
    public List<Map<String, Object>> deptWithMinEmployee(int minCount) {
        if (minCount < 1) {
            minCount = 5;
        }
        return analysisMapper.deptWithMinEmployee(minCount);
    }

    @Override
    public List<Map<String, Object>> positionWithDeptAndCount(String position) {
        return analysisMapper.positionWithDeptAndCount(position);
    }

    @Override
    public List<Map<String, Object>> employeeInDept(String deptCode) {
        return analysisMapper.employeeInDept(deptCode);
    }

    @Override
    public List<Map<String, Object>> aboveCompanyAvg() {
        return analysisMapper.aboveCompanyAvg();
    }

    @Override
    public List<Map<String, Object>> higherThanAllOfDept(Long deptId) {
        return analysisMapper.higherThanAllOfDept(deptId);
    }

    @Override
    public List<Map<String, Object>> deptStats() {
        return analysisMapper.deptStats();
    }

    @Override
    public List<Map<String, Object>> annualSalary() {
        return analysisMapper.annualSalary();
    }

    @Override
    public List<Map<String, Object>> continuousLogin(int days) {
        if (days < 1) {
            days = 3;
        }
        return analysisMapper.continuousLogin(days);
    }

    @Override
    public List<Map<String, Object>> categorySalesTopN(int topN) {
        if (topN < 1 || topN > 10) {
            topN = 3;
        }
        return analysisMapper.categorySalesTopN(topN);
    }

    @Override
    public List<Map<String, Object>> monthlySalesTopN(int topN) {
        if (topN < 1 || topN > 10) {
            topN = 3;
        }
        return analysisMapper.monthlySalesTopN(topN);
    }

    @Override
    public List<Map<String, Object>> salaryGradeCount() {
        return analysisMapper.salaryGradeCount();
    }

    @Override
    public List<Map<String, Object>> orderStatusStats() {
        return analysisMapper.orderStatusStats();
    }

    @Override
    public List<Map<String, Object>> userConsumeRank() {
        return analysisMapper.userConsumeRank();
    }
}