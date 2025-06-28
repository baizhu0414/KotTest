package com.example.kottest

class KotCls {
    fun procMutList(mutableList: MutableList<Int>) {
        println("kt 处理可变LIst")
    }

    fun OpitonalFun() {
        // 空检查 Optional
        val nullableVal:Int? = null
//        val notNullVal:Int = null
        val mutList:MutableList<Int?> = mutableListOf(1,2,3)
        mutList.add(null)

        // 连续空检查
        val stockResponse = StockResponse()
        val prices = stockResponse.stock?.prices
        // 安全调用
        prices?.forEach{
            val price = it
            val openPrice = price?.open
            val intOpenPrice = openPrice?.toInt() ?:0 // null判断
        }

        /*Optional 为null的情况下不执行语句*/
        // !! 强行将Optional转为非Optional类型，不建议使用！
//        stockResponse.stock?.process()
//        stockResponse.stock = null

//        此方法不好
//        if(stockResponse.stock != null){
//            stockResponse.stock!!.process()
//            stockResponse.stock = null
//        }

        //使用非空值
        stockResponse.stock?.let {
            val prices = it.prices // Response.stock不为null时执行
        }


        /*Optional 为null的情况下给出兜底值*/
        getStockPrice()


    }

    val stock:Stock? = null
    fun getStockPrice():Double {
        return stock?.prices?.get(0)?.open ?: 0.0
    }
}