package com.library.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartData implements Serializable {

    private String name;
    private Object value;
    private String type;

    public static ChartData of(String name, Object value) {
        return ChartData.builder()
                .name(name)
                .value(value)
                .build();
    }

    public static ChartData of(String name, Object value, String type) {
        return ChartData.builder()
                .name(name)
                .value(value)
                .type(type)
                .build();
    }
}
