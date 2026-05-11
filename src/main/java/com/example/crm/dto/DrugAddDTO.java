package com.example.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

/**
 * 新增药品DTO
 */
@Data
public class DrugAddDTO {
    @NotBlank(message = "药品名称不能为空")
    private String drugName;

    @NotBlank(message = "批准文号不能为空")
    private String approvalNo;

    @NotBlank(message = "药品规格不能为空")
    private String specification;

    @NotBlank(message = "生产厂家不能为空")
    private String manufacturer;

    @NotBlank(message = "药品分类不能为空")
    private String drugCategory;

    @NotBlank(message = "批次号不能为空")
    private String batchNo;

    @NotNull(message = "有效期至不能为空")
    private LocalDate expiryDate;

    @NotNull(message = "初始库存不能为空")
    private Integer initialStock;

    @NotNull(message = "库存预警值不能为空")
    private Integer stockWarning;

    // 效期预警天数（新增，可选，默认30天）
    private Integer expiryWarningDays = 30;

    // 生产日期（可选，默认当前日期）
    private LocalDate productionDate;
}