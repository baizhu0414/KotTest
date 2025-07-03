# Kotlin基础知识学习
## 模块划分
1. 基础语法只是：
	- 代码在`src\androidTest\java\com\example` 目录下
	- 知识点根据文件夹划分
		- kottest 基础语法
		- ktclstest 类使用(重点，待补充Android重点类)
		- lambdatest Lambda表达式基本使用（待完善）
		- candytest 语法糖（待完善）
		
2. Dagger+Retrofit+Room使用Kotlin测试
	- 代码在`src\main\java\com\example`目录下
	- Dagger2主要划分Module（Provider）-Component(桥梁)-Client(Inject)

3. 此项目要求：
```kotlin
minSdk = 24
compileSdk = 35
jdk11
```
