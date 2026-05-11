package com.example.crm.vo;

import lombok.Data;
import java.util.Date;

/**
 * 单据VO（前端选择弹窗用）
 */
@Data
public class DocumentVO {
    /** 单据ID */
    private Long documentId;
    /** 单据类型（采购单/入库单/出库单） */
    private String documentType;
    /** 创建时间 */
    private Date createTime;
    /** 单据状态（可选） */
    private String status;
}