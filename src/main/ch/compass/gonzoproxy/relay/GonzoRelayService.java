package ch.compass.gonzoproxy.relay;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ch.compass.gonzoproxy.listener.StateListener;
import ch.compass.gonzoproxy.model.ForwardingType;
import ch.compass.gonzoproxy.model.SessionModel;
import ch.compass.gonzoproxy.relay.io.RelayDataHandler;
import ch.compass.gonzoproxy.relay.io.streamhandler.PacketStreamReader;
import ch.compass.gonzoproxy.relay.io.streamhandler.PacketStreamWriter;
import ch.compass.gonzoproxy.relay.modifier.FieldRule;
import ch.compass.gonzoproxy.relay.modifier.PacketRegex;
import ch.compass.gonzoproxy.relay.modifier.PacketRule;
import ch.compass.gonzoproxy.relay.settings.RelaySettings;
import ch.compass.gonzoproxy.relay.settings.RelaySettings.SessionState;

public class GonzoRelayService implements RelayService {

	private ExecutorService threadPool;

	private ServerSocket serverSocket;
	private Socket initiator;
	private Socket target;
	private RelaySettings sessionSettings = new RelaySettings();;

	private RelayDataHandler relayDataHandler;

	public GonzoRelayService() {
		relayDataHandler = new RelayDataHandler(sessionSettings);
	}

	@Override
	public void run() {
		handleRelaySession();
	}

	private void handleRelaySession() {
		threadPool = Executors.newFixedThreadPool(4);
		if(establishConnection()){
			initProducerConsumer();
			handleData();
		}
	}

	private void handleData() {
		try {
			relayDataHandler.processRelayData();
		} catch (InterruptedException e) {
			stopSession();
		}

	}

	private void initProducerConsumer() {
		try {
			initCommandStreamHandlers();
			initResponseStreamHandlers();
		} catch (IOException e) {
			sessionSettings.setSessionState(SessionState.CONNECTION_REFUSED);
			stopSession();
		}
		sessionSettings.setSessionState(SessionState.FORWARDING);
	}

	private boolean establishConnection() {
		try {
			sessionSettings.setSessionState(SessionState.CONNECTING);
			
			if(awaitInitiatorConnection()){
				connectToTarget();
				sessionSettings.setSessionState(SessionState.CONNECTED);
				return true;
			}else {
				sessionSettings.setSessionState(SessionState.DISCONNECTED);
				return false;
			}
		} finally {
			try {
				serverSocket.close();
			} catch (IOException e) {
				// is this code reached? :>
				// log server socket not closed ?
			}
		}
	}

	private void connectToTarget() {
		try {
			target = new Socket(sessionSettings.getRemoteHost(),
					sessionSettings.getRemotePort());
		} catch (IOException e) {
			try {
				initiator.close();
			} catch (IOException e1) {
			}
			sessionSettings.setSessionState(SessionState.CONNECTION_REFUSED);
		}
	}

