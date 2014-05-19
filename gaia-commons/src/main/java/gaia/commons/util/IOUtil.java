package gaia.commons.util;

import java.io.IOException;
import java.net.ServerSocket;

public class IOUtil {
	public static int getRandomPort() {
		ServerSocket server = null;
		try {
			server = new ServerSocket(0);
			return server.getLocalPort();
		} catch (IOException e) {
			throw new Error(e);
		} finally {
			if (server != null)
				try {
					server.close();
				} catch (IOException ignore) {
				}
		}
	}

	public static int[] getRandomPorts(int count) {
		int[] result = new int[count];
		ServerSocket[] sockets = new ServerSocket[count];
		try {
			for (int i = 0; i < count; i++) {
				try {
					sockets[i] = new ServerSocket(0);
					result[i] = sockets[i].getLocalPort();
				} catch (IOException e) {
					throw new Error(e);
				}

			}

			if ((sockets != null) && (sockets.length > 0))
				for (int i = 0; i < sockets.length; i++)
					if (sockets[i] != null)
						try {
							sockets[i].close();
						} catch (IOException e) {
						}
		} finally {
			if ((sockets != null) && (sockets.length > 0)) {
				for (int i = 0; i < sockets.length; i++) {
					if (sockets[i] != null) {
						try {
							sockets[i].close();
						} catch (IOException e) {
						}
					}
				}
			}
		}
		return result;
	}
}
