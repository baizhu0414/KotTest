package com.example.daggeruse.network

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Database类绑定了Entity和Dao类。Entity定义表结构。Dao用于访问数据库。
 */
@Database(
    entities = [UserEntity::class],  // 实体类列表
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao  // DAO 接口声明
}

// 实体类定义
@Entity(tableName = "user_msg")
data class UserEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val email: String
)

// DAO 接口
@Dao
interface UserDao {
    /**
     * 向用户表中插入一条用户记录。
     * 若插入的数据主键与表中已有记录重复，会用新数据替换旧数据。
     *
     * suspend 关键字表明这是一个挂起函数，意味着该函数可以在协程中被异步调用，不会阻塞当前线程。
     *
     * @param user 要插入的用户记录实体对象。
     * @return 插入记录的行 ID，若插入失败则返回 -1。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(users: List<UserEntity>): List<Long>

    @Query("SELECT * FROM user_msg")
    fun getAll(): List<UserEntity> // Flow 实时监听
}
