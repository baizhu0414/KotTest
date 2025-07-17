package com.example.component

import android.os.Process
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author zhuangjinkun@bytedance.com
 * @Description: 启动专用，其它业务请不要使用
 * @date 2021/11/9
 */
object StartExecutorService {
    private val CPU_COUNT = Runtime.getRuntime().availableProcessors()

    private const val THREAD_POOL_IO = "start-tp-io"
    private const val THREAD_POOL_DEFAULT = "start-tp-default"
    private const val THREAD_POOL_BACKGROUND = "start-tp-background"
    private const val THREAD_POOL_FIXED = "start-tp-fixed"
    private const val THREAD_POOL_IO_REJECT = "start-vtp-reject"
    private const val THREAD_POOL_DEFAULT_REJECT = "start-tp-default-reject"

    private val sThreadCount = AtomicInteger(0)

    private val S_CORE_NUM = 1
    private val DEFAULT_CORE_POOL_SIZE = CPU_COUNT
    private val DEFAULT_MAXIMUM_POOL_SIZE = DEFAULT_CORE_POOL_SIZE * 2 + 1

    private const val IO_MAXIMUM_POOL_SIZE = 128

    private const val DEFAULT_TASK_QUEUE_SIZE = 128
    private const val BACKGROUND_TASK_QUEUE_SIZE = 128

    private const val KEEP_ALIVE_SECONDS = 30L
    private const val FIXED_KEEP_ALIVE_SECONDS = 30L
    private const val BACKGROUND_KEEP_ALIVE_SECONDS = 30L

    private val REJECT_DEFAULT_CORE_POOL_SIZE = CPU_COUNT + 1
    private val REJECT_IO_CORE_POOL_SIZE = 2 * 2.coerceAtLeast((CPU_COUNT - 1).coerceAtMost(6))

    private var executors: ExecutorService? = null

    // 普通任务
    var NormalTurboExecutors : ExecutorService? = null
    // 高优任务
    var HighTurboExecutors : ExecutorService? = null
    // 低优任务
    var LowTurboExecutors : ExecutorService? = null

    fun execute(task: Runnable) {
        if (executors == null) {
            executors = if (HighTurboExecutors != null) {
                HighTurboExecutors
            } else {
                Executors.newFixedThreadPool(DEFAULT_CORE_POOL_SIZE, StartThreadFactory())
            }
        }
        executors!!.execute(task)
    }

    /**
     * 正常执行异步任务线程
     */
    val executorWork: ExecutorService by lazy {
        createDefaultExecutor()
    }

    val ioExecutorWork: ExecutorService by lazy {
        createIOExecutor()
    }

    /**
     * 执行长耗时异步任务，放低线程优先级，防止抢占主线程
     */
    val bgExecutorWork: ExecutorService by lazy {
        createBackgroundExecutor()
    }

    /**
     * 单独线程池，同 executorWork 隔开，不可执行task
     */
    val singleExecutorWork: ExecutorService by lazy {
        createFixedExecutorInner(4)
    }

    /**
     * 单线程线程池，同 executorWork 隔开，不可执行task
     */
    val loadSOExecutor: ExecutorService by lazy {
        createSoLoadExecutor()
    }

    /**
     * 单线程线程池，同 executorWork 隔开，不可执行task
     */
    val settingsExecutor: ExecutorService by lazy {
        createSerialExecutor()
    }

    @Volatile
    private var sSerialExecutor: ExecutorService? = null
    private fun createSerialExecutor(): ExecutorService {
        if (sSerialExecutor == null) {
            synchronized(StartExecutorService::class.java) {
                if (sSerialExecutor == null) {
                    sSerialExecutor = createSettingsLoadExecutorInner()
                }
            }
        }
        return sSerialExecutor!!
    }

    private fun createSettingsLoadExecutorInner(): ExecutorService {
        if (NormalTurboExecutors != null) {
            return NormalTurboExecutors!!
        }
        return ThreadPoolExecutor(
            S_CORE_NUM,
            S_CORE_NUM,
            KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(),
            threadFactory(
                "settings-load",
                false,
                Process.THREAD_PRIORITY_DEFAULT
            ),
            ThreadPoolExecutor.AbortPolicy()
        )
    }

    @Volatile
    private var sSOLoadExecutor: ExecutorService? = null
    @Suppress("UnsafeCallOnNullableType")
    private fun createSoLoadExecutor(): ExecutorService {
        if (sSOLoadExecutor == null) {
            synchronized(StartExecutorService::class.java) {
                if (sSOLoadExecutor == null) {
                    sSOLoadExecutor = createSoLoadExecutorInner()
                }
            }
        }
        @Suppress("UnsafeCallOnNullableType")
        return sSOLoadExecutor!!
    }

    private fun createSoLoadExecutorInner(): ExecutorService {
        if (NormalTurboExecutors != null) {
            return NormalTurboExecutors!!
        }
        return ThreadPoolExecutor(
            S_CORE_NUM,
            S_CORE_NUM,
            KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            PriorityBlockingQueue(),
            threadFactory(
                "load-so-serial",
                false,
                Process.THREAD_PRIORITY_DEFAULT
            ),
            ThreadPoolExecutor.AbortPolicy()
        )
    }

    private val sIORejectExecutor: ExecutorService = createRejectHandlerExecutor(
        REJECT_IO_CORE_POOL_SIZE,
        THREAD_POOL_IO_REJECT
    )

    private val sDefaultRejectExecutor: ExecutorService = createRejectHandlerExecutor(
        REJECT_DEFAULT_CORE_POOL_SIZE,
        THREAD_POOL_DEFAULT_REJECT
    )

    private var sLastIORejectTime: Long = 0
    private var sLastDefaultRejectTime: Long = 0
    private val FREQ_CONTROL = TimeUnit.SECONDS.toMillis(3)

    private val sIORejectHandler =
        RejectedExecutionHandler { r, _ ->
            sIORejectExecutor.execute(r)
            val curTime: Long = SystemClock.elapsedRealtime()
            sLastIORejectTime = if (curTime - sLastIORejectTime >= FREQ_CONTROL) {
                curTime
            } else {
                curTime
            }
        }

    private val sDefaultRejectHandler =
        RejectedExecutionHandler { r, _ ->
            sDefaultRejectExecutor.execute(r)
            val curTime: Long = SystemClock.elapsedRealtime()
            sLastDefaultRejectTime = if (curTime - sLastDefaultRejectTime >= FREQ_CONTROL) {
                curTime
            } else {
                curTime
            }
        }

    @Volatile
    private var sDefaultExecutor: ExecutorService? = null
    private fun createDefaultExecutor(): ExecutorService {
        if (sDefaultExecutor == null) {
            synchronized(StartExecutorService::class.java) {
                if (sDefaultExecutor == null) {
                    sDefaultExecutor = if (NormalTurboExecutors != null) {
                        NormalTurboExecutors
                    } else {
                        createDefaultExecutorInner()
                    }
                }
            }
        }
        return sDefaultExecutor!!
    }

    @Volatile
    private var sBackgroundExecutor: ExecutorService? = null
    private fun createBackgroundExecutor(): ExecutorService {
        if (sBackgroundExecutor == null) {
            synchronized(StartExecutorService::class.java) {
                if (sBackgroundExecutor == null) {
                    sBackgroundExecutor = if (LowTurboExecutors != null) {
                        LowTurboExecutors
                    } else {
                        createBackgroundExecutorInner()
                    }
                }
            }
        }
        return sBackgroundExecutor!!
    }

    @Volatile
    private var sIOExecutor: ExecutorService? = null
    private fun createIOExecutor(): ExecutorService {
        if (sIOExecutor == null) {
            synchronized(StartExecutorService::class.java) {
                if (sIOExecutor == null) {
                    sIOExecutor = if (LowTurboExecutors != null) {
                        LowTurboExecutors
                    } else {
                        val executor = ThreadPoolExecutor(
                            0,
                            IO_MAXIMUM_POOL_SIZE,
                            KEEP_ALIVE_SECONDS,
                            TimeUnit.SECONDS,
                            SynchronousQueue(),
                            threadFactory(
                                THREAD_POOL_IO,
                                false,
                                Process.THREAD_PRIORITY_BACKGROUND
                            ),
                            sIORejectHandler
                        )
                        executor.allowCoreThreadTimeOut(true)
                        executor
                    }
                }
            }
        }
        return sIOExecutor!!
    }

    private fun createRejectHandlerExecutor(
        nThreads: Int,
        threadName: String
    ): ExecutorService {
        val executor = ThreadPoolExecutor(
            nThreads,
            nThreads,
            KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(),
            threadFactory(threadName, false, Process.THREAD_PRIORITY_DEFAULT)
        )
        executor.allowCoreThreadTimeOut(true)
        return executor
    }

    private fun createDefaultExecutorInner(): ExecutorService {
        val executor = ThreadPoolExecutor(
            DEFAULT_CORE_POOL_SIZE,
            DEFAULT_MAXIMUM_POOL_SIZE,
            KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(DEFAULT_TASK_QUEUE_SIZE),
            threadFactory(THREAD_POOL_DEFAULT, false, Process.THREAD_PRIORITY_DEFAULT),
            sDefaultRejectHandler
        )
        executor.allowCoreThreadTimeOut(true)
        return executor
    }

    private fun createBackgroundExecutorInner(): ExecutorService {
        val executor = ThreadPoolExecutor(
            1,
            1,
            BACKGROUND_KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(BACKGROUND_TASK_QUEUE_SIZE),
            threadFactory(THREAD_POOL_BACKGROUND, true, Process.THREAD_PRIORITY_BACKGROUND),
            ThreadPoolExecutor.AbortPolicy()
        )
        executor.allowCoreThreadTimeOut(true)
        return executor
    }

    private fun createFixedExecutorInner(nThread: Int): ExecutorService {
        if (NormalTurboExecutors != null) {
            return NormalTurboExecutors!!
        }
        val executor = ThreadPoolExecutor(
            nThread,
            nThread,
            FIXED_KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(),
            threadFactory(
                THREAD_POOL_FIXED,
                false,
                Process.THREAD_PRIORITY_DEFAULT
            ),
            ThreadPoolExecutor.AbortPolicy()
        )
        executor.allowCoreThreadTimeOut(true)
        return executor
    }

    private fun threadFactory(
        threadName: String,
        isDaemon: Boolean,
        priority: Int
    ): ThreadFactory {
        return object : ThreadFactory {
            override fun newThread(r: Runnable): Thread {
                val thread = Thread {
                    setThreadPriority(priority)
                    r.run()
                }
                val name = threadName + "-" + sThreadCount.incrementAndGet()
                thread.name = name
                thread.isDaemon = isDaemon
                return thread
            }
        }
    }

    private fun setThreadPriority(priority: Int) {
        try {
            Process.setThreadPriority(priority)
        } catch (ignore: Throwable) {
            Log.e("StartExecutorService", ignore.toString())
        }
    }
}
