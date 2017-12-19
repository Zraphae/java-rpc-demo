package cn.enn.test.nio;

import java.util.concurrent.ExecutorService;

import cn.enn.test.nio.client.Client;
import cn.enn.test.nio.service.HelloService;
import cn.enn.test.nio.thread.ThreadPool;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestClient {

	public static void main(String[] args) {

		ExecutorService pool = ThreadPool.builder().build().newThreadPool();

		for (int i = 0; i < 10000; i++) {
			pool.submit(new Runnable() {

				@Override
				public void run() {
					HelloService service = Client.builder().ip("127.0.0.1").port(8090).build()
							.refer(HelloService.class);
					String result = service.hello(Thread.currentThread().getName());
					log.info("result: " + result);

				}
			});
		}

		pool.shutdown();
	}

}
