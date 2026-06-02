package org.dawnteam.accessibility.tts;

public record TtsOptions(int rate, int volume, String voiceId) {
	public String voiceIdOrBlank() {
		return voiceId == null ? "" : voiceId;
	}
}