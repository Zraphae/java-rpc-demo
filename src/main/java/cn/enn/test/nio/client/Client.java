package cn.enn.test.nio.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import org.apache.commons.lang3.SerializationUtils;

import cn.enn.test.nio.vo.MethodParam;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class Client {

	@Builder.Default
	private String ip = "127.0.0.1";
	@Builder.Default
	private int port = 8090;

	@SuppressWarnings("unchecked")
	public <T> T refer(Class<T> clazz) {

		return (T) Proxy.newProxyInstance(clazz.getClassLoader(), (Class<?>[]) Arrays.asList(clazz).toArray(),
				new InvocationHandler() {

					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

						String methodName = method.getName();
						Class<?>[] parameterTypes = method.getParameterTypes();
						MethodParam methodParam = MethodParam.builder().methodName(methodName)
								.parameterTypes(parameterTypes).args(args).build();

						SocketChannel sc = null;
						Object receiveData = null;
						try {
							sc = SocketChannel.open(new InetSocketAddress(ip, port));
							sc.configureBlocking(false);
							boolean connected = sc.isConnected();
							log.info("========>connected:" + connected);
							if (connected) {
								sendData(sc, methodParam);
								receiveData = receiveData(sc);
								log.info((String) receiveData);
							}
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

						return receiveData;
					}
				});

	}

	public void sendData(SocketChannel socketChannel, MethodParam obj) throws IOException {
		byte[] bytes = SerializationUtils.serialize(obj);
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		socketChannel.write(buffer);
		socketChannel.socket().shutdownOutput();
	}

	public Object receiveData(SocketChannel socketChannel) {
		Object myResponseObject = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try {
			ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
			byte[] bytes;
			int count = 0;
			while ((count = socketChannel.read(buffer)) >= 0) {
				buffer.flip();
				bytes = new byte[count];
				buffer.get(bytes);
				baos.write(bytes);
				buffer.clear();
			}
			bytes = baos.toByteArray();
			Object obj = SerializationUtils.deserialize(bytes);
			myResponseObject = obj;
			socketChannel.socket().shutdownInput();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				baos.close();
			} catch (Exception ex) {
			}
		}
		return myResponseObject;
	}

}
