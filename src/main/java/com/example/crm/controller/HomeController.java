package com.example.crm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.crm.common.Result;
import com.example.crm.dto.TraceAuditDTO; // 需新增该DTO类
import com.example.crm.dto.TraceRecordQueryDTO;
import com.example.crm.entity.TraceRecord;
import com.example.crm.service.TraceRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/home")
public class HomeController {

    @Autowired
    private TraceRecordService traceRecordService;

    // ==================== 原有接口（仅修改新增/更新/删除逻辑） ====================
    @GetMapping("/stat")
    public Result<Map<String, Object>> getHomeStat() {
        try {
            return Result.success("查询成功", traceRecordService.getTraceStat());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询统计数据失败：" + e.getMessage());
        }
    }

    @GetMapping("/recent/trace")
    public Result<List<TraceRecord>> getRecentTrace() {
        try {
            return Result.success("查询成功", traceRecordService.getRecentTraceList());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询流向跟踪记录失败：" + e.getMessage());
        }
    }

    // 条件分页查询（已兼容审核状态筛选，需配合Service层修改）
    @PostMapping("/trace/page")
    public Result<IPage<TraceRecord>> queryTracePage(@RequestBody TraceRecordQueryDTO queryDTO) {
        try {
            IPage<TraceRecord> page = traceRecordService.queryTraceRecordPage(queryDTO);
            return Result.success("查询成功", page);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("条件查询流向跟踪记录失败：" + e.getMessage());
        }
    }

    // 新增追溯记录：默认设置为【待审核】状态
    @PostMapping("/trace")
    public Result<?> addTrace(@RequestBody TraceRecord traceRecord) {
        try {
            // 新增记录默认审核状态为 0（待审核）
            traceRecord.setAuditStatus(0);
            boolean success = traceRecordService.addTraceRecord(traceRecord);
            return success ? Result.success("新增成功") : Result.error("新增失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("新增失败：" + e.getMessage());
        }
    }

    // 修改追溯记录：禁止修改已审核数据
    @PutMapping("/trace")
    public Result<?> updateTrace(@RequestBody TraceRecord traceRecord) {
        try {
            // 校验：已审核（状态=1）的记录禁止修改
            TraceRecord oldRecord = traceRecordService.getTraceDetail(traceRecord.getTraceId());
            if (oldRecord != null && oldRecord.getAuditStatus() == 1) {
                return Result.error("已审核的追溯记录不允许修改");
            }
            boolean success = traceRecordService.updateTraceRecord(traceRecord);
            return success ? Result.success("修改成功") : Result.error("修改失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("修改失败：" + e.getMessage());
        }
    }

    // 删除追溯记录：禁止删除已审核数据
    @DeleteMapping("/trace/{traceId}")
    public Result<?> deleteTrace(@PathVariable Long traceId) {
        try {
            // 校验：已审核（状态=1）的记录禁止删除
            TraceRecord record = traceRecordService.getTraceDetail(traceId);
            if (record != null && record.getAuditStatus() == 1) {
                return Result.error("已审核的追溯记录不允许删除");
            }
            boolean success = traceRecordService.deleteTraceRecord(traceId);
            return success ? Result.success("删除成功") : Result.error("删除失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    @GetMapping("/trace/{traceId}")
    public Result<TraceRecord> getTraceDetail(@PathVariable Long traceId) {
        try {
            TraceRecord record = traceRecordService.getTraceDetail(traceId);
            return Result.success("查询成功", record);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询详情失败：" + e.getMessage());
        }
    }

    // ==================== 新增：审核核心接口 ====================
    /**
     * 审核追溯记录（前端调用该接口完成审核）
     * @param auditDTO 审核参数（含追溯ID、审核结果、审核人信息等）
     */
    @PostMapping("/trace/audit")
    public Result<?> auditTraceRecord(@Validated @RequestBody TraceAuditDTO auditDTO) {
        try {
            // 基础校验：驳回时必须填写备注
            if (auditDTO.getAuditResult() == 2 && (auditDTO.getAuditRemark() == null || auditDTO.getAuditRemark().trim().isEmpty())) {
                return Result.error("驳回审核时必须填写备注说明原因");
            }

            // 调用Service执行审核
            boolean auditSuccess = traceRecordService.auditTraceRecord(auditDTO);
            if (!auditSuccess) {
                return Result.error("审核操作失败");
            }

            // 审核通过时，自动同步相关数据
            if (auditDTO.getAuditResult() == 1) {
                boolean syncSuccess = traceRecordService.syncTraceData(auditDTO.getTraceId());
                if (syncSuccess) {
                    return Result.success("审核通过且数据同步成功");
                } else {
                    // 核心修改：用success替换warning，保留原警告语义
                    return Result.success("审核通过，但数据同步失败，请手动重试");
                }
            } else {
                return Result.success("审核驳回成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("审核失败：" + e.getMessage());
        }
    }

    /**
     * 手动同步追溯数据（供审核同步失败后重试）
     * @param traceId 追溯记录ID
     */
    @PostMapping("/trace/sync/{traceId}")
    public Result<?> syncTraceData(@PathVariable Long traceId) {
        try {
            boolean success = traceRecordService.syncTraceData(traceId);
            return success ? Result.success("数据同步成功") : Result.error("数据同步失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("同步失败：" + e.getMessage());
        }
    }
}