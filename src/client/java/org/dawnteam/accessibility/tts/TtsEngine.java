package org.dawnteam.accessibility.tts;

import java.util.List;

public interface TtsEngine {
	void speak(String text, TtsOptions options);

	void stop();

	List<Voice> listVoices();

	boolean isAvailable();

	String availabilityMessage();
}
