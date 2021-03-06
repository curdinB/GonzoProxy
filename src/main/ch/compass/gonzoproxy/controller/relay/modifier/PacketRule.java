package ch.compass.gonzoproxy.controller.relay.modifier;

import java.io.Serializable;
import java.util.ArrayList;

import ch.compass.gonzoproxy.model.packet.Field;

public class PacketRule implements Serializable {

	private static final long serialVersionUID = -3893571727728725384L;

	private String correspondingPacket;

	private ArrayList<FieldRule> rules = new ArrayList<FieldRule>();

	private Boolean updateContentLength =  false;

	public PacketRule(String correspondingPacket) {
		this.correspondingPacket = correspondingPacket;
	}

	public String getCorrespondingPacket() {
		return correspondingPacket;
	}

	public void setCorrespondingPacket(String correspondingPacket) {
		this.correspondingPacket = correspondingPacket;
	}

	public void add(FieldRule rule) {
		rules.add(rule);
	}

	public ArrayList<FieldRule> getRules() {
		return rules;
	}

	public FieldRule findMatchingRule(Field field) {
		for (FieldRule rule : rules) {
			if (isMatchingRule(field, rule))
				return rule;
		}
		return null;
	}

	private boolean isMatchingRule(Field field, FieldRule rule) {
		return field.getName().equals(rule.getCorrespondingField())
				&& field.getValue().contains(rule.getOriginalValue());
	}

	public void setUpdateLength(Boolean updateLength) {
		this.updateContentLength = updateLength;
		
	}
	
	public boolean shouldUpdateContentLength(){
		return updateContentLength;
	}

	@Override
	public boolean equals(Object object) {
		return ((PacketRule) object).getCorrespondingPacket().equals(
				correspondingPacket);
	}
}
