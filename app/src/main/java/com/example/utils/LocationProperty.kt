package com.vega.core.context

class LocationProperty(var loc: String, val level: Int, val name: String)

val developer_usingLocation = LocationProperty("", 3, "develop")

val cc4b_createMode_usingLocation = LocationProperty("US", 1, "cc4b_create_mode")

val cc4b_betaAccess_usingLocation = LocationProperty("US", 2, "cc4b_beta_access")
