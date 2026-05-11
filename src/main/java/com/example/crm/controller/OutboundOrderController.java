package com.example.crm.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.common.ExcelUtils;
import com.example.crm.common.Result;
import com.example.crm.entity.OutboundOrder;
import com.example.crm.entity.OutboundOrderItem;
import com.example.crm.entity.SysUser;
import com.example.crm.service.OutboundOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/outbound")
public class OutboundOrderController {

    private static final Logger log = LoggerFactory.getLogger(OutboundOrderController.class);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private OutboundOrderService outboundOrderService;

    @PostMapping("/order/page")
    public Result<IPage<OutboundOrder>> queryOutboundPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestBody(required = false) Map<String, Object> params) {
        try {
            Page<OutboundOrder> page = new Page<>(pageNum, pageSize);
            IPage<OutboundOrder> resultPage = outboundOrderService.queryOutboundPage(page, params);
            return Result.success(resultPage);
        } catch (Exception e) {
            log.error("分页查询出库单失败", e);
            return Result.error("查询出库单失败：" + e.getMessage());
        }
    }

    @GetMapping("/order/{outboundId}")
    public Result<OutboundOrder> getOutboundDetail(@PathVariable Long outboundId) {
        if (outboundId == null || outboundId <= 0) {
            return Result.error("出库单ID不能为空");
        }
        try {
            OutboundOrder outboundOrder = outboundOrderService.getOutboundDetail(outboundId);
            if (outboundOrder == null) {
                return Result.error("出库单不存在");
            }
            return Result.success(outboundOrder);
        } catch (Exception e) {
            log.error("查询出库单详情失败，ID={}", outboundId, e);
            return Result.error("查询详情失败：" + e.getMessage());
        }
    }

    @PostMapping("/order")
    public Result<?> addOutbound(@RequestBody Map<String, Object> request) {
        try {
            OutboundOrder outboundOrder = new OutboundOrder();
            outboundOrder.setReceiverName(request.get("receiverName").toString());
            outboundOrder.setReceiverContact(request.get("receiverContact").toString());
            outboundOrder.setOutboundTime(parseDateTimeSafely(request.get("outboundTime").toString()));
            outboundOrder.setOutboundReason(request.get("outboundReason").toString());
            outboundOrder.setRemarks(request.get("remarks") == null ? "" : request.get("remarks").toString());

            List<Map<String, Object>> itemList = (List<Map<String, Object>>) request.get("items");
            List<OutboundOrderItem> items = itemList.stream().map(item -> {
                OutboundOrderItem orderItem = new OutboundOrderItem();
                orderItem.setBatchId(Long.parseLong(item.get("batchId").toString()));
                orderItem.setActualOutQty(Integer.parseInt(item.get("actualOutQty").toString()));
                return orderItem;
            }).toList();

            boolean success = outboundOrderService.addOutbound(outboundOrder, items);
            if (success) {
                return Result.success("新增出库单成功");
            } else {
                return Result.error("新增出库单失败");
            }
        } catch (Exception e) {
            log.error("新增出库单失败", e);
            return Result.error("新增失败：" + e.getMessage());
        }
    }

    @PutMapping("/order")
    public Result<?> editOutbound(@RequestBody Map<String, Object> request) {
        try {
            Long outboundId = Long.parseLong(request.get("outboundId").toString());
            OutboundOrder oldOrder = outboundOrderService.getById(outboundId);
            if (oldOrder == null) {
                return Result.error("出库单不存在");
            }
            if (oldOrder.getAuditStatus() != 0) {
                return Result.error("仅待审核状态的出库单可编辑");
            }

            OutboundOrder outboundOrder = new OutboundOrder();
            outboundOrder.setOutboundId(outboundId);
            outboundOrder.setReceiverName(request.get("receiverName").toString());
            outboundOrder.setReceiverContact(request.get("receiverContact").toString());
            outboundOrder.setOutboundTime(parseDateTimeSafely(request.get("outboundTime").toString()));
            outboundOrder.setOutboundReason(request.get("outboundReason").toString());
            outboundOrder.setRemarks(request.get("remarks") == null ? "" : request.get("remarks").toString());

            List<Map<String, Object>> itemList = (List<Map<String, Object>>) request.get("items");
            List<OutboundOrderItem> items = itemList.stream().map(item -> {
                OutboundOrderItem orderItem = new OutboundOrderItem();
                orderItem.setBatchId(Long.parseLong(item.get("batchId").toString()));
                orderItem.setActualOutQty(Integer.parseInt(item.get("actualOutQty").toString()));
                return orderItem;
            }).toList();

            boolean success = outboundOrderService.editOutbound(outboundOrder, items);
            if (success) {
                return Result.success("编辑出库单成功");
            } else {
                return Result.error("编辑出库单失败");
            }
        } catch (Exception e) {
            log.error("编辑出库单失败", e);
            return Result.error("编辑失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/order/{outboundId}")
    public Result<?> deleteOutbound(@PathVariable Long outboundId) {
        if (outboundId == null || outboundId <= 0) {
            return Result.error("出库单ID不能为空");
        }
        try {
            OutboundOrder outboundOrder = outboundOrderService.getById(outboundId);
            if (outboundOrder == null) {
                return Result.error("出库单不存在");
            }
            if (outboundOrder.getAuditStatus() != 0) {
                return Result.error("仅待审核状态的出库单可删除");
            }

            LambdaUpdateWrapper<OutboundOrder> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(OutboundOrder::getOutboundId, outboundId)
                    .set(OutboundOrder::getIsDeleted, 1);

            boolean success = outboundOrderService.update(wrapper);

            if (success) {
                return Result.success("删除出库单成功");
            } else {
                return Result.error("删除出库单失败");
            }
        } catch (Exception e) {
            log.error("删除出库单失败，ID={}", outboundId, e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    // ==================== ✅ 6. 完善：出库单导出接口 ====================
    @GetMapping("/order/export")
    public void exportOutbound(
            @RequestParam(required = false) String receiverName,
            @RequestParam(required = false) String outboundTimeStart,
            @RequestParam(required = false) String outboundTimeEnd,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10000") Integer pageSize,
            HttpServletResponse response
    ) {
        try {
            // 1. 封装查询参数
            Map<String, Object> params = new HashMap<>();
            params.put("receiverName", receiverName);
            params.put("outboundTimeStart", outboundTimeStart);
            params.put("outboundTimeEnd", outboundTimeEnd);
            params.put("pageNum", pageNum);
            params.put("pageSize", pageSize);

            // 2. 查询数据
            Page<OutboundOrder> page = new Page<>(pageNum, pageSize);
            IPage<OutboundOrder> resultPage = outboundOrderService.queryOutboundPage(page, params);
            List<OutboundOrder> list = resultPage.getRecords();

            // 3. 规范设置响应头
            response.reset();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String fileName = "出库单列表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename=" + encodedFileName + ".xlsx;filename*=UTF-8''" + encodedFileName + ".xlsx");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

            // 4. 定义Excel表头和数据
            List<String> head = Arrays.asList(
                    "出库单ID", "接收人", "联系电话", "出库时间", "出库原因", "审核状态", "备注", "创建时间"
            );

            List<List<String>> dataList = new ArrayList<>();
            for (OutboundOrder oo : list) {
                List<String> row = new ArrayList<>();
                row.add(oo.getOutboundId() == null ? "" : oo.getOutboundId().toString());
                row.add(oo.getReceiverName() == null ? "" : oo.getReceiverName());
                row.add(oo.getReceiverContact() == null ? "" : oo.getReceiverContact());
                row.add(oo.getOutboundTime() == null ? "" : oo.getOutboundTime().format(DATETIME_FORMATTER));
                row.add(oo.getOutboundReason() == null ? "" : oo.getOutboundReason());
                row.add(getAuditStatusText(oo.getAuditStatus()));
                row.add(oo.getRemarks() == null ? "" : oo.getRemarks());
                row.add(oo.getCreateTime() == null ? "" : oo.getCreateTime().format(DATETIME_FORMATTER));
                dataList.add(row);
            }

            // 5. 写入Excel输出流
            ExcelUtils.exportExcel(response.getOutputStream(), "出库单列表", head, dataList);
            log.info("出库单导出成功，导出条数：{}", list.size());

        } catch (Exception e) {
            log.error("出库单导出失败", e);
            try {
                response.reset();
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"导出失败：" + e.getMessage() + "\"}");
            } catch (IOException ex) {
                log.error("导出异常响应写入失败", ex);
            }
        }
    }

    @PostMapping("/order/updateStatus")
    public Result<?> updateOutboundStatus(@RequestBody Map<String, Object> params) {
        try {
            Long outboundId = Long.parseLong(params.get("outboundId").toString());
            Integer auditStatus = Integer.parseInt(params.get("auditStatus").toString());
            String auditOpinion = params.get("auditOpinion") == null ? "" : params.get("auditOpinion").toString();

            Map<String, Object> result = outboundOrderService.updateOutboundStatus(outboundId, auditStatus, auditOpinion);

            if ((Boolean) result.get("success")) {
                return Result.success((String) result.get("message"));
            } else {
                return Result.error((String) result.get("message"));
            }

        } catch (Exception e) {
            log.error("更新出库单状态失败", e);
            return Result.error("系统异常，状态更新失败");
        }
    }

    @PostMapping("/order/cancelAudit")
    public Result<?> cancelAudit(@RequestBody Map<String, Object> params) {
        try {
            Long outboundId = Long.parseLong(params.get("outboundId").toString());
            Map<String, Object> result = outboundOrderService.cancelOutboundAudit(outboundId);
            if ((Boolean) result.get("success")) {
                return Result.success((String) result.get("message"));
            } else {
                return Result.error((String) result.get("message"));
            }
        } catch (Exception e) {
            log.error("取消审核失败", e);
            return Result.error("取消审核失败");
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof SysUser sysUser) {
            return sysUser.getId();
        }
        return 5L;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof SysUser sysUser) {
            return sysUser.getLoginName();
        }
        return "测试用户";
    }

    private LocalDateTime parseDateTimeSafely(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) return null;
        String cleanStr = dateTimeStr.trim();
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(cleanStr, formatter);
            } catch (Exception ignored) {}
        }
        throw new IllegalArgumentException("时间格式解析失败：" + dateTimeStr);
    }

    private String getAuditStatusText(Integer status) {
        Map<Integer, String> statusMap = new HashMap<>();
        statusMap.put(0, "待审核");
        statusMap.put(1, "已通过");
        statusMap.put(2, "已驳回");
        return statusMap.getOrDefault(status, "未知状态");
    }
}