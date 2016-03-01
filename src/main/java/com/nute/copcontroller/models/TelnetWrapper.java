package com.nute.copcontroller.models;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelnetWrapper implements Closeable {
	private static final Logger LOGGER = LoggerFactory.getLogger(TelnetWrapper.class);

	private Socket socket;
	private final InputStream in;
	private final PrintStream out;
	private static final Long timeout = 1000L;

	public TelnetWrapper(String server, Integer port) throws IOException {
		this.socket = new Socket(server, port);
		this.in = socket.getInputStream();
		this.out = new PrintStream(socket.getOutputStream());

	}

	public String readUntil(String pattern) throws IOException {
		Long lastTime = System.currentTimeMillis();
		StringBuilder sb = new StringBuilder();
		while (true) {
			int c = -1;
			byte[] text;
			if (in.available() > 0) {
				c = in.read(text = new byte[in.available()]);
				sb.append(new String(text));
			}
			long now = System.currentTimeMillis();
			if (c != -1) {
				lastTime = now;
			}
			if (now - lastTime > timeout) {
				break;
			}
			if (sb.toString().contains(pattern)) {
				return sb.toString();
			}
			try {
				Thread.sleep(50);
			} catch (Exception e) {
			}
		}
		return sb.toString();
	}

	/**
	 * This method writes to server, but waits no response.
	 * 
	 * @param value
	 */
	public void write(String value) {
		try {
			out.println(value);
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends command and receives result.
	 * 
	 * @param command
	 * @param waitForPattern
	 * @return
	 */
	public String sendCommand(String command, String waitForPattern) {
		try {
			write(command);
			String until = readUntil(waitForPattern);
			LOGGER.debug("Command: " + command + "\t ouptut: " + until);
			return until;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void close() throws IOException {
		LOGGER.debug("Closing Telnet connection.");
		if (socket != null) {
			LOGGER.debug("Socket is: " + (socket.isClosed() ? "CLOSED" : "OPEN"));
			socket.close();
			LOGGER.debug("Socket is: " + (socket.isClosed() ? "CLOSED" : "OPEN"));
		}
	}

}
