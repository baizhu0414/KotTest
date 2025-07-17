package com.example.kottest

import android.content.Intent
import android.content.pm.PackageManager
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
import com.example.daggeruse.network.DaggerNetworkDatabaseComponent
import com.example.daggeruse.network.UserDao
import com.example.kottest.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import javax.inject.Inject
import androidx.databinding.DataBindingUtil
import com.example.daggeruse.mvvmdagger.MainViewModel
import com.example.launcher.MyApplication

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

    // 声明布局绑定对象
    private lateinit var binding: ActivityMainBinding
    private val viewModel = MainViewModel(this@MainActivity)


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

        // 初始化Data Binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        // 将ViewModel实例设置到布局中(布局文件dagger_vm自动编程daggerVm)
        binding.daggerVm = viewModel
        // 设置生命周期所有者，确保LiveData能正常工作
        binding.lifecycleOwner = this


//        Dagger2222
//        从Application获取Dagger组件实例并调用其注入方法，这样apiService才能被正确初始化。否则Inject报错。
        MyApplication.instance.component.injectActivity(this)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 调用 API 方法
                val users = apiService.getUsers()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "获取到的用户信息：${users.size}", Toast.LENGTH_SHORT).show()
                }
                // 打印获取到的用户信息
                users.forEach { user ->
                    Log.d("Dagger2222 MainActivity", "User: ${user.name}")
                }

                // 插入用户信息到数据库
                val ids = userDao.insertAll(users)
                ids.forEach { id ->
                    if(id == -1L)
                        Log.e("Dagger2222MainActivity", "Insert failed")
                }
            } catch (e: Exception) {
                // 处理异常
                Log.e("Dagger2222MainActivity", "Error fetching users", e)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.onActivityResult(requestCode, resultCode) // 相机指定uri不用data了
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        viewModel.handlePermissionResult(requestCode, grantResults)
    }
}