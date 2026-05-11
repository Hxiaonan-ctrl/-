package com.example.crm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.common.ExcelUtils;
import com.example.crm.common.Result;
import com.example.crm.entity.WarehousingOrder;
import com.example.crm.entity.WarehousingOrderItem;
import com.example.crm.service.WarehousingOrderService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/warehousing/order")
public class WarehousingOrderController {

    private static final Logger log = LoggerFactory.getLogger(WarehousingOrderController.class);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private WarehousingOrderService warehousingOrderService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== 1. 分页查询入库单 ====================
    @PostMapping("/page")
    public Result<IPage<WarehousingOrder>> queryWarehousingPage(@RequestBody Map<String, Object> params) {
        try {
            Long pageNum = Long.parseLong(params.getOrDefault("pageNum", 1).toString());
            Long pageSize = Long.parseLong(params.getOrDefault("pageSize", 10).toString());
            Page<WarehousingOrder> page = new Page<>(pageNum, pageSize);

            IPage<WarehousingOrder> result = warehousingOrderService.queryWarehousingPage(page, params);
            log.info("分页查询入库单成功：共{}条数据，当前第{}页", result.getTotal(), pageNum);
            return Result.success(result);
        } catch (NumberFormatException e) {
            log.error("分页查询入库单失败：分页参数格式错误", e);
            return Result.error("分页参数格式错误！pageNum和pageSize需为数字类型");
        } catch (Exception e) {
            log.error("分页查询入库单失败：系统异常", e);
            return Result.error("分页查询入库单失败：" + e.getMessage());
        }
    }

    // ==================== 2. 查询入库单详情（含明细） ====================
    @GetMapping("/{warehousingId}")
    public Result<WarehousingOrder> getWarehousingDetail(@PathVariable Long warehousingId) {
        try {
            if (warehousingId == null || warehousingId <= 0) {
                return Result.error("入库单ID不能为空且必须为正整数");
            }

            WarehousingOrder detail = warehousingOrderService.getWarehousingDetail(warehousingId);
            if (detail == null) {
                return Result.error("入库单不存在");
            }
            log.info("查询入库单详情成功：warehousingId={}", warehousingId);
            return Result.success(detail);
        } catch (Exception e) {
            log.error("查询入库单详情失败：warehousingId={}", warehousingId, e);
            return Result.error("查询入库单详情失败：" + e.getMessage());
        }
    }

