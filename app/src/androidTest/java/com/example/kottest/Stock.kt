package com.example.kottest

class Stock {
    val prices:MutableList<Price?>? = null
    fun Stock(
        symbol: String,
        open: Double,
        high: Double,
        low: Double,
        close: Double
    ) {
    }

    fun process(){}
}

class Price {
    val open:Double? = null
}
