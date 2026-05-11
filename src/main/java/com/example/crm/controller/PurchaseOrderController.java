package com.example.crm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.common.ExcelUtils;
import com.example.crm.common.Result;
import com.example.crm.dto.PurchaseOrderDTO;
import com.example.crm.entity.PurchaseOrder;
import com.example.crm.entity.PurchaseOrderItem;
import com.example.crm.entity.SysUser;
import com.example.crm.service.PurchaseOrderService;
import com.example.crm.service.SysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/purchase")
public class PurchaseOrderController {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderController.class);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private SysUserService sysUserService;

    // ==================== 1. 分页查询（保持不变） ====================
    @PostMapping("/order/page")
    public Result<?> queryPage(@RequestBody Map<String, Object> params) {
        try {
            Page<PurchaseOrder> page = new Page<>(
                    Integer.parseInt(params.getOrDefault("pageNum", "1").toString()),
                    Integer.parseInt(params.getOrDefault("pageSize", "10").toString())
            );
            IPage<PurchaseOrder> result = purchaseOrderService.queryPurchasePage(page, params);
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    // ==================== 2. 详情查询（保持不变） ====================
    @GetMapping("/order/{purchaseId}")
    public Result<?> getDetail(@PathVariable Long purchaseId) {
        try {
            PurchaseOrder purchase = purchaseOrderService.getPurchaseDetail(purchaseId);
            if (purchase != null) {
                return Result.success(purchase);
            } else {
                return Result.error("采购单不存在");
            }
        } catch (Exception e) {
            log.error("详情查询失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    // ==================== 3. 新增采购单（最终修复版） ====================
    @PostMapping("/order")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> addPurchase(@RequestBody PurchaseOrderDTO dto) {
        Long currentUserId = null;

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                currentUserId = 5L;
                log.warn("未获取到有效登录信息，临时使用测试用户ID: {}", currentUserId);
            } else {
                if (authentication.getPrincipal() instanceof SysUser sysUser) {
                    currentUserId = sysUser.getId();
                } else {
                    String username = authentication.getName();
                    if (!StringUtils.hasText(username)) {
                        return Result.error("登录用户名不能为空");
                    }
                    SysUser sysUser = sysUserService.getOne(new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getUsername, username));
                    if (sysUser == null) {
                        return Result.error("登录用户不存在：" + username);
                    }
                    currentUserId = sysUser.getId();
                }
            }

            if (!StringUtils.hasText(dto.getSupplierName())) {
                return Result.error("供应商名称不能为空");
            }
            if (!StringUtils.hasText(dto.getSupplierContact())) {
                return Result.error("供应商联系方式不能为空");
            }
            if (dto.getTotalAmount() == null || dto.getTotalAmount().compareTo(BigDecimal.ZERO) < 0) {
                return Result.error("采购总金额不能为空且不能为负数");
            }
            if (dto.getItems() == null || dto.getItems().isEmpty()) {
                return Result.error("采购明细不能为空，请至少添加一条采购药品");
            }
            for (PurchaseOrderDTO.PurchaseOrderItemDTO itemDto : dto.getItems()) {
                if (itemDto.getDrugId() == null) {
                    return Result.error("采购明细药品ID(drugId)不能为空");
                }
                if (itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
                    return Result.error("采购明细数量必须大于0");
                }
                if (itemDto.getUnitPrice() == null || itemDto.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                    return Result.error("采购明细单价不能为空且不能为负数");
                }
            }

            PurchaseOrder purchaseOrder = new PurchaseOrder();
            BeanUtils.copyProperties(dto, purchaseOrder);

            if (!StringUtils.hasText(dto.getOrderNo())) {
                String orderNo = "PO" + System.currentTimeMillis() + IdWorker.getIdStr().substring(0, 6);
                purchaseOrder.setOrderNo(orderNo);
            } else {
                boolean orderNoExist = purchaseOrderService.count(new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getOrderNo, dto.getOrderNo())) > 0;
                if (orderNoExist) {
                    return Result.error("订单编号已存在：" + dto.getOrderNo());
                }
                purchaseOrder.setOrderNo(dto.getOrderNo());
            }

            purchaseOrder.setTotalAmount(dto.getTotalAmount().setScale(2, BigDecimal.ROUND_HALF_UP));
            purchaseOrder.setPurchaserId(currentUserId);
            purchaseOrder.setOrderStatus(0);
            purchaseOrder.setIsDeleted(0);
            purchaseOrder.setSupplierPhone(dto.getSupplierPhone());
            purchaseOrder.setRemark(dto.getRemark());
            purchaseOrder.setExpectedDate(dto.getExpectedDate());

            List<PurchaseOrderItem> items = new ArrayList<>();
            for (PurchaseOrderDTO.PurchaseOrderItemDTO itemDto : dto.getItems()) {
                PurchaseOrderItem item = new PurchaseOrderItem();
                item.setDrugId(itemDto.getDrugId());
                item.setPurchaseQty(itemDto.getQuantity());
                item.setUnitPrice(itemDto.getUnitPrice().setScale(2, BigDecimal.ROUND_HALF_UP));
                BigDecimal itemTotalAmount = itemDto.getUnitPrice()
                        .multiply(BigDecimal.valueOf(itemDto.getQuantity()))
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                item.setTotalAmount(itemTotalAmount);
                item.setBatchNo(itemDto.getBatchNo());
                items.add(item);
            }

            boolean success = purchaseOrderService.addPurchase(purchaseOrder, items);
            if (success) {
                return Result.success("新增采购单成功", purchaseOrder.getOrderNo());
            } else {
                return Result.error("新增采购单失败：Service层执行返回false");
            }

        } catch (NumberFormatException e) {
            log.error("新增采购单失败：数字格式异常", e);
            return Result.error("新增失败：金额/数量格式错误");
        } catch (Exception e) {
            log.error("新增采购单失败", e);
            return Result.error("新增失败：" + e.getMessage());
        }
    }

    // ==================== 4. 修改采购单 ====================
    @PutMapping("/order")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updatePurchase(@RequestBody PurchaseOrderDTO dto) {
        try {
            if (dto.getPurchaseId() == null) {
                return Result.error("采购单ID不能为空");
            }

            PurchaseOrder originalOrder = purchaseOrderService.getById(dto.getPurchaseId());
            if (originalOrder == null) {
                return Result.error("采购单不存在，ID：" + dto.getPurchaseId());
            }
            if (originalOrder.getOrderStatus() != 0) {
                return Result.error("仅待审核状态的采购单可修改，当前状态：" + getStatusText(originalOrder.getOrderStatus()));
            }

            if (!StringUtils.hasText(dto.getSupplierName())) {
                return Result.error("供应商名称不能为空");
            }
            if (!StringUtils.hasText(dto.getSupplierContact())) {
                return Result.error("供应商联系方式不能为空");
            }
            if (dto.getTotalAmount() == null || dto.getTotalAmount().compareTo(BigDecimal.ZERO) < 0) {
                return Result.error("采购总金额不能为空且不能为负数");
            }

            PurchaseOrder purchaseOrder = new PurchaseOrder();
            BeanUtils.copyProperties(dto, purchaseOrder);
            purchaseOrder.setTotalAmount(dto.getTotalAmount().setScale(2, BigDecimal.ROUND_HALF_UP));
            purchaseOrder.setPurchaserId(originalOrder.getPurchaserId());
            purchaseOrder.setOrderNo(originalOrder.getOrderNo());
            purchaseOrder.setCreateTime(originalOrder.getCreateTime());
            purchaseOrder.setIsDeleted(originalOrder.getIsDeleted());
            purchaseOrder.setExpectedDate(dto.getExpectedDate());
            purchaseOrder.setSupplierPhone(dto.getSupplierPhone());
            purchaseOrder.setRemark(dto.getRemark());
            purchaseOrder.setUpdateTime(LocalDateTime.now());

            List<PurchaseOrderItem> items = new ArrayList<>();
            if (dto.getItems() == null || dto.getItems().isEmpty()) {
                return Result.error("采购明细不能为空，请至少保留一条采购药品");
            }
            for (PurchaseOrderDTO.PurchaseOrderItemDTO itemDto : dto.getItems()) {
                if (itemDto.getDrugId() == null) {
                    return Result.error("采购明细药品ID(drugId)不能为空");
                }
                if (itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
                    return Result.error("采购明细数量必须大于0（药品ID：" + itemDto.getDrugId() + "）");
                }
                if (itemDto.getUnitPrice() == null || itemDto.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                    return Result.error("采购明细单价不能为空且不能为负数（药品ID：" + itemDto.getDrugId() + "）");
                }

                PurchaseOrderItem item = new PurchaseOrderItem();
                BeanUtils.copyProperties(itemDto, item);
                item.setPurchaseId(dto.getPurchaseId());
                item.setPurchaseQty(itemDto.getQuantity());
                BigDecimal unitPrice = itemDto.getUnitPrice().setScale(2, BigDecimal.ROUND_HALF_UP);
                item.setUnitPrice(unitPrice);
                item.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity())).setScale(2, BigDecimal.ROUND_HALF_UP));
                item.setBatchNo(itemDto.getBatchNo());
                items.add(item);
            }

            boolean success = purchaseOrderService.editPurchase(purchaseOrder, items);
            if (success) {
                return Result.success("修改采购单成功", purchaseOrder.getPurchaseId());
            } else {
                return Result.error("修改采购单失败：Service层执行返回false");
            }

        } catch (Exception e) {
            log.error("修改采购单失败（ID：{}）", dto.getPurchaseId(), e);
            return Result.error("修改失败：" + e.getMessage());
        }
    }

    // ==================== 5. 删除采购单 ====================
    @DeleteMapping("/order/{purchaseId}")
    public Result<?> deletePurchase(@PathVariable Long purchaseId) {
        try {
            boolean success = purchaseOrderService.deletePurchasePhysically(purchaseId);

            if (success) {
                log.info("物理删除采购单成功，采购单ID={}", purchaseId);
                return Result.success("删除成功");
            } else {
                log.warn("物理删除采购单失败：未找到该采购单，ID={}", purchaseId);
                return Result.error("删除失败：未找到该采购单");
            }
        } catch (Exception e) {
            log.error("物理删除采购单异常，采购单ID={}", purchaseId, e);
            return Result.error("删除失败：系统异常，请稍后重试");
        }
    }

    // ==================== 6. 审核/状态更新 ====================
    @PostMapping("/order/updateStatus")
    public Result<?> updatePurchaseStatus(@RequestBody Map<String, Object> params) {
        if (params == null) {
            return Result.error("请求参数不能为空");
        }
        String purchaseIdStr = params.getOrDefault("purchaseId", "").toString().trim();
        String orderStatusStr = params.getOrDefault("orderStatus", "").toString().trim();
        String auditTimeStr = params.getOrDefault("auditTime", "").toString().trim();

        if (purchaseIdStr.isEmpty()) return Result.error("采购单ID(purchaseId)不能为空");
        if (orderStatusStr.isEmpty()) return Result.error("状态值(orderStatus)不能为空");
        if (auditTimeStr.isEmpty()) return Result.error("操作时间(auditTime)不能为空");

        try {
            Long purchaseId = Long.parseLong(purchaseIdStr);
            Integer orderStatus = Integer.parseInt(orderStatusStr);
            String auditTime = auditTimeStr;
            String auditOpinion = params.get("auditOpinion") != null ? params.get("auditOpinion").toString().trim() : "";

            Map<String, Object> result = purchaseOrderService.updatePurchaseStatus(
                    purchaseId, orderStatus, auditTime, auditOpinion);

            if ((Boolean) result.get("success")) {
                return Result.success((String) result.get("message"));
            } else {
                return Result.error((String) result.get("message"));
            }
        } catch (NumberFormatException e) {
            log.error("参数格式错误", e);
            return Result.error("参数格式错误！ID、状态需为数字");
        } catch (Exception e) {
            log.error("状态更新异常", e);
            return Result.error("系统异常：" + e.getMessage());
        }
    }

    // ==================== ✅ 7. 新增：采购单导出接口（你要的） ====================
    // ==================== ✅ 7. 采购单导出接口（修复版） ====================
    @GetMapping("/order/export")
    public void exportPurchaseOrder(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String purchaserName,
            @RequestParam(required = false) String createTimeStart,
            @RequestParam(required = false) String createTimeEnd,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10000") Integer pageSize,
            HttpServletResponse response
    ) {
        try {
            // 1. 封装查询参数，复用分页查询逻辑
            Map<String, Object> params = new HashMap<>();
            params.put("orderNo", orderNo);
            params.put("supplierName", supplierName);
            params.put("purchaserName", purchaserName);
            params.put("createTimeStart", createTimeStart);
            params.put("createTimeEnd", createTimeEnd);
            params.put("pageNum", pageNum);
            params.put("pageSize", pageSize);

            // 2. 查询数据
            Page<PurchaseOrder> page = new Page<>(pageNum, pageSize);
            IPage<PurchaseOrder> resultPage = purchaseOrderService.queryPurchasePage(page, params);
            List<PurchaseOrder> list = resultPage.getRecords();

            // 3. 【核心修复】规范设置响应头，先设置头再写流
            response.reset();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            // 文件名兼容所有浏览器
            String fileName = "采购单列表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename=" + encodedFileName + ".xlsx;filename*=UTF-8''" + encodedFileName + ".xlsx");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

            // 4. 定义Excel表头和数据
            List<String> head = Arrays.asList(
                    "采购单ID", "采购单号", "采购专员", "供应商名称", "供应商联系人",
                    "供应商电话", "总金额(元)", "审核状态", "审核人", "审核时间", "创建时间", "备注"
            );

            List<List<String>> dataList = new ArrayList<>();
            for (PurchaseOrder po : list) {
                List<String> row = new ArrayList<>();
                row.add(po.getPurchaseId() == null ? "" : po.getPurchaseId().toString());
                row.add(po.getOrderNo() == null ? "" : po.getOrderNo());
                row.add(po.getPurchaserName() == null ? "" : po.getPurchaserName());
                row.add(po.getSupplierName() == null ? "" : po.getSupplierName());
                row.add(po.getSupplierContact() == null ? "" : po.getSupplierContact());
                row.add(po.getSupplierPhone() == null ? "" : po.getSupplierPhone());
                row.add(po.getTotalAmount() == null ? "0.00" : po.getTotalAmount().toString());
                row.add(getStatusText(po.getOrderStatus()));
                row.add(po.getAuditorName() == null ? "" : po.getAuditorName());
                row.add(po.getAuditTime() == null ? "" : po.getAuditTime().format(DATETIME_FORMATTER));
                row.add(po.getCreateTime() == null ? "" : po.getCreateTime().format(DATETIME_FORMATTER));
                row.add(po.getRemark() == null ? "" : po.getRemark());
                dataList.add(row);
            }

            // 5. 写入Excel输出流
            ExcelUtils.exportExcel(response.getOutputStream(), "采购单列表", head, dataList);
            log.info("采购单导出成功，导出条数：{}", list.size());

        } catch (Exception e) {
            log.error("采购单导出失败", e);
            // 异常时重置响应，返回错误信息
            try {
                response.reset();
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"导出失败：" + e.getMessage() + "\"}");
            } catch (Exception ex) {
                log.error("导出异常响应写入失败", ex);
            }
        }
    }

    private String getStatusText(Integer status) {
        Map<Integer, String> statusMap = new HashMap<>();
        statusMap.put(0, "待审核");
        statusMap.put(1, "已通过");
        statusMap.put(2, "已到货");
        statusMap.put(3, "已完成");
        return statusMap.getOrDefault(status, "未知状态");
    }

}