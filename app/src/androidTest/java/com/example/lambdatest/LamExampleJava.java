package com.example.lambdatest;

import java.sql.Array;
import java.util.Arrays;

public class LamExampleJava {
    LamExample example = new LamExample();

    public void testLamExample() {
        // Array类型
        example.read(new Integer[]{1, 2, 3}, 1,2);
        // 数组类型(JvmOverloads会生成多个方法)
        example.read(new Integer[]{1, 2, 3});
    }
}
