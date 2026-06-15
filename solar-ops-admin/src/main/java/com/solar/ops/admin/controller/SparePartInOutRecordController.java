package com.solar.ops.admin.controller;

import com.alibaba.excel.EasyExcel;
import com.solar.ops.admin.dto.SparePartInOutRecordQueryDTO;
import com.solar.ops.admin.excel.SparePartInOutRecordExcelVO;
import com.solar.ops.admin.service.SparePartInOutRecordService;
import com.solar.ops.admin.vo.SparePartInOutRecordVO;
import com.solar.ops.common.page.PageQuery;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/spare-part-records")
@Api(tags = "备件出入库记录")
public class SparePartInOutRecordController {

    @Resource
    private SparePartInOutRecordService recordService;

    @GetMapping
    @ApiOperation(value = "分页查询出入库记录")
    public Result<PageResult<SparePartInOutRecordVO>> page(@ApiParam(value = "分页参数") PageQuery pageQuery,
                                                              @ApiParam(value = "查询条件") SparePartInOutRecordQueryDTO queryDTO) {
        PageResult<SparePartInOutRecordVO> pageResult = recordService.page(pageQuery, queryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "根据ID查询记录详情")
    public Result<SparePartInOutRecordVO> getById(@ApiParam(value = "记录ID") @PathVariable Long id) {
        SparePartInOutRecordVO detailVO = recordService.getDetailById(id);
        return Result.success(detailVO);
    }

    @GetMapping("/export")
    @ApiOperation(value = "导出入出库记录")
    public void export(@ApiParam(value = "查询条件") SparePartInOutRecordQueryDTO queryDTO,
                       HttpServletResponse response) throws IOException {
        List<SparePartInOutRecordExcelVO> list = recordService.exportList(queryDTO);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("备件出入库记录", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), SparePartInOutRecordExcelVO.class)
                .sheet("出入库记录")
                .doWrite(list);
    }
}
