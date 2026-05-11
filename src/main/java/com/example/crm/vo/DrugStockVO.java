package com.example.crm.vo;

import lombok.Data;
import java.util.Date;

@Data
public class DrugStockVO {
    // 原有属性（包含你之前缺失的approvalNo，已补充）
    private Long drugId;
    private String drugName;
    private String approvalNo; // 补充的审批号字段
    private String specification;
    private String manufacturer;
    private String drugCategory;
    private String batchNo;
    private Integer currentStock;
    private Integer stockWarning;
    private Date expiryDate;
    private Integer expiryWarningDays;
    private Date createTime;
    private Integer stockWarningFlag; // 1=预警，0=非预警
    private Integer expiryWarningFlag; // 1=预警，0=非预警

    // 库存预警布尔判断方法（规范写法，无格式错误）
    public boolean isStockWarning() {
        // 1为Integer常量，equals避免null空指针，所有符号为英文半角
        return Integer.valueOf(1).equals(this.stockWarningFlag);
    }

    // 效期预警布尔判断方法（规范写法，无格式错误）
    public boolean isExpiryWarning() {
        return Integer.valueOf(1).equals(this.expiryWarningFlag);
    }
}