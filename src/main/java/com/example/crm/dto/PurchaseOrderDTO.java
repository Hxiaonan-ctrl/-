package com.example.crm.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购单新增/编辑DTO（严格匹配实体类+数据库表+前端请求体）
 */
@Data
public class PurchaseOrderDTO {
    // 主表字段
    private Long purchaseId; // 新增时为空，编辑时传值
    private String orderNo;
    private Long purchaserId; // 采购专员ID（必填）
    private String purchaserName; // 非数据库字段，仅前端展示

    // 【新增】供应商手机号（匹配请求体的supplierPhone）
    private String supplierPhone;
    // 【新增】备注（匹配请求体的remark）
    private String remark;
    // 【新增】预计到货日期（匹配请求体的expectedDate）
    private LocalDate expectedDate;

    private String supplierName;
    private String supplierContact;
    private String auditOpinion;
    private BigDecimal totalAmount; // 保持BigDecimal，避免浮点精度丢失
    private Integer orderStatus; // 匹配请求体的orderStatus（0-待审核）

    // 明细列表
    private List<PurchaseOrderItemDTO> items;

    // 明细DTO内部类
    @Data
    public static class PurchaseOrderItemDTO {
        // 【修改】drugId改为Long（匹配实体类Long/数据库bigint，避免类型转换异常）
        private Long drugId;
        private String drugName;
        private String specification;
        private String batchNo;
        private Integer quantity; // 对应实体类的purchaseQty
        // 【修改】unitPrice改为BigDecimal（匹配实体类，避免Double精度丢失）
        private BigDecimal unitPrice;
    }
}