    // ==================== 3. 新增入库单（含明细） ====================
    @PostMapping
    public Result<?> addWarehousing(@RequestBody Map<String, Object> params) {
        try {
            WarehousingOrder warehousingOrder = new WarehousingOrder();

            Object purchaseIdObj = params.get("purchaseId");
            if (purchaseIdObj == null) {
                return Result.error("采购单ID不能为空");
            }
            warehousingOrder.setPurchaseId(Long.parseLong(purchaseIdObj.toString()));

            Object warehousingTimeObj = params.get("warehousingTime");
            if (warehousingTimeObj == null) {
                return Result.error("入库时间不能为空");
            }
            String warehousingTimeStr = warehousingTimeObj.toString();
            LocalDateTime warehousingTime = LocalDateTime.parse(warehousingTimeStr, DATETIME_FORMATTER);
            warehousingOrder.setWarehousingTime(warehousingTime);

            Object auditStatusObj = params.get("auditStatus");
            warehousingOrder.setAuditStatus(auditStatusObj == null ? 0 : Integer.parseInt(auditStatusObj.toString()));

            Object remarksObj = params.get("remarks");
            warehousingOrder.setRemarks(remarksObj == null ? "" : remarksObj.toString());

            Object operatorIdObj = params.get("operatorId");
            if (operatorIdObj == null) {
                return Result.error("操作人ID不能为空");
            }
            warehousingOrder.setOperatorId(Long.parseLong(operatorIdObj.toString()));

            Object itemsObj = params.get("items");
            if (itemsObj == null) {
                return Result.error("入库明细不能为空");
            }

            List<WarehousingOrderItem> items = new ArrayList<>();
            if (itemsObj instanceof List) {
                List<?> itemList = (List<?>) itemsObj;
                if (itemList.isEmpty()) {
                    return Result.error("入库明细不能为空");
                }

                for (Object obj : itemList) {
                    WarehousingOrderItem item = objectMapper.convertValue(obj, WarehousingOrderItem.class);
                    items.add(item);
                }
            } else {
                return Result.error("入库明细格式错误，需为数组类型");
            }

            boolean success = warehousingOrderService.addWarehousing(warehousingOrder, items);
            if (success) {
                log.info("新增入库单成功：purchaseId={}", warehousingOrder.getPurchaseId());
                return Result.success("新增入库单成功");
            } else {
                return Result.error("新增入库单失败");
            }
        } catch (NumberFormatException e) {
            log.error("新增入库单失败：参数格式错误", e);
            return Result.error("参数格式错误！采购单ID、操作人ID需为数字类型");
        } catch (IllegalArgumentException e) {
            log.error("新增入库单失败：参数校验失败", e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("新增入库单失败：系统异常", e);
            return Result.error("新增入库单失败：" + e.getMessage());
        }
    }

    // ==================== 4. 编辑入库单（含明细） ====================
    @PutMapping
    public Result<?> editWarehousing(@RequestBody Map<String, Object> params) {
        try {
            if (params.get("warehousingId") == null) {
                return Result.error("入库单ID不能为空");
            }
            WarehousingOrder warehousingOrder = new WarehousingOrder();
            warehousingOrder.setWarehousingId(Long.parseLong(params.get("warehousingId").toString()));
            warehousingOrder.setPurchaseId(Long.parseLong(params.get("purchaseId").toString()));
            warehousingOrder.setOperatorId(Long.parseLong(params.get("operatorId").toString()));

            String warehousingTimeStr = params.get("warehousingTime").toString();
            warehousingOrder.setWarehousingTime(LocalDateTime.parse(warehousingTimeStr, DATETIME_FORMATTER));

            warehousingOrder.setAuditStatus(params.get("auditStatus") == null ? 0 : Integer.parseInt(params.get("auditStatus").toString()));
            warehousingOrder.setRemarks(params.get("remarks") == null ? "" : params.get("remarks").toString());

            Object itemsObj = params.get("items");
            List<WarehousingOrderItem> items = new ArrayList<>();
            if (itemsObj instanceof List) {
                List<?> itemList = (List<?>) itemsObj;
                for (Object obj : itemList) {
                    WarehousingOrderItem item = objectMapper.convertValue(obj, WarehousingOrderItem.class);
                    items.add(item);
                }
            }

            if (items.isEmpty()) {
                return Result.error("入库明细不能为空");
            }

            boolean success = warehousingOrderService.editWarehousing(warehousingOrder, items);
            if (success) {
                log.info("编辑入库单成功：warehousingId={}", warehousingOrder.getWarehousingId());
                return Result.success("编辑入库单成功");
            } else {
                return Result.error("编辑入库单失败");
            }
        } catch (NumberFormatException e) {
            log.error("编辑入库单失败：参数格式错误", e);
            return Result.error("参数格式错误！入库单ID、采购单ID、操作人ID需为数字类型");
        } catch (IllegalArgumentException e) {
            log.error("编辑入库单失败：参数校验失败", e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("编辑入库单失败：系统异常", e);
            return Result.error("编辑入库单失败：" + e.getMessage());
        }
    }

    // ==================== 5. 删除入库单 ====================
    @DeleteMapping("/{warehousingId}")
    public Result<?> deleteWarehousing(@PathVariable Long warehousingId) {
        try {
            if (warehousingId == null || warehousingId <= 0) {
                return Result.error("入库单ID不能为空且必须为正整数");
            }

            boolean success = warehousingOrderService.deleteWarehousing(warehousingId);
            if (success) {
                log.info("删除入库单成功：warehousingId={}", warehousingId);
                return Result.success("删除入库单成功");
            } else {
                return Result.error("删除入库单失败：入库单不存在");
            }
        } catch (IllegalArgumentException e) {
            log.error("删除入库单失败：参数校验失败", e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除入库单失败：系统异常", e);
            return Result.error("删除入库单失败：" + e.getMessage());
        }
    }

    // ==================== 6. 更新入库单状态 ====================
    @PostMapping("/updateStatus")
    public Result<?> updateWarehousingStatus(@RequestBody Map<String, Object> params) {
        if (params == null) {
            log.warn("更新入库单状态：请求参数为空");
            return Result.error("请求参数不能为空");
        }

        try {
            Object warehousingIdObj = params.get("warehousingId");
            Object auditStatusObj = params.get("auditStatus");
            Object auditorIdObj = params.get("auditorId");
            Object auditTimeStrObj = params.get("auditTime");

            if (warehousingIdObj == null || auditStatusObj == null || auditorIdObj == null || auditTimeStrObj == null) {
                return Result.error("入库单ID、审核状态、审核人ID、审核时间不能为空");
            }

            Long warehousingId = Long.parseLong(warehousingIdObj.toString().trim());
            Integer auditStatus = Integer.parseInt(auditStatusObj.toString().trim());
            Long auditorId = Long.parseLong(auditorIdObj.toString().trim());
            String auditTimeStr = auditTimeStrObj.toString().trim();

            if (auditStatus != 0 && auditStatus != 1) {
                return Result.error("审核状态只能是0（待审核）或1（已通过）");
            }

            Map<String, Object> result = warehousingOrderService.updateWarehousingStatus(warehousingId, auditStatus, auditTimeStr);

            if ((Boolean) result.get("success")) {
                log.info("更新入库单状态成功：入库单ID={}，新审核状态={}", warehousingId, auditStatus);
                return Result.success((String) result.get("message"));
            } else {
                log.warn("更新入库单状态失败：入库单ID={}，原因={}", warehousingId, result.get("message"));
                return Result.error((String) result.get("message"));
            }

        } catch (NumberFormatException e) {
            log.error("更新入库单状态：参数格式错误", e);
            return Result.error("参数格式错误！入库单ID、审核状态、审核人ID需为数字类型");
        } catch (Exception e) {
            log.error("更新入库单状态：系统异常", e);
            return Result.error("系统异常，入库单状态更新失败：" + e.getMessage());
        }
    }

    // ==================== ✅ 7. 新增：入库单导出接口 ====================
    @GetMapping("/export")
    public void exportWarehousing(
            @RequestParam(required = false) String purchaseNo,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String warehousingTimeStart,
            @RequestParam(required = false) String warehousingTimeEnd,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10000") Integer pageSize,
            HttpServletResponse response
    ) {
        try {
            // 1. 封装查询参数，复用分页查询逻辑
            Map<String, Object> params = new HashMap<>();
            params.put("purchaseNo", purchaseNo);
            params.put("operatorName", operatorName);
            params.put("warehousingTimeStart", warehousingTimeStart);
            params.put("warehousingTimeEnd", warehousingTimeEnd);
            params.put("pageNum", pageNum);
            params.put("pageSize", pageSize);

            // 2. 查询数据
            Page<WarehousingOrder> page = new Page<>(pageNum, pageSize);
            IPage<WarehousingOrder> resultPage = warehousingOrderService.queryWarehousingPage(page, params);
            List<WarehousingOrder> list = resultPage.getRecords();

            // 3. 规范设置响应头
            response.reset();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String fileName = "入库单列表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename=" + encodedFileName + ".xlsx;filename*=UTF-8''" + encodedFileName + ".xlsx");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

            // 4. 定义Excel表头和数据
            List<String> head = Arrays.asList(
                    "入库单ID", "采购单ID", "操作人", "入库时间", "审核状态", "备注", "创建时间"
            );

            List<List<String>> dataList = new ArrayList<>();
            for (WarehousingOrder wo : list) {
                List<String> row = new ArrayList<>();
                row.add(wo.getWarehousingId() == null ? "" : wo.getWarehousingId().toString());
                row.add(wo.getPurchaseId() == null ? "" : wo.getPurchaseId().toString());
                row.add(wo.getOperatorName() == null ? "" : wo.getOperatorName());
                row.add(wo.getWarehousingTime() == null ? "" : wo.getWarehousingTime().format(DATETIME_FORMATTER));
                row.add(getAuditStatusText(wo.getAuditStatus()));
                row.add(wo.getRemarks() == null ? "" : wo.getRemarks());
                row.add(wo.getCreateTime() == null ? "" : wo.getCreateTime().format(DATETIME_FORMATTER));
                dataList.add(row);
            }

            // 5. 写入Excel输出流
            ExcelUtils.exportExcel(response.getOutputStream(), "入库单列表", head, dataList);
            log.info("入库单导出成功，导出条数：{}", list.size());

        } catch (Exception e) {
            log.error("入库单导出失败", e);
            try {
                response.reset();
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"导出失败：" + e.getMessage() + "\"}");
            } catch (IOException ex) {
                log.error("导出异常响应写入失败", ex);
            }
        }
    }

    private String getAuditStatusText(Integer status) {
        Map<Integer, String> statusMap = new HashMap<>();
        statusMap.put(0, "待审核");
        statusMap.put(1, "已通过");
        statusMap.put(2, "已驳回");
        return statusMap.getOrDefault(status, "未知状态");
    }
}