package com.example.kottest;

import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class JavCls {
    @Test
    public void test() {
        KotCls ktCls = new KotCls();
        List<Integer> list = new ArrayList<>();
        list.add(1);
        Log.d("JavCls", "Java调用Kot方法");
        ktCls.procMutList(list); // Java List默认是Mutable的
        // 反之，Kot调用java不会有限制，因为Java全是Mutable的。
        // 最佳实践：Kot分类型，Java统一使用List（不要用ArrayList ）
    }
}
