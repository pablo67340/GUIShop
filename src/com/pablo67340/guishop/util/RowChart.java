package com.pablo67340.guishop.util;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RowChart {

    @Getter
    public static final List<List<Integer>> rowChart = new ArrayList<>();

    public RowChart() {
        List<Integer> row1, row2, row3, row4, row5, row6;
        row1 = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));
        row2 = new ArrayList<>(Arrays.asList(9, 10, 11, 12, 13, 14, 15, 16, 17));
        row3 = new ArrayList<>(Arrays.asList(18, 19, 20, 21, 22, 23, 24, 25, 26));
        row4 = new ArrayList<>(Arrays.asList(27, 28, 29, 30, 31, 32, 33, 34, 35));
        row5 = new ArrayList<>(Arrays.asList(36, 37, 38, 39, 40, 41, 42, 43, 44));
        row6 = new ArrayList<>(Arrays.asList(45, 46, 47, 48, 49, 50, 51, 52, 53));
        rowChart.addAll(Arrays.asList(row1, row2, row3, row4, row5, row6));
    }

    public int getRowsFromHighestSlot(int slot) {
        for (List<Integer> row : rowChart) {
            for (int slotNum : row) {
                if (slotNum == slot) {
                    return rowChart.indexOf(row) + 1;
                }
            }
        }
        return 6;
    }
}
