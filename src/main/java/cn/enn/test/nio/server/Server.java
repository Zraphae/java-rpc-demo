package cn.enn.test.nio.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.SerializationUtils;

import cn.enn.test.nio.service.HelloService;
import cn.enn.test.nio.service.HelloServiceImpl;
import cn.enn.test.nio.thread.ThreadPool;
import cn.enn.test.nio.vo.MethodParam;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class Server {

	@Builder.Default
	private int timeout = 1000;
	@Builder.Default
	private int port = 8080;
	@Builder.Default
	private int bufferSize = 1024;
	@Builder.Default
	private HelloService service = new HelloServiceImpl();
	@Builder.Default
	private ExecutorService threadPool = ThreadPool.builder().build().newThreadPool();

	public void start() {

		log.info(String.format("server has started with params: port: %d, timeout: %d, buffersize: %d", this.port,
				this.timeout, this.bufferSize));
		ServerSocketChannel ssc = null;
		Selector selector = null;
		try {
			ssc = ServerSocketChannel.open();
			selector = Selector.open();
			ssc.configureBlocking(false);
			ssc.bind(new InetSocketAddress(port));
			ssc.register(selector, SelectionKey.OP_ACCEPT);

			while (selector.select() > 0) {

				Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

				while (iterator.hasNext()) {
					SelectionKey key = iterator.next();
					iterator.remove();

					this.threadPool.submit(new Runnable() {

						@Override
						public void run() {
							try {
								this.execute((ServerSocketChannel) key.channel());
							} catch (NoSuchMethodException | SecurityException | IllegalAccessException
									| IllegalArgumentException | InvocationTargetException e) {
								e.printStackTrace();
							}
						}

						private void execute(ServerSocketChannel ssc) throws NoSuchMethodException, SecurityException,
								IllegalAccessException, IllegalArgumentException, InvocationTargetException {
							SocketChannel sc = null;
							MethodParam methodParam = null;
							try {
								sc = ssc.accept();
								methodParam = receiveData(sc, MethodParam.class);
								log.info("receive data: " + methodParam);

								Method method = service.getClass().getMethod(methodParam.getMethodName(),
										methodParam.getParameterTypes());
								Object obj = method.invoke(service, methodParam.getArgs());
								sendData(sc, obj);
							} catch (IOException e) {
								e.printStackTrace();
							} finally {
								if (sc != null) {
									try {
										sc.close();
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							}

						}
					});

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (selector != null) {
				try {
					selector.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (ssc != null) {
				try {
					ssc.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void execute(ServerSocketChannel ssc) throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		SocketChannel sc = null;
		MethodParam methodParam = null;
		try {
			sc = ssc.accept();
			methodParam = receiveData(sc, MethodParam.class);
			log.info("receive data: " + methodParam);

			Method method = service.getClass().getMethod(methodParam.getMethodName(), methodParam.getParameterTypes());
			Object obj = method.invoke(this.service, methodParam.getArgs());
			sendData(sc, obj);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (sc != null) {
				try {
					sc.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private void sendData(SocketChannel sc, Object obj) throws IOException {
		byte[] array = SerializationUtils.serialize((Serializable) obj);
		ByteBuffer buffer = ByteBuffer.wrap(array);
		sc.write(buffer);

	}

	@SuppressWarnings("unchecked")
	private <T> T receiveData(SocketChannel sc, Class<T> clazz) throws IOException {
		T t = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteBuffer buffer = ByteBuffer.allocate(1024);

		byte[] bytes;
		int size = 0;
		while ((size = sc.read(buffer)) >= 0) {
			buffer.flip();
			bytes = new byte[size];
			buffer.get(bytes);
			baos.write(bytes);
			buffer.clear();
		}
		bytes = baos.toByteArray();
		t = (T) SerializationUtils.deserialize(bytes);
		return t;
	}

	public void writeHandle(SelectionKey key) throws IOException {
		ByteBuffer buffer = (ByteBuffer) key.attachment();
		buffer.flip();
		SocketChannel sc = (SocketChannel) key.channel();
		while (buffer.hasRemaining()) {
			sc.write(buffer);
		}
		buffer.compact();
	}

	public void acceptHandle(SelectionKey key) throws IOException {
		ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		SocketChannel sc = ssc.accept();
		sc.configureBlocking(false);
		sc.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocateDirect(bufferSize));
	}

	public void readHandle(SelectionKey key) {
		SocketChannel sc = (SocketChannel) key.channel();
		ByteBuffer buffer = (ByteBuffer) key.attachment();
		try {
			sc.read(buffer);
			buffer.flip();
			int limit = buffer.limit();
			byte[] arr = new byte[limit];
			buffer.get(arr, 0, limit);
			System.out.println(new String(arr));
			arr = null;
			buffer.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
