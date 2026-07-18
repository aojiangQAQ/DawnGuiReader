package org.dawnteam.accessibility.tts;

import com.mojang.text2speech.Narrator;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.dawnteam.accessibility.mixin.GameNarratorAccessor;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class SystemTtsEngine implements TtsEngine {
	private static final int WINDOWS_TTS_TIMEOUT_SECONDS = 15;
	private static final int DEFAULT_TTS_TIMEOUT_SECONDS = 5;

	private final Logger logger;
	private final Platform platform;
	private final ExecutorService executor;
	private final List<Voice> voices;
	private final Path windowsTtsScript;
	private final AtomicLong speakGeneration = new AtomicLong();
	private volatile boolean available;
	private volatile String availabilityMessage;

	private Process windowsProcess;
	private BufferedWriter windowsStdin;
	private final Object windowsLock = new Object();

	public SystemTtsEngine(Logger logger) {
		this.logger = logger;
		this.platform = Platform.detect();
		this.executor = Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "Dawn GUI Reader TTS");
			thread.setDaemon(true);
			return thread;
		});
		this.windowsTtsScript = platform == Platform.WINDOWS ? writeWindowsTtsScript() : null;
		this.voices = Collections.unmodifiableList(loadVoices());
		this.available = checkAvailability();
		if (available) {
			this.availabilityMessage = "available";
		} else if (availabilityMessage == null || availabilityMessage.isBlank()) {
			this.availabilityMessage = "No supported system TTS command found";
		}
		if (platform == Platform.WINDOWS && available && windowsTtsScript != null) {
			startWindowsProcess();
		}
	}

	private Path writeWindowsTtsScript() {
		try {
			Path configDir = FabricLoader.getInstance().getConfigDir();
			Files.createDirectories(configDir);
			Path script = configDir.resolve("dawn-tts-speak.ps1");
			String nl = System.lineSeparator();
			Files.writeString(script, String.join(nl,
					"[Console]::InputEncoding = [System.Text.Encoding]::UTF8",
					"[Console]::OutputEncoding = [System.Text.Encoding]::UTF8",
					"Add-Type -AssemblyName System.Speech",
					"$s = New-Object System.Speech.Synthesis.SpeechSynthesizer",
					"try {",
					"    while ($true) {",
					"        $line = [Console]::In.ReadLine()",
					"        if ($null -eq $line) { break }",
					"        if ($line -eq \"STOP\") {",
					"            $s.SpeakAsyncCancelAll()",
					"            continue",
					"        }",
					"        $parts = $line.Split('|', 4)",
					"        if ($parts.Length -lt 4) { continue }",
					"        $rate = [int]$parts[0]",
					"        $volume = [int]$parts[1]",
					"        $voice = $parts[2]",
					"        $text = $parts[3]",
					"        $s.SpeakAsyncCancelAll()",
					"        $s.Rate = $rate",
					"        $s.Volume = $volume",
					"        if ($voice.Length -gt 0) { try { $s.SelectVoice($voice) } catch {} }",
					"        $s.SpeakAsync($text)",
					"    }",
					"} finally {",
					"    $s.Dispose()",
					"}"
			), StandardCharsets.UTF_8);
			return script;
		} catch (IOException exception) {
			logger.warn("Failed to write Windows TTS script", exception);
			return null;
		}
	}

	private boolean startWindowsProcess() {
		synchronized (windowsLock) {
			if (windowsTtsScript == null) return false;
			try {
				ProcessBuilder builder = new ProcessBuilder(
						"powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive",
						"-ExecutionPolicy", "Bypass", "-File", windowsTtsScript.toString()
				);
				builder.redirectErrorStream(true);
				Process process = builder.start();
				windowsProcess = process;
				windowsStdin = new BufferedWriter(new OutputStreamWriter(
						process.getOutputStream(), StandardCharsets.UTF_8));
				Thread drainer = new Thread(() -> {
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(
							process.getInputStream(), StandardCharsets.UTF_8))) {
						while (reader.readLine() != null) { }
					} catch (IOException ignored) {}
				}, "Dawn TTS Drainer");
				drainer.setDaemon(true);
				drainer.start();
				logger.info("Windows TTS persistent process started");
				return true;
			} catch (IOException exception) {
				logger.warn("Failed to start Windows TTS process", exception);
				windowsProcess = null;
				windowsStdin = null;
				return false;
			}
		}
	}

	private boolean sendWindowsCommand(String command) {
		synchronized (windowsLock) {
			if (windowsStdin == null || windowsProcess == null || !windowsProcess.isAlive()) {
				if (!startWindowsProcess()) return false;
			}
			try {
				windowsStdin.write(command);
				windowsStdin.newLine();
				windowsStdin.flush();
				return true;
			} catch (IOException exception) {
				logger.warn("Failed to send TTS command", exception);
				closeWindowsProcess();
				return false;
			}
		}
	}

	private void closeWindowsProcess() {
		if (windowsStdin != null) {
			try { windowsStdin.close(); } catch (IOException ignored) { }
		}
		if (windowsProcess != null) windowsProcess.destroy();
		windowsStdin = null;
		windowsProcess = null;
	}

	@Override
	public void speak(String text, TtsOptions options) {
		if (text == null || text.isBlank()) return;
		if (!available) {
			speakWithMinecraftNarrator(text);
			return;
		}

		long generation = speakGeneration.incrementAndGet();
		executor.execute(() -> {
			if (speakGeneration.get() != generation) return;
			if (platform == Platform.WINDOWS) {
				String singleLineText = text.replace('\r', ' ').replace('\n', ' ');
				String command = options.rate() + "|" + options.volume() + "|"
						+ options.voiceIdOrBlank() + "|" + singleLineText;
				if (!sendWindowsCommand(command)) speakWithMinecraftNarrator(text);
				return;
			}

			try {
				List<String> command = commandForSpeech(text, options);
				Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
				boolean finished = process.waitFor(DEFAULT_TTS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
				if (!finished) {
					process.destroyForcibly();
					speakWithMinecraftNarrator(text);
				} else if (process.exitValue() != 0) {
					speakWithMinecraftNarrator(text);
				}
			} catch (IOException exception) {
				speakWithMinecraftNarrator(text);
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
			}
		});
	}

	@Override
	public void stop() {
		speakGeneration.incrementAndGet();
		if (platform == Platform.WINDOWS) {
			executor.execute(() -> sendWindowsCommand("STOP"));
		}
	}

	@Override
	public List<Voice> listVoices() { return voices; }

	@Override
	public boolean isAvailable() { return available; }

	@Override
	public String availabilityMessage() { return availabilityMessage; }

	private boolean checkAvailability() {
		try {
			List<String> command = checkCommand();
			Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
			if (!process.waitFor(probeTimeoutSeconds(), TimeUnit.SECONDS)) {
				process.destroyForcibly();
				return false;
			}
			return process.exitValue() == 0;
		} catch (IOException | InterruptedException e) {
			if (e instanceof InterruptedException) Thread.currentThread().interrupt();
			return false;
		}
	}

	private List<String> checkCommand() {
		return switch (platform) {
			case WINDOWS -> List.of(
					"powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive",
					"-ExecutionPolicy", "Bypass", "-Command",
					"Add-Type -AssemblyName System.Speech; $s=New-Object System.Speech.Synthesis.SpeechSynthesizer; $s.Dispose()"
			);
			case MACOS -> List.of("say", "--version");
			case LINUX -> List.of("spd-say", "--version");
			case UNKNOWN -> List.of("dawn-tts-unavailable");
		};
	}

	private List<String> commandForSpeech(String text, TtsOptions options) {
		String voice = options.voiceIdOrBlank();
		return switch (platform) {
			case MACOS -> {
				List<String> command = new ArrayList<>();
				command.add("say");
				if (!voice.isBlank()) { command.add("-v"); command.add(voice); }
				command.add("-r");
				command.add(Integer.toString(175 + options.rate() * 25));
				command.add(text);
				yield command;
			}
			case LINUX -> {
				List<String> command = new ArrayList<>();
				command.add("spd-say");
				command.add("-r");
				command.add(Integer.toString(options.rate() * 10));
				if (!voice.isBlank()) { command.add("-y"); command.add(voice); }
				command.add(text);
				yield command;
			}
			default -> List.of("dawn-tts-unavailable", text);
		};
	}

	private List<Voice> loadVoices() {
		List<Voice> found = new ArrayList<>();
		found.add(Voice.none());
		try {
			List<String> command = switch (platform) {
				case WINDOWS -> List.of(
						"powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive",
						"-ExecutionPolicy", "Bypass", "-Command",
						"Add-Type -AssemblyName System.Speech; $s=New-Object System.Speech.Synthesis.SpeechSynthesizer; $s.GetInstalledVoices() | ForEach-Object { $_.VoiceInfo.Name }"
				);
				case MACOS -> List.of("say", "-v", "?");
				case LINUX -> List.of("spd-say", "-L");
				case UNKNOWN -> List.of("dawn-tts-unavailable");
			};
			Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
			if (!process.waitFor(probeTimeoutSeconds(), TimeUnit.SECONDS)) {
				process.destroyForcibly();
				return found;
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					String voiceName = parseVoiceLine(line);
					if (!voiceName.isBlank()) found.add(new Voice(voiceName, voiceName));
				}
			}
		} catch (IOException exception) {
			logger.debug("Unable to enumerate system TTS voices", exception);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
		return found;
	}

	private String parseVoiceLine(String line) {
		String trimmed = line.trim();
		if (trimmed.isBlank()) return "";
		if (platform == Platform.MACOS) {
			int firstSpace = trimmed.indexOf(' ');
			return firstSpace > 0 ? trimmed.substring(0, firstSpace) : trimmed;
		}
		return trimmed;
	}

	private int probeTimeoutSeconds() {
		return platform == Platform.WINDOWS ? WINDOWS_TTS_TIMEOUT_SECONDS : DEFAULT_TTS_TIMEOUT_SECONDS;
	}

	private void speakWithMinecraftNarrator(String text) {
		Minecraft client = Minecraft.getInstance();
		if (client == null) return;
		client.execute(() -> {
			GameNarrator gameNarrator = client.getNarrator();
			if (gameNarrator == null) return;
			Narrator narrator = ((GameNarratorAccessor) gameNarrator).dawnAccessibility$getNarrator();
			if (narrator.active()) {
				narrator.clear();
				narrator.say(text, true, client.options.getFinalSoundSourceVolume(SoundSource.VOICE));
			}
		});
	}

	private enum Platform {
		WINDOWS, MACOS, LINUX, UNKNOWN;
		static Platform detect() {
			String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
			if (os.contains("win")) return WINDOWS;
			if (os.contains("mac")) return MACOS;
			if (os.contains("linux")) return LINUX;
			return UNKNOWN;
		}
	}
}
