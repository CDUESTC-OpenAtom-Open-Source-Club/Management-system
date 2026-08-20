package com.openatom.club.point.dto;

import lombok.Data;
import java.util.List;

@Data
public class PointTableResult {
    private List<PointTableRowDto> rows;
    private int page;
    private int size;
    private long total;
}
