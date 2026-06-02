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
	private volatile Process currentProcess;
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
			Files.writeString(script, String.join(System.lineSeparator(),
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
					"        $sep = $line.IndexOf('|')",
					"        if ($sep -lt 0) { continue }",
					"        $sep2 = $line.IndexOf('|', $sep + 1)",
					"        if ($sep2 -lt 0) { continue }",
					"        $rate = [int]$line.Substring(0, $sep)",
					"        $voice = $line.Substring($sep + 1, $sep2 - $sep - 1)",
					"        $text = $line.Substring($sep2 + 1)",
					"        $s.SpeakAsyncCancelAll()",
					"        $s.Rate = $rate",
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

	private void startWindowsProcess() {
		synchronized (windowsLock) {
			try {
				ProcessBuilder builder = new ProcessBuilder(
						"powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive",
						"-ExecutionPolicy", "Bypass", "-File", windowsTtsScript.toString()
				);
				builder.redirectErrorStream(true);
				windowsProcess = builder.start();
				windowsStdin = new BufferedWriter(new OutputStreamWriter(
						windowsProcess.getOutputStream(), StandardCharsets.UTF_8));
				Thread drainer = new Thread(() -> {
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(
							windowsProcess.getInputStream(), StandardCharsets.UTF_8))) {
						while (reader.readLine() != null) { }
					} catch (IOException ignored) {}
				}, "Dawn TTS Drainer");
				drainer.setDaemon(true);
				drainer.start();
				logger.info("Windows TTS persistent process started");
			} catch (IOException exception) {
				logger.warn("Failed to start Windows TTS process", exception);
				windowsProcess = null;
				windowsStdin = null;
			}
		}
	}

	private void sendWindowsCommand(String command) {
		synchronized (windowsLock) {
			if (windowsProcess == null || !windowsProcess.isAlive()) {
				startWindowsProcess();
			}
			if (windowsStdin == null) {
				return;
			}
			try {
				windowsStdin.write(command);
				windowsStdin.newLine();
				windowsStdin.flush();
			} catch (IOException exception) {
				logger.warn("TTS command failed, restarting process", exception);
				try { windowsStdin.close(); } catch (IOException ignored) {}
				windowsProcess.destroyForcibly();
				windowsProcess = null;
				windowsStdin = null;
				startWindowsProcess();
				if (windowsStdin != null) {
					try {
						windowsStdin.write(command);
						windowsStdin.newLine();
						windowsStdin.flush();
					} catch (IOException retryException) {
						logger.warn("TTS command failed after restart", retryException);
					}
				}
			}
		}
	}

	@Override
	public void speak(String text, TtsOptions options) {
		if (text.isBlank()) {
			return;
		}

		if (!available) {
			speakWithMinecraftNarrator(text);
			return;
		}

		if (platform == Platform.WINDOWS) {
			final String cmd = options.rate() + "|" + options.voiceIdOrBlank() + "|" + text;
			executor.execute(() -> sendWindowsCommand(cmd));
			return;
		}

		stop();
		final long gen = speakGeneration.incrementAndGet();
		executor.execute(() -> {
			if (gen != speakGeneration.get()) {
				return;
			}
			try {
				ProcessBuilder builder = new ProcessBuilder(commandForSpeech(text, options));
				builder.redirectErrorStream(true);
				Process process = builder.start();
				currentProcess = process;
				int exitCode = process.waitFor();
				if (exitCode != 0) {
					logger.warn("System TTS exited with code {}", exitCode);
					speakWithMinecraftNarrator(text);
				}
			} catch (IOException exception) {
				available = false;
				availabilityMessage = exception.getMessage();
				logger.warn("System TTS failed", exception);
				speakWithMinecraftNarrator(text);
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
			} finally {
				currentProcess = null;
			}
		});
	}

	@Override
	public void stop() {
		if (platform == Platform.WINDOWS) {
			synchronized (windowsLock) {
				if (windowsStdin != null) {
					try {
						windowsStdin.write("STOP");
						windowsStdin.newLine();
						windowsStdin.flush();
					} catch (IOException ignored) {}
				}
			}
		} else {
			Process process = currentProcess;
			if (process != null && process.isAlive()) {
				process.destroyForcibly();
			}
		}
		clearMinecraftNarrator();
	}

	@Override
	public List<Voice> listVoices() {
		return voices;
	}

	@Override
	public boolean isAvailable() {
		return available || isMinecraftNarratorAvailable();
	}

	@Override
	public String availabilityMessage() {
		if (!available && isMinecraftNarratorAvailable()) {
			return "Minecraft narrator fallback available";
		}
		return availabilityMessage;
	}

	private boolean checkAvailability() {
		try {
			Process process = new ProcessBuilder(commandForAvailability()).redirectErrorStream(true).start();
			if (!process.waitFor(probeTimeoutSeconds(), TimeUnit.SECONDS)) {
				process.destroyForcibly();
				availabilityMessage = "System TTS probe timed out";
				return false;
			}
			String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
			if (process.exitValue() == 0) {
				return true;
			}
			availabilityMessage = output.isBlank() ? "System TTS probe exited with code " + process.exitValue() : output;
			return false;
		} catch (IOException exception) {
			availabilityMessage = exception.getMessage();
			return false;
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private List<String> commandForAvailability() {
		return switch (platform) {
			case WINDOWS -> List.of(
					"powershell.exe",
					"-NoLogo",
					"-NoProfile",
					"-NonInteractive",
					"-ExecutionPolicy",
					"Bypass",
					"-Command",
					"Add-Type -AssemblyName System.Speech; $s=New-Object System.Speech.Synthesis.SpeechSynthesizer; $s.Dispose()"
			);
			case MACOS -> List.of("say", "--version");
			case LINUX -> List.of("spd-say", "--version");
			case UNKNOWN -> List.of("dawn-tts-unavailable");
		};
	}

	private List<String> commandForSpeech(String text, TtsOptions options) {
		String voice = options.voiceIdOrBlank();
		String rate = Integer.toString(options.rate());
		return switch (platform) {
			case WINDOWS -> {
				if (windowsTtsScript == null) {
					yield List.of("dawn-tts-unavailable", text);
				}
				yield List.of(
						"powershell.exe",
						"-NoLogo",
						"-NoProfile",
						"-NonInteractive",
						"-ExecutionPolicy",
						"Bypass",
						"-File",
						windowsTtsScript.toString(),
						voice,
						rate,
						text
				);
			}
			case MACOS -> {
				List<String> command = new ArrayList<>();
				command.add("say");
				if (!voice.isBlank()) {
					command.add("-v");
					command.add(voice);
				}
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
				if (!voice.isBlank()) {
					command.add("-y");
					command.add(voice);
				}
				command.add(text);
				yield command;
			}
			case UNKNOWN -> List.of("dawn-tts-unavailable", text);
		};
	}

	private List<Voice> loadVoices() {
		List<Voice> found = new ArrayList<>();
		found.add(Voice.none());

		try {
			List<String> command = switch (platform) {
				case WINDOWS -> List.of(
						"powershell.exe",
						"-NoLogo",
						"-NoProfile",
						"-NonInteractive",
						"-ExecutionPolicy",
						"Bypass",
						"-Command",
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
					if (!voiceName.isBlank()) {
						found.add(new Voice(voiceName, voiceName));
					}
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
		if (trimmed.isBlank()) {
			return "";
		}
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
		if (client == null) {
			return;
		}

		client.execute(() -> {
			GameNarrator gameNarrator = client.getNarrator();
			if (gameNarrator == null) {
				return;
			}
			Narrator narrator = ((GameNarratorAccessor) gameNarrator).dawnAccessibility$getNarrator();
			if (narrator.active()) {
				narrator.clear();
				narrator.say(text, true, client.options.getFinalSoundSourceVolume(SoundSource.VOICE));
			}
		});
	}

	private void clearMinecraftNarrator() {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}

		client.execute(() -> {
			GameNarrator gameNarrator = client.getNarrator();
			if (gameNarrator == null) {
				return;
			}
			Narrator narrator = ((GameNarratorAccessor) gameNarrator).dawnAccessibility$getNarrator();
			if (narrator.active()) {
				narrator.clear();
			}
		});
	}

	private boolean isMinecraftNarratorAvailable() {
		try {
			Minecraft client = Minecraft.getInstance();
			if (client == null || client.getNarrator() == null) {
				return false;
			}
			return ((GameNarratorAccessor) client.getNarrator()).dawnAccessibility$getNarrator().active();
		} catch (RuntimeException exception) {
			return false;
		}
	}

	private enum Platform {
		WINDOWS,
		MACOS,
		LINUX,
		UNKNOWN;

		static Platform detect() {
			String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
			if (os.contains("win")) {
				return WINDOWS;
			}
			if (os.contains("mac")) {
				return MACOS;
			}
			if (os.contains("linux")) {
				return LINUX;
			}
			return UNKNOWN;
		}
	}
}
