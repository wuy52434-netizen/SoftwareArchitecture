package com.library.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {

    private Long totalBooks;
    private Long availableBooks;
    private Long totalUsers;
    private Long totalBorrows;
    private Long activeBorrows;
    private Long overdueCount;
    private Long todayBorrows;
    private Long todayReturns;

    private List<ChartData> borrowTrend;
    private List<ChartData> categoryDistribution;
    private List<ChartData> userTypeDistribution;
    private List<ChartData> popularBooks;
    private List<ChartData> hourlyDistribution;
    private Map<String, Long> recentActivity;
}
