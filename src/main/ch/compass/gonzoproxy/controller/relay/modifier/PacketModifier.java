package ch.compass.gonzoproxy.controller.relay.modifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import ch.compass.gonzoproxy.model.modifier.FieldRule;
import ch.compass.gonzoproxy.model.modifier.PacketRegex;
import ch.compass.gonzoproxy.model.modifier.PacketRule;
import ch.compass.gonzoproxy.model.packet.Field;
import ch.compass.gonzoproxy.model.packet.Packet;
import ch.compass.gonzoproxy.model.packet.PacketDataFormat;
import ch.compass.gonzoproxy.model.template.TemplateSettings;
import ch.compass.gonzoproxy.utils.PersistingUtils;
import ch.compass.gonzoproxy.utils.TemplateUtils;

public class PacketModifier {

	private static final String REGEX_FILE = "resources/regex_rules.dat";
	private static final String RULE_FILE = "resources/modifier_rules.dat";

	private ArrayList<PacketRule> packetRules = new ArrayList<PacketRule>();
	private ArrayList<PacketRegex> packetsRegex = new ArrayList<PacketRegex>();

	public PacketModifier() {
		loadModifiers();
		loadRegex();
	}

	public void modifyByRule(Packet originalPacket) {
		for (PacketRule modifier : packetRules) {
			if (ruleSetMatches(modifier, originalPacket)) {
				applyRules(modifier, originalPacket);
			}
		}
	}

	public void modifyByRegex(Packet packet) {
		for (PacketRegex regex : packetsRegex) {
			if (regex.isActive()) {

				String originalPacketData = new String(packet.getPacketData());
				String modifiedPacketData = originalPacketData.replaceAll(
						regex.getRegex(), regex.getReplaceWith());
				if (!originalPacketData.equals(modifiedPacketData)) {
					packet.setPacketData(modifiedPacketData.getBytes());
					packet.setModified(true);
				}
			}
		}
	}

	public void addRule(String packetName, String fieldName,
			String originalValue, String replacedValue, Boolean updateLength) {
		FieldRule fieldRule = new FieldRule(fieldName, originalValue,
				replacedValue);
		createRule(packetName, fieldRule, updateLength);

	}

	private void createRule(String packetName, FieldRule fieldRule,
			Boolean updateLength) {
		PacketRule existingRuleSet = findRuleSet(packetName);
		if (existingRuleSet != null) {
			existingRuleSet.add(fieldRule);
			existingRuleSet.setUpdateLength(updateLength);
		} else {
			PacketRule createdRuleSet = new PacketRule(packetName);
			createdRuleSet.add(fieldRule);
			packetRules.add(createdRuleSet);
			createdRuleSet.setUpdateLength(updateLength);
		}
	}

	public void addRegex(String regex, String replaceWith, boolean isActive) {
		PacketRegex packetRegex = new PacketRegex(regex, replaceWith);
		packetRegex.setActive(isActive);
		packetsRegex.add(packetRegex);
	}

	public ArrayList<PacketRule> getPacketRule() {
		return packetRules;
	}

	public ArrayList<PacketRegex> getPacketRegex() {
		return packetsRegex;
	}

	public void persistRules() throws IOException {
		File modifierFile = new File(RULE_FILE);
		PersistingUtils.saveFile(modifierFile, packetRules);
	}

	public void persistRegex() throws IOException {
		File regexFile = new File(REGEX_FILE);
		PersistingUtils.saveFile(regexFile, packetsRegex);
	}

	private PacketRule findRuleSet(String packetName) {
		for (PacketRule existingModifier : packetRules) {
			if (existingModifier.getCorrespondingPacket().equals(packetName))
				return existingModifier;
		}
		return null;
	}

	private void applyRules(PacketRule modifier, Packet packet) {

		for (Field field : packet.getFields()) {
			FieldRule rule = modifier.findMatchingRule(field);

			if (rule != null && rule.isActive()) {
				int fieldLengthDiff;

				if (rule.getOriginalValue().isEmpty()) {
					fieldLengthDiff = computeLengthDifference(field.getValue(),
							rule.getReplacedValue());

					updatePacketLenght(packet, fieldLengthDiff);

					if (shouldUpdateContentLength(modifier, field)) {
						updateContentLengthField(packet, fieldLengthDiff);
					}
					field.setValue(rule.getReplacedValue());

				} else {
					fieldLengthDiff = computeLengthDifference(
							rule.getOriginalValue(), rule.getReplacedValue());

					updatePacketLenght(packet, fieldLengthDiff);

					if (shouldUpdateContentLength(modifier, field)) {
						updateContentLengthField(packet, fieldLengthDiff);
					}
					field.replaceValue(rule.getOriginalValue(),
							rule.getReplacedValue());
				}
				packet.setModified(true);
			}
		}
	}

	private boolean shouldUpdateContentLength(PacketRule modifier, Field field) {
		return modifier.shouldUpdateContentLength()
				&& field.getName().toUpperCase()
						.contains(TemplateSettings.CONTENT_DATA);
	}

	private void updatePacketLenght(Packet modifiedPacket, int fieldLengthDiff) {
		int updatedPacketSize = modifiedPacket.getSize() + fieldLengthDiff;
		modifiedPacket.setSize(updatedPacketSize);
	}

	private void updateContentLengthField(Packet packet, int fieldLengthDiff) {
		Field contentLengthField = TemplateUtils.findContentLengthField(packet);
		if (contentLengthField.getValue() != null) {
			try {
				int currentContentLength = Integer.parseInt(
						contentLengthField.getValue(), 16);
				int newContentLength = currentContentLength + fieldLengthDiff;
				contentLengthField.setValue(toHexString(newContentLength));
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}

	}

	private int computeLengthDifference(String originalValue,
			String replacedValue) {
		String originalValueNoWhitespaces = originalValue.replaceAll("\\s", "");
		String replacedValueNoWhitespaces = replacedValue.replaceAll("\\s", "");
		int diff = (replacedValueNoWhitespaces.length() - originalValueNoWhitespaces
				.length()) / PacketDataFormat.ENCODING_OFFSET;
		return diff;
	}

	private boolean ruleSetMatches(PacketRule existingRuleSet,
			Packet originalPacket) {
		return existingRuleSet.getCorrespondingPacket().equals(
				originalPacket.getDescription());
	}

	private String toHexString(int newContentLength) {
		StringBuilder sb = new StringBuilder();
		sb.append(Integer.toHexString(newContentLength));
		if (sb.length() < 2) {
			sb.insert(0, '0');
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private void loadModifiers() {
		File modifierFile = new File(RULE_FILE);
		try {
			packetRules = (ArrayList<PacketRule>) PersistingUtils
					.loadFile(modifierFile);

		} catch (FileNotFoundException e) {

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private void loadRegex() {
		File regexFile = new File(REGEX_FILE);
		try {
			packetsRegex = (ArrayList<PacketRegex>) PersistingUtils
					.loadFile(regexFile);
		} catch (FileNotFoundException e) {

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
