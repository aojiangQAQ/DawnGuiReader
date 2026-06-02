package org.dawnteam.accessibility.tts;

public record Voice(String id, String displayName) {
	public static Voice none() {
		return new Voice("", "Default");
	}
}
