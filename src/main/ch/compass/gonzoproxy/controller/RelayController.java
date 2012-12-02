package ch.compass.gonzoproxy.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.ResourceBundle;

import ch.compass.gonzoproxy.model.Packet;
import ch.compass.gonzoproxy.model.SessionModel;
import ch.compass.gonzoproxy.model.SessionSettings;
import ch.compass.gonzoproxy.model.SessionSettings.SessionState;
import ch.compass.gonzoproxy.relay.RelaySessionHandler;
import ch.compass.gonzoproxy.relay.modifier.PacketModifier;
import ch.compass.gonzoproxy.relay.modifier.FieldRule;

public class RelayController {

	private PacketModifier packetModifier = new PacketModifier();
	private SessionModel sessionModel = new SessionModel();
	private SessionSettings sessionSettings = new SessionSettings();
	private String[] modes;

	private Thread relayHandlerThread;

	public RelayController() {
		loadModes();
	}

	private void loadModes() {
		ArrayList<String> inputModes = new ArrayList<>();

		ResourceBundle bundle = ResourceBundle.getBundle("plugin");

		Enumeration<String> keys = bundle.getKeys();
		while (keys.hasMoreElements()) {
			String element = keys.nextElement();
			if (element.contains("name")) {
				inputModes.add(bundle.getString(element));
			}
		}

		this.modes = inputModes.toArray(new String[2]);
	}

	public void startRelaySession() {
		RelaySessionHandler relayHandler = new RelaySessionHandler();
		relayHandler.setSessionParameters(sessionModel, sessionSettings,
				packetModifier);
		relayHandlerThread = new Thread(relayHandler);
		relayHandlerThread.start();
	}

	public void newSession(String portListen, String remoteHost,
			String remotePort, String mode) {
		stopRunningSession();
		generateNewSessionParameters(portListen, remoteHost, remotePort, mode);
		startRelaySession();

	}

	private void generateNewSessionParameters(String portListen,
			String remoteHost, String remotePort, String mode) {
		sessionSettings.setSession(Integer.parseInt(portListen), remoteHost,
				Integer.parseInt(remotePort));
		sessionModel.clearData();
		sessionSettings.setMode(mode);
	}

	public void stopRunningSession() {
		if (relayHandlerThread != null && relayHandlerThread.isAlive()) 
			relayHandlerThread.interrupt();
	}

	//
	// public void stopRelaySession() {
	// relaySession.stopForwarder();
	// }

	public SessionModel getSessionModel() {
		return sessionModel;
	}

	public void addModifierRule(String packetName, String fieldName,
			String originalValue, String replacedValue, Boolean updateLength) {
		FieldRule fieldRule = new FieldRule(fieldName, originalValue,
				replacedValue);
		packetModifier.addRule(packetName, fieldRule, updateLength);
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
			break;
		}
	}

	public void sendOneCmd() {
		sessionSettings.sendOneCommand();
	}

	public void sendOneRes() {
		sessionSettings.sendOneResponse();
	}

	public String[] getModes() {
		return modes;
	}

	@SuppressWarnings("unchecked")
	public void openFile(File file) {
		RelaySessionHandler fakedRelay = new RelaySessionHandler();
		stopRunningSession();
		try (FileInputStream fin = new FileInputStream(file);
				ObjectInputStream ois = new ObjectInputStream(fin)) {
			ArrayList<Packet> loadedPacketStream = (ArrayList<Packet>) ois
					.readObject();
			fakedRelay.reParse(loadedPacketStream);
			sessionModel.addList(loadedPacketStream);
			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void saveFile(File file) {
		FileOutputStream fout;
		try {
			fout = new FileOutputStream(file);
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(sessionModel.getPacketList());
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public PacketModifier getPacketModifier() {
		return packetModifier;
	}

	public SessionSettings getSessionSettings() {
		return sessionSettings;
	}

}
