package com.nute.copcontroller.models;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelnetWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(TelnetWrapper.class);

	private Socket socket;
	private DataOutputStream out;

	public TelnetWrapper(String server, Integer port) {
		try {
			this.socket = new Socket(server, port);
			this.out = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
		}
		LOGGER.debug("Connected to: {}:{}", server, port);
	}

	public synchronized InputStream getInputStreamWithMessage(String message) throws IOException {
		LOGGER.debug("Sending: {}", message);
		out.writeUTF(message);
		return socket.getInputStream();
	}

	public void close() throws IOException {
		LOGGER.debug("Closing Telnet connection.");
		if (socket != null) {
			LOGGER.debug("Socket is: " + (socket.isClosed() ? "CLOSED" : "STILL OPEN"));
			socket.close();
			LOGGER.debug("Socket is: " + (socket.isClosed() ? "CLOSED" : "STILL OPEN"));
		}
	}

}