	private boolean awaitInitiatorConnection() {
		try {
			serverSocket = new ServerSocket(sessionSettings.getListenPort());
			initiator = serverSocket.accept();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	private void initCommandStreamHandlers() throws IOException {

		InputStream inputStream = new BufferedInputStream(
				initiator.getInputStream());
		OutputStream outputStream = new BufferedOutputStream(
				target.getOutputStream());
		PacketStreamReader commandStreamReader = new PacketStreamReader(
				inputStream, relayDataHandler, sessionSettings.getMode(),
				ForwardingType.COMMAND);
		PacketStreamWriter commandStreamWriter = new PacketStreamWriter(
				outputStream, relayDataHandler, sessionSettings.getMode(),
				ForwardingType.COMMAND);

		commandStreamWriter.setTrapListener(sessionSettings);

		threadPool.execute(commandStreamReader);
		threadPool.execute(commandStreamWriter);
	}

	private void initResponseStreamHandlers() throws IOException {
		InputStream inputStream = new BufferedInputStream(
				target.getInputStream());
		OutputStream outputStream = new BufferedOutputStream(
				initiator.getOutputStream());
		PacketStreamReader responseStreamReader = new PacketStreamReader(
				inputStream, relayDataHandler, sessionSettings.getMode(),
				ForwardingType.RESPONSE);
		PacketStreamWriter responseStreamWriter = new PacketStreamWriter(
				outputStream, relayDataHandler, sessionSettings.getMode(),
				ForwardingType.RESPONSE);

		responseStreamWriter.setTrapListener(sessionSettings);

		threadPool.execute(responseStreamReader);
		threadPool.execute(responseStreamWriter);
	}

	public void stopSession() {
		relayDataHandler.stopDataHandling();
		closeSockets();
		shutdownConsumerProducer();
		sessionSettings.setSessionState(SessionState.DISCONNECTED);
	}

	private void shutdownConsumerProducer() {
		if (threadPool != null)
			threadPool.shutdownNow();
	}

	private void closeSockets() {
		if (socketIsOpen(initiator)) {
			try {
				initiator.close();
			} catch (IOException e) {
				// TODO : state -> socket closing error
				sessionSettings.setSessionState(SessionState.DISCONNECTED);
			}
		}

		if (socketIsOpen(target)) {
			try {
				target.close();
			} catch (IOException e) {
				sessionSettings.setSessionState(SessionState.DISCONNECTED);
			}
		}
		if(serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				sessionSettings.setSessionState(SessionState.DISCONNECTED);
			}
		}
	}

	private boolean socketIsOpen(Socket socket) {
		return socket != null && !socket.isClosed();
	}

	public void generateNewSessionParameters(String portListen,
			String remoteHost, String remotePort, String mode) {
		sessionSettings.setSession(Integer.parseInt(portListen), remoteHost,
				Integer.parseInt(remotePort));
		sessionSettings.setMode(mode);
		relayDataHandler.reset();
	}

	public void commandTrapChanged() {
		switch (sessionSettings.getSessionState()) {
		case COMMAND_TRAP:
			sessionSettings.setTrapState(SessionState.FORWARDING);
			break;
		case FORWARDING:
			sessionSettings.setTrapState(SessionState.COMMAND_TRAP);
			break;
		case RESPONSE_TRAP:
			sessionSettings.setTrapState(SessionState.TRAP);
			break;
		case TRAP:
			sessionSettings.setTrapState(SessionState.RESPONSE_TRAP);
			break;
		default:
			sessionSettings.setTrapState(SessionState.COMMAND_TRAP);
			break;
		}
	}

	public void responseTrapChanged() {
		switch (sessionSettings.getSessionState()) {
		case RESPONSE_TRAP:
			sessionSettings.setTrapState(SessionState.FORWARDING);
			break;
		case FORWARDING:
			sessionSettings.setTrapState(SessionState.RESPONSE_TRAP);
			break;
		case COMMAND_TRAP:
			sessionSettings.setTrapState(SessionState.TRAP);
			break;
		case TRAP:
			sessionSettings.setTrapState(SessionState.COMMAND_TRAP);
			break;
		default:
			sessionSettings.setTrapState(SessionState.RESPONSE_TRAP);
			break;
		}
	}

	public void sendOneCmd() {
		sessionSettings.sendOneCommand();
	}

	public void sendOneRes() {
		sessionSettings.sendOneResponse();
	}

	public int getCurrentListenPort() {
		return sessionSettings.getListenPort();
	}

	public String getCurrentRemoteHost() {
		return sessionSettings.getRemoteHost();
	}

	public int getCurrentRemotePort() {
		return sessionSettings.getRemotePort();
	}

	public void addSessionStateListener(StateListener stateListener) {
		sessionSettings.addSessionStateListener(stateListener);
	}

	public SessionModel getSessionModel() {
		return relayDataHandler.getSessionModel();
	}

	public void reParse() {
		relayDataHandler.reparse();
	}

	public void persistSessionData(File file) throws IOException {
		relayDataHandler.persistSessionData(file);
	}

	public void loadPacketsFromFile(File file) throws ClassNotFoundException,
			IOException {
		relayDataHandler.loadPacketsFromFile(file);
	}

	public ArrayList<PacketRule> getPacketRules() {
		return relayDataHandler.getPacketRules();
	}

	public ArrayList<PacketRegex> getPacketRegex() {
		return relayDataHandler.getPacketRegex();
	}

	public void addRule(String packetName, FieldRule fieldRule,
			Boolean updateLength) {
		relayDataHandler.addRule(packetName, fieldRule, updateLength);

	}

	public void addRegex(PacketRegex packetRegex, boolean isActive) {
		relayDataHandler.addRegex(packetRegex, isActive);
	}

	public void persistRules() throws IOException {
		relayDataHandler.persistRules();
	}

	public void persistRegex() throws IOException {
		relayDataHandler.persistRegex();
	}

}
