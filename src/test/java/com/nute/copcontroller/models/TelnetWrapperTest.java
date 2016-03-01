package com.nute.copcontroller.models;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.junit.Before;
import org.junit.Test;

public class TelnetWrapperTest {
	private TelnetWrapper telnetWrapper = mock(TelnetWrapper.class);
	private final Socket socket = mock(Socket.class);
	private static final String TESTSTRING = "test";
	
	@Before
	public void init() throws IOException {
		when(socket.getInputStream()).thenReturn(mock(InputStream.class));
		when(socket.getOutputStream()).thenReturn(mock(OutputStream.class));
	}
	
	@Test
	public void testSendCommand() {
		telnetWrapper.sendCommand(TESTSTRING, TESTSTRING);
		verify(telnetWrapper, atLeastOnce()).sendCommand(TESTSTRING, TESTSTRING);
	}
	
	@Test
	public void testClose() throws IOException {
		telnetWrapper.close();
		verify(telnetWrapper, atLeastOnce()).close();
	}
}
