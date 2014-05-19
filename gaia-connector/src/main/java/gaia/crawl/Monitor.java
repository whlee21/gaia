package gaia.crawl;

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Monitor extends Thread {
	private static final Logger LOG = LoggerFactory.getLogger(Monitor.class);
	private Process _process;
	private final int _port;
	private final String _key;
	ServerSocket _socket;

	public Monitor(int port, String key) {
		try {
			if (port < 0)
				return;
			setDaemon(true);
			setName("StopMonitor");
			_socket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
			if (port == 0) {
				port = _socket.getLocalPort();
				System.out.println(port);
			}

			if (key == null) {
				key = Long.toString((long) (Long.MAX_VALUE * Math.random()) + hashCode() + System.currentTimeMillis(), 36);
				System.out.println("STOP.KEY=" + key);
			}
		} catch (Exception e) {
			LOG.error("Error binding monitor port " + port + ": " + e.toString());
		} finally {
			_port = port;
			_key = key;
		}

		if (_socket != null)
			start();
		else
			LOG.warn("WARN: Not listening on monitor port: " + _port);
	}

	public Process getProcess() {
		return _process;
	}

	public void setProcess(Process process) {
		_process = process;
	}

	public void run() {
		while (true) {
			Socket socket = null;
			try {
				socket = _socket.accept();

				LineNumberReader lin = new LineNumberReader(new InputStreamReader(socket.getInputStream()));

				String key = lin.readLine();
				if (!_key.equals(key)) {
					System.err.println("Ignoring command with incorrect key");

					if (socket != null)
						try {
							socket.close();
						} catch (Exception e) {
						}
					socket = null;
				} else {
					String cmd = lin.readLine();
					LOG.debug("command=" + cmd);
					if ("stop".equals(cmd)) {
						if (_process != null) {
							try {
								_process.destroy();
								_process.waitFor();
							} catch (InterruptedException e) {
								LOG.warn("Interrupted waiting for child to terminate");
							}
						}
						socket.getOutputStream().write("Stopped\r\n".getBytes());
						try {
							socket.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
						try {
							_socket.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
						System.exit(0);
					} else if ("status".equals(cmd)) {
						socket.getOutputStream().write("OK\r\n".getBytes());
						socket.getOutputStream().flush();
					}
				}
			} catch (Exception e) {
				LOG.debug(e.toString());
			} finally {
				if (socket != null)
					try {
						socket.close();
					} catch (Exception e) {
					}
				socket = null;
			}
		}
	}
}