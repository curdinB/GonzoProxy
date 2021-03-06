package ch.compass.gonzoproxy.controller.relay;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import ch.compass.gonzoproxy.model.listener.StateListener;
import ch.compass.gonzoproxy.model.modifier.PacketRegex;
import ch.compass.gonzoproxy.model.modifier.PacketRule;
import ch.compass.gonzoproxy.model.relay.RelayDataModel;

public interface RelayService extends Runnable {

	public void run();

	public void stopSession();

	public void setConnectionParameters(String portListen,
			String remoteHost, String remotePort, String mode);

	public void commandTrapChanged();

	public void responseTrapChanged();

	public void sendOneCmd();

	public void sendOneRes();

	public int getCurrentListenPort();

	public String getCurrentRemoteHost();

	public int getCurrentRemotePort();

	public void addSessionStateListener(StateListener stateListener);

	public RelayDataModel getSessionModel();

	public void reparse();

	public void persistSessionData(File file) throws IOException;

	public void loadPacketsFromFile(File file) throws ClassNotFoundException,
			IOException;

	public ArrayList<PacketRule> getPacketRules();

	public ArrayList<PacketRegex> getPacketRegex();

	public void addRule(String packetName, String fieldName,
			String originalValue, String replacedValue, Boolean updateLength);

	public void addRegex(String regex, String replaceValue, boolean isActive);

	public void persistRules() throws IOException;

	public void persistRegex() throws IOException;

}