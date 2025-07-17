package com.example.daggeruse.mvvmdagger

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.daggeruse.network.ApiService
import com.example.daggeruse.network.UserDao
import com.example.kottest.MainActivity
import com.example.kottest.R
import com.example.provider.MyFileProvider
import com.example.provider.ProviderInstallProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException


/**
 * 用于管理页面数据。
 */
class MainViewModel(private val ctx: Activity) : ViewModel(), PermissionCallback {
    // LiveData用于存储UI状态
    val _buttonText = MutableLiveData<String>("点击按钮MVVM")
    val buttonText: LiveData<String> = _buttonText

    val imageUri = MutableLiveData<Uri?>(null)
    // 保存拍照时创建的文件及其Uri
    private var tempImageFile: File? = null
    private var tempImageUri: Uri? = null

    val CAMERA_REQUEST_CODE = 1

    private var permissionCallback: PermissionCallback? = null

    // 处理按钮点击事件
    fun onButtonClick(view: View) {
        when (view.id) {
            R.id.btn_dagger -> {
                _buttonText.value = "Dagger+MVVM改变内容"
            }
            R.id.btn_share -> {
                //在外部存储中创建my_external文件夹,保证其存在
//                val imgDir = File(Environment.getExternalStorageDirectory(), "my_external")
                if (hasStoragePermission(ctx) && hasPicturePermission(ctx) && hasCameraPermission(ctx)) {
                    // 应用专属外存，Android10后不用申请权限
                    val storageDir =
                        ctx.getExternalFilesDir("${Environment.DIRECTORY_PICTURES}${File.separator}my_external")
                    if (storageDir == null || !storageDir.exists()) {
                        Log.e("StorageCheck", "存储目录不可用：$storageDir")
                        return
                    }
                    storageDir.exists().let {
                        if (!it) {
                            storageDir.mkdirs()
                            Log.i(ProviderInstallProxy.TAG, "创建文件夹：" + storageDir.absolutePath)
                        }
                    }
                    try {
                        tempImageFile = File.createTempFile(
                            "tmp_camera_capture",
                            ".jpg",
                            storageDir
                        )
                    } catch (e: IOException) {
                        Log.e("FileCheck", "文件创建失败：${e.message}")
                        return
                    }
                    tempImageFile?.let {
                        tempImageUri = MyFileProvider.getUriForFile(ctx, tempImageFile!!)
                        cameraPermission(this)
                        Log.i(ProviderInstallProxy.TAG, "创建文件：" + it.absolutePath)
                    }
                } else {
                    // 如果没有权限，就请求权限(此处逻辑并为完善，低版本手机需要完善后面的申请完成创建文件逻辑)
                    ActivityCompat.requestPermissions(
                        ctx,
                        arrayOf(Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_MEDIA_IMAGES),
                        1111
                    )
//                    ActivityCompat.requestPermissions(
//                        ctx,
//                        arrayOf(),
//                        1112
//                    )
//                    ActivityCompat.requestPermissions(
//                        ctx, arrayOf(),
//                        1113
//                    )
                }
            }
        }
    }

    fun cameraPermission(callback: PermissionCallback) {
        permissionCallback = callback
        permissionCallback?.onPermissionGranted(1111)
    }

    // 处理权限结果
    fun handlePermissionResult(requestCode: Int, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            permissionCallback?.onPermissionGranted(requestCode)
        } else {
            permissionCallback?.onPermissionDenied(requestCode)
        }
    }


    /**
     * 在 Android 10 以下，需要额外的WRITE_EXTERNAL_STORAGE权限。
     */
    private fun hasStoragePermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true // Android 10+ 使用Scoped Storage，无需此权限
        } else {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasPicturePermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true // Android 13+ 需要申请 READ_MEDIA_IMAGES
        } else {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasCameraPermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startCamera() {
        if (tempImageUri == null) {
            Log.e("CameraCheck", "文件Uri为空，无法启动相机")
            return
        }
        Intent().apply {
            action = MediaStore.ACTION_IMAGE_CAPTURE
            putExtra(MediaStore.EXTRA_OUTPUT, tempImageUri)
            // 获取相机应用的包名
            val resolveInfo = ctx.packageManager.resolveActivity(this, 0)
            resolveInfo?.activityInfo?.packageName?.let { packageName ->
                // 显式授予该应用读写权限
                ctx.grantUriPermission(
                    packageName,
                    tempImageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                Log.i(ProviderInstallProxy.TAG, "已授权相机应用: $packageName")
            }
            ctx.startActivityForResult(this, CAMERA_REQUEST_CODE)
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int): Unit {
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // 验证文件是否存在且有内容
            if (tempImageFile?.exists() == true && tempImageFile?.length()!! > 0) {
                Log.i("ImageCheck", "文件有效，大小：${tempImageFile?.length()}")
                imageUri.value = tempImageUri // 仅在文件有效时更新

                if(isWeChatInstalled(ctx)) {
                    // 分享到微信
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg" // 根据文件类型设置 MIME 类型
                        putExtra(Intent.EXTRA_STREAM, tempImageUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // 授予微信临时访问权限
                        setPackage("com.tencent.mm") // 直接指定微信包名，避免选择器
                    }
                    // 启动分享
                    ctx.startActivity(shareIntent)
                } else {
                    Log.w("ImageCheck", "未安装微信")
                }
            } else {
                Log.e("ImageCheck", "文件不存在或为空！")
            }
            Log.i(ProviderInstallProxy.TAG, "授权字符串：" + tempImageUri.toString())
        }
    }

    override fun onPermissionGranted(code: Int) {
        if (code == 1111) {
            startCamera()
        }
    }

    override fun onPermissionDenied(code: Int) {
        TODO("Not yet implemented")
    }

    private fun isWeChatInstalled(context: Context): Boolean {
        return try {
            // 仅获取基础包信息（无需获取Activity列表）
            context.packageManager.getPackageInfo(
                "com.tencent.mm",
                0 // 不需要额外标志，减少系统限制
            )
            true // 微信已安装
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(ProviderInstallProxy.TAG, "未安装微信"+e)
            false // 微信未安装或包可见性受限
        } catch (e: Exception) {
            // 捕获其他异常（如系统限制）
            Log.e(ProviderInstallProxy.TAG, "检查微信安装状态失败"+e)
            false
        }
    }

}

// ViewModel 中定义权限请求接口
interface PermissionCallback {
    fun onPermissionGranted(code: Int)
    fun onPermissionDenied(code: Int)
}