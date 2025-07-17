package com.example.component

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author zhuangjinkun@bytedance.com
 * @Description:
 * @date 2021/10/20
 */
internal class StartThreadFactory : ThreadFactory {
    val poolNumber = AtomicInteger(1)
    var group: ThreadGroup? = null
    val threadNumber = AtomicInteger(1)
    var namePrefix: String? = null

    init {
        val s = System.getSecurityManager()
        group = if (s != null) s.threadGroup else Thread.currentThread().threadGroup
        namePrefix = "start-pool-" + poolNumber.getAndIncrement() + "-thread-"
    }

    override fun newThread(r: Runnable): Thread {
        val t = Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0)
        if (t.isDaemon) t.isDaemon = false
        if (t.priority != Thread.MAX_PRIORITY) t.priority = Thread.MAX_PRIORITY
        return t
    }
}
