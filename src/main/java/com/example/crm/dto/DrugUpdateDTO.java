package com.example.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

/**
 * 修改药品DTO
 */
@Data
public class DrugUpdateDTO {
    @NotNull(message = "药品ID不能为空")
    private Long drugId;

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

    @NotNull(message = "库存预警值不能为空")
    private Integer stockWarning;

    // 效期预警天数
    private Integer expiryWarningDays;

    // 当前库存（编辑时同步更新批次库存）
    private Integer currentStock;
}