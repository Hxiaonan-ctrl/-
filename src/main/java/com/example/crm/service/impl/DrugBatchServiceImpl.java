package com.example.crm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.crm.entity.DrugBatch;
import com.example.crm.mapper.DrugBatchMapper;
import com.example.crm.service.DrugBatchService;
import org.springframework.stereotype.Service;

@Service
public class DrugBatchServiceImpl extends ServiceImpl<DrugBatchMapper, DrugBatch> implements DrugBatchService {
}