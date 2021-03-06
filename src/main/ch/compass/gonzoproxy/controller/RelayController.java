package ch.compass.gonzoproxy.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.ResourceBundle;

import ch.compass.gonzoproxy.controller.relay.GonzoRelayService;
import ch.compass.gonzoproxy.controller.relay.RelayService;
import ch.compass.gonzoproxy.model.listener.StateListener;
import ch.compass.gonzoproxy.model.modifier.PacketRegex;
import ch.compass.gonzoproxy.model.modifier.PacketRule;
import ch.compass.gonzoproxy.model.relay.RelayDataModel;

public class RelayController {

	private String[] relayModes;

	private Thread relayServiceThread;
	private RelayService relayService = new GonzoRelayService();

	public RelayController() {
		loadPossibleRelayModes();
	}

	public void newSession(String portListen, String remoteHost,
			String remotePort, String mode, boolean runLocally) {
		stopRunningSession();
		relayService.setConnectionParameters(portListen, remoteHost,
				remotePort, mode);
		relayServiceThread = new Thread(relayService);
		relayServiceThread.start();
		if(runLocally){
			runLocally();
		}
	}

	public void stopRunningSession() {
		relayService.stopSession();
	}

	public RelayDataModel getSessionModel() {
		return relayService.getSessionModel();
	}

	public String[] getPossibleRelayModes() {
		return relayModes;
	}

	public ArrayList<PacketRule> getPacketRules() {
		return relayService.getPacketRules();
	}

	public ArrayList<PacketRegex> getPacketRegex() {
		return relayService.getPacketRegex();
	}

	public void addRule(String packetName, String fieldName,
			String originalValue, String replacedValue, Boolean updateContentLength) {
		relayService.addRule(packetName,fieldName, originalValue, replacedValue, updateContentLength);
		persistRules();
	}

	public void addRegex(String regex, String replaceWith, boolean isActive) {
		relayService.addRegex(regex,replaceWith, isActive);
		persistRegex();
	}

	public void commandTrapChanged() {
		relayService.commandTrapChanged();
	}

	public void responseTrapChanged() {
		relayService.responseTrapChanged();
	}

	public void sendOneCmd() {
		relayService.sendOneCmd();
	}

	public void sendOneRes() {
		relayService.sendOneRes();
	}

	public int getCurrentListenPort() {
		return relayService.getCurrentListenPort();
	}

	public String getCurrentRemoteHost() {
		return relayService.getCurrentRemoteHost();
	}

	public int getCurrentRemotePort() {
		return relayService.getCurrentRemotePort();
	}

	public void addSessionStateListener(StateListener stateListener) {
		relayService.addSessionStateListener(stateListener);
	}

	public void reparsePackets() {
		relayService.reparse();
	}

	public void persistSessionData(File file) {
		try {
			relayService.persistSessionData(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void loadPacketsFromFile(File file) {
		try {
			relayService.loadPacketsFromFile(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void persistRules() {
		try {
			relayService.persistRules();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void persistRegex() {
		try {
			relayService.persistRegex();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadPossibleRelayModes() {
		ArrayList<String> inputModes = new ArrayList<String>();

		ResourceBundle bundle = ResourceBundle.getBundle("plugin");

		Enumeration<String> keys = bundle.getKeys();
		while (keys.hasMoreElements()) {
			String element = keys.nextElement();
			if (element.contains("name")) {
				inputModes.add(bundle.getString(element));
			}
		}

		this.relayModes = inputModes.toArray(new String[inputModes.size()]);
	}

	private void runLocally() {
		String[] listenCmd = { "socat", "TCP-LISTEN:4321,reuseaddr",
				"EXEC:nfc-relay-picc -i,fdin=3,fdout=4" };

		String[] connectCmd = { "socat", "TCP:localhost:1234",
				"EXEC:nfc-relay-picc -t,fdin=3,fdout=4" };

		try {
			Runtime.getRuntime().exec(listenCmd);
			Runtime.getRuntime().exec(connectCmd);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
