package cn.enn.test.nio;

import cn.enn.test.nio.server.Server;

public class TestServer {

	public static void main(String[] args) {

		Server server = Server.builder().port(8090).timeout(1000).build();
		server.start();
	}

}
