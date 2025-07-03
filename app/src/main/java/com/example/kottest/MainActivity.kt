package com.example.kottest

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.daggeruse.Computer
import com.example.daggeruse.ComputerModule
import com.example.daggeruse.DaggerComputerComponent
import com.example.daggeruse.network.ApiService
import com.example.daggeruse.network.UserDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

//    Dagger1111
//    @Inject
//    lateinit var computer: Computer

//    Dagger2222
    @Inject
    lateinit var retrofit: Retrofit
    @Inject
    lateinit var apiService: ApiService
    @Inject
    lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

//        Dagger1111
//        DaggerComputerComponent.create().injectActivity(this)
//        Log.d("11111Dagger-Main", computer.info())

//        Dagger2222
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 调用 API 方法
                val users = apiService.getUsers()
                Toast.makeText(this@MainActivity, "获取到的用户信息：${users.size}", Toast.LENGTH_SHORT).show()
                // 打印获取到的用户信息
                users.forEach { user ->
                    Log.d("Dagger2222 MainActivity", "User: ${user.name}")
                }

                // 插入用户信息到数据库
                userDao.insertAll(users)
            } catch (e: Exception) {
                // 处理异常
                Log.e("Dagger2222MainActivity", "Error fetching users", e)
            }
        }
    }
}