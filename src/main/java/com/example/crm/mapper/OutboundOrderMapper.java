package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.entity.OutboundOrder;
import com.example.crm.vo.AreaFlowVO;
import com.example.crm.vo.ChannelFlowVO;
import com.example.crm.vo.MonthFlowVO;
import com.example.crm.vo.RecentFlowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 出库单Mapper接口
 */
public interface OutboundOrderMapper extends BaseMapper<OutboundOrder> {
    // 新增：查询合作企业总数（首页统计用）
    @Select("SELECT COUNT(DISTINCT receiver_name) FROM outbound_order")
    Integer selectTotalEnterpriseNum();

    // 新增：查询待审核出库单数量（首页统计用）
    @Select("SELECT COUNT(*) FROM outbound_order WHERE audit_status = 0")
    Integer selectPendingAuditNum();

    // 新增：查询当月出库金额（首页统计用）
    @Select("SELECT SUM(ooi.actual_out_qty * db.purchase_price) " +
            "FROM outbound_order oo " +
            "LEFT JOIN outbound_order_item ooi ON oo.outbound_id = ooi.outbound_id " +
            "LEFT JOIN drug_batch db ON ooi.batch_id = db.batch_id " +
            "WHERE DATE_FORMAT(oo.outbound_time, '%Y-%m') = #{month}")
    Double selectMonthFlowAmount(@Param("month") String month);

    // 核心修改：查询最近出库流水列表（改别名+过滤null行）
    @Select("SELECT " +
            "oo.outbound_id as flowNo, " +          // 原flowId → flowNo（匹配前端）
            "d.drug_name as drugName, " +
            "oo.receiver_name as companyName, " +  // 原enterpriseName → companyName（匹配前端）
            "ooi.actual_out_qty as flowQty, " +
            "db.purchase_price as unitPrice, " +
            "(ooi.actual_out_qty * db.purchase_price) as amount, " +  // 原totalAmount → amount（匹配前端）
            "oo.outbound_time as createTime, " +   // 原flowTime → createTime（匹配前端）
            "CASE WHEN oo.audit_status = 0 THEN '待审核' ELSE '已通过' END as auditStatus " +
            "FROM outbound_order oo " +
            "LEFT JOIN outbound_order_item ooi ON oo.outbound_id = ooi.outbound_id " +
            "LEFT JOIN drug_batch db ON ooi.batch_id = db.batch_id " +
            "LEFT JOIN drug d ON db.drug_id = d.drug_id " +
            "WHERE d.drug_name IS NOT NULL " +    // 新增：过滤drugName为null的无效行
            "ORDER BY oo.outbound_time DESC " +
            "LIMIT 10")
    List<RecentFlowVO> selectRecentFlowList();

    // 区域流向统计
    @Select("SELECT " +
            "SUBSTRING(c.address, 1, 3) as areaName, " +
            "COUNT(DISTINCT oo.outbound_id) as orderNum, " +
            "SUM(ooi.actual_out_qty) as totalQty, " +
            "SUM(ooi.actual_out_qty * db.purchase_price) as totalAmount " +
            "FROM outbound_order oo " +
            "LEFT JOIN customer c ON oo.receiver_name = c.customer_name " +
            "LEFT JOIN outbound_order_item ooi ON oo.outbound_id = ooi.outbound_id " +
            "LEFT JOIN drug_batch db ON ooi.batch_id = db.batch_id " +
            "WHERE oo.audit_status = 1 " +
            "AND c.is_deleted = 0 " +
            "AND oo.is_deleted = 0 " +
            "AND (DATE_FORMAT(oo.outbound_time, '%Y') = #{year} OR #{year} IS NULL) " +
            "GROUP BY areaName " +
            "ORDER BY totalAmount DESC")
    List<AreaFlowVO> selectAreaFlow(@Param("year") String year);

    // 渠道流向统计
    @Select("SELECT " +
            "CASE c.type WHEN 1 THEN '药企' WHEN 2 THEN '药店' ELSE '其他' END as channelName, " +
            "c.type as channelType, " +
            "COUNT(DISTINCT oo.outbound_id) as orderNum, " +
            "SUM(ooi.actual_out_qty) as totalQty, " +
            "SUM(ooi.actual_out_qty * db.purchase_price) as totalAmount " +
            "FROM outbound_order oo " +
            "LEFT JOIN customer c ON oo.receiver_name = c.customer_name " +
            "LEFT JOIN outbound_order_item ooi ON oo.outbound_id = ooi.outbound_id " +
            "LEFT JOIN drug_batch db ON ooi.batch_id = db.batch_id " +
            "WHERE oo.audit_status = 1 " +
            "AND c.is_deleted = 0 " +
            "AND oo.is_deleted = 0 " +
            "AND (DATE_FORMAT(oo.outbound_time, '%Y') = #{year} OR #{year} IS NULL) " +
            "GROUP BY channelType, channelName")
    List<ChannelFlowVO> selectChannelFlow(@Param("year") String year);

    // 月度流向统计
    @Select("SELECT " +
            "DATE_FORMAT(oo.outbound_time, '%Y-%m') as month, " +
            "COUNT(DISTINCT oo.outbound_id) as orderNum, " +
            "SUM(ooi.actual_out_qty) as totalQty, " +
            "SUM(ooi.actual_out_qty * db.purchase_price) as totalAmount " +
            "FROM outbound_order oo " +
            "LEFT JOIN outbound_order_item ooi ON oo.outbound_id = ooi.outbound_id " +
            "LEFT JOIN drug_batch db ON ooi.batch_id = db.batch_id " +
            "WHERE oo.audit_status = 1 " +
            "AND oo.is_deleted = 0 " +
            "AND DATE_FORMAT(oo.outbound_time, '%Y') = #{year} " +
            "GROUP BY month " +
            "ORDER BY month ASC")
    List<MonthFlowVO> selectMonthFlow(@Param("year") String year);

    // 新增：分页查询出库单（多条件筛选）
    IPage<OutboundOrder> selectOutboundPage(
            Page<OutboundOrder> page,
            @Param("receiverName") String receiverName,
            @Param("auditStatus") Integer auditStatus,
            @Param("outboundTimeStart") String outboundTimeStart,
            @Param("outboundTimeEnd") String outboundTimeEnd
    );
}