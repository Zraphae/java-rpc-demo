package cn.enn.test.nio.thread;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class ThreadPool {

	@Builder.Default
	private int corePoolSize = Runtime.getRuntime().availableProcessors();
	@Builder.Default
	private int maximumPoolSize = Runtime.getRuntime().availableProcessors() * 10;
	@Builder.Default
	private long keepAliveTime = 1000;
	@Builder.Default
	private TimeUnit unit = TimeUnit.MILLISECONDS;
	@Builder.Default
	private BlockingQueue<Runnable> workQueue = new SynchronousQueue<>();
	@Builder.Default
	private RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();

	public ExecutorService newThreadPool() {

		log.info(String.format(
				"create threadPoolExecutor finished, params corePoolSize:%d, maximumPoolSize:%d, keepAliveTime:%d, ",
				corePoolSize, maximumPoolSize, keepAliveTime));
		return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
	}

}
