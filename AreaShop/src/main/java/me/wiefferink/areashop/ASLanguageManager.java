package me.wiefferink.areashop;

import java.io.Reader;
import java.nio.charset.StandardCharsets;

import me.wiefferink.interactivemessenger.Log;
import me.wiefferink.interactivemessenger.processing.Message;
import me.wiefferink.interactivemessenger.source.MessageProvider;
import me.wiefferink.interactivemessenger.translation.Transifex;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URISyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Collections;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;


public class ASLanguageManager implements MessageProvider {
    /**
     * Folder inside the language folder with user overrides, these files are never overwritten by the plugin.
     */
    public static final String CUSTOM_FOLDER = "custom";
    /**
     * Folder inside the language folder with copies of the bundled files that the language files are based on.
     */
    public static final String BUNDLED_FOLDER = ".bundled";
    /**
     * Folder inside the language folder with backups of language files that had to be replaced.
     */
    public static final String BACKUP_FOLDER = "backups";

    private JavaPlugin plugin;
    private String jarLanguagePath;
    private File languageFolder;
    private String wgPrefix;
    private List<String> chatPrefix;
    private Map<String, List<String>> currentLanguage, defaultLanguage;

    public ASLanguageManager(JavaPlugin plugin,
                             String jarLanguagePath,
                             String currentLanguageName,
                             String defaultLanguageName,
                             List<String> chatPrefix,
                             String wgPrefix) {
        this.plugin = plugin;
        this.jarLanguagePath = jarLanguagePath;
        this.chatPrefix = chatPrefix;
        this.languageFolder = new File(plugin.getDataFolder() + File.separator + jarLanguagePath);

        Message.init(this, plugin.getLogger());
        saveDefaults();
        currentLanguage = loadLanguage(currentLanguageName);
        if (defaultLanguageName.equals(currentLanguageName)) {
            defaultLanguage = currentLanguage;
        } else {
            defaultLanguage = loadLanguage(defaultLanguageName);
        }
        // Apply user overrides on top of the active language
        File overrideFile = new File(languageFolder, CUSTOM_FOLDER + File.separator + currentLanguageName + ".yml");
        if (overrideFile.isFile()) {
            Map<String, List<String>> overrides = loadLanguage(CUSTOM_FOLDER + File.separator + currentLanguageName, false);
            if (!overrides.isEmpty()) {
                Map<String, List<String>> merged = new HashMap<>(currentLanguage);
                merged.putAll(overrides);
                if (currentLanguage == defaultLanguage) {
                    defaultLanguage = merged;
                }
                currentLanguage = merged;
                Log.info("Loaded " + overrides.size() + " custom messages from " + overrideFile.getPath());
            }
        }
        this.wgPrefix = wgPrefix;
    }

    @Override
    public List<String> getMessage(String key) {
        List<String> message;
        if (key.equalsIgnoreCase(Message.CHATLANGUAGEVARIABLE)) {
            message = chatPrefix;
        } else if (key.equalsIgnoreCase("wgPrefix")) {
            message = new ArrayList<>();
            message.add(wgPrefix);
        } else if (currentLanguage.containsKey(key)) {
            message = currentLanguage.get(key);
        } else {
            message = defaultLanguage.get(key);
        }
        if (message == null) {
            Log.warn("Did not find message '" + key + "' in the current or default language");
            return new ArrayList<>();
        }
        return new ArrayList<>(message);
    }

    /**
     * Saves the bundled language files to the language folder without losing edits made to them.
     * A copy of the bundled version that a file was last based on is kept in {@link #BUNDLED_FOLDER},
     * so that on a plugin update only the messages the server owner did not change get the new text.
     */
    private void saveDefaults() {
        // Create the language folder if it not exists
        if (!languageFolder.exists()) {
            if (!languageFolder.mkdirs()) {
                Log.warn("Could not create language directory: " + languageFolder.getAbsolutePath());
                return;
            }
        }
        File customFolder = new File(languageFolder, CUSTOM_FOLDER);
        if (!customFolder.exists() && !customFolder.mkdirs()) {
            Log.warn("Could not create custom language directory: " + customFolder.getAbsolutePath());
        }
        File bundledFolder = new File(languageFolder, BUNDLED_FOLDER);
        if (!bundledFolder.exists() && !bundledFolder.mkdirs()) {
            Log.warn("Could not create bundled language directory: " + bundledFolder.getAbsolutePath());
            return;
        }
        File jarPath;
        try {
            // Read jar as ZIP file
            jarPath = new File(plugin.getClass()
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
        } catch (URISyntaxException e) {
            Log.error("Failed to find location of jar file:", ExceptionUtils.getStackTrace(e));
            return;
        }
        try (ZipFile jar = new ZipFile(jarPath)) {
            Enumeration<? extends ZipEntry> entries = jar.entries();

            // Each entry is a file or directory
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                // Filter to YAML files in the language directory
                if (!entry.isDirectory() && entry.getName()
                        .startsWith(jarLanguagePath + "/") && entry.getName()
                        .endsWith(".yml")) {
                    String fileName = entry.getName().substring(entry.getName().lastIndexOf("/") + 1);
                    try (InputStream input = jar.getInputStream(entry)) {
                        updateLanguageFile(languageFolder, fileName, input.readAllBytes());
                    } catch (IOException | InvalidConfigurationException e) {
                        Log.warn("Something went wrong saving default language file " + fileName + ":", ExceptionUtils.getStackTrace(e));
                    }
                }
            }
        } catch (
                IOException e) {
            Log.error("Failed to read zip file:", ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * Brings a language file on disk up to date with the version bundled in the jar.
     *
     * @param languageFolder The folder with the language files
     * @param fileName       The name of the language file, including '.yml'
     * @param bundled        The content of the file in the jar
     */
    static void updateLanguageFile(File languageFolder, String fileName, byte[] bundled) throws IOException, InvalidConfigurationException {
        File target = new File(languageFolder, fileName);
        File snapshot = new File(languageFolder, BUNDLED_FOLDER + File.separator + fileName);
        Files.createDirectories(snapshot.toPath().getParent());

        // New file: save the bundled version
        if (!target.exists()) {
            Files.write(target.toPath(), bundled);
            Files.write(snapshot.toPath(), bundled);
            return;
        }

        // File saved by an older version of the plugin, which overwrote it on every start.
        // There is no way to tell edits apart from changes in the plugin, so keep a backup and start fresh.
        if (!snapshot.exists()) {
            if (!Arrays.equals(Files.readAllBytes(target.toPath()), bundled)) {
                File backupFolder = new File(languageFolder, BACKUP_FOLDER);
                if (!backupFolder.exists() && !backupFolder.mkdirs()) {
                    throw new IOException("Could not create language backup directory: " + backupFolder.getAbsolutePath());
                }
                String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
                File backup = new File(backupFolder, fileName.substring(0, fileName.length() - 4) + "-" + timestamp + ".yml");
                Files.copy(target.toPath(), backup.toPath());
                Files.write(target.toPath(), bundled);
                Log.warn("Language file " + fileName + " was replaced by the bundled version, the previous file is saved as "
                        + backup.getPath() + ". From now on, edits to " + fileName + " are kept when the plugin starts or updates.");
            }
            Files.write(snapshot.toPath(), bundled);
            return;
        }

        // Bundled version did not change since the file was last updated: keep the file as it is
        byte[] previousBundled = Files.readAllBytes(snapshot.toPath());
        if (Arrays.equals(previousBundled, bundled)) {
            return;
        }

        // Plugin update: take the new text for every message that was not edited on disk
        YamlConfiguration current = new YamlConfiguration();
        current.load(target);
        YamlConfiguration previous = new YamlConfiguration();
        previous.loadFromString(new String(previousBundled, StandardCharsets.UTF_8));
        YamlConfiguration latest = new YamlConfiguration();
        latest.loadFromString(new String(bundled, StandardCharsets.UTF_8));
        for (String messageKey : latest.getKeys(false)) {
            List<String> currentValue = getLines(current, messageKey);
            if (currentValue == null || currentValue.equals(getLines(previous, messageKey))) {
                current.set(messageKey, latest.get(messageKey));
            }
        }
        current.save(target);
        Files.write(snapshot.toPath(), bundled);
    }

    /**
     * Get a message from a language file as lines.
     *
     * @param file The language file
     * @param key  The key of the message
     * @return The lines of the message, or null if the file does not have it
     */
    private static List<String> getLines(YamlConfiguration file, String key) {
        if (!file.contains(key)) {
            return null;
        }
        if (file.isList(key)) {
            return file.getStringList(key);
        }
        return Collections.singletonList(file.getString(key));
    }

    private Map<String, List<String>> loadLanguage(String key) {
        return loadLanguage(key, true);
    }

    /**
     * Loads the specified language
     *
     * @param key     The language to load
     * @param convert try conversion or not (infinite recursion prevention)
     * @return Map with the messages loaded from the file
     */
    private Map<String, List<String>> loadLanguage(String key, boolean convert) {
        Map<String, List<String>> result = new HashMap<>();

        // Load the language file
        boolean convertFromTransifex = false;
        File file = new File(languageFolder.getAbsolutePath() + File.separator + key + ".yml");
        try (
                InputStream inputStream = new FileInputStream(file);
                Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        ) {
            // Detect empty language files, happens when the YAML parsers prints an exception (it does return an empty YamlConfiguration though)
            YamlConfiguration ymlFile = YamlConfiguration.loadConfiguration(reader);
            if (ymlFile.getKeys(false).isEmpty()) {
                Log.warn("Language file " + key + ".yml has zero messages.");
                return result;
            }

            // Retrieve the messages from the YAML file and create the result
            if (convert && Transifex.needsConversion(ymlFile)) {
                convertFromTransifex = true;
            } else {
                for (String messageKey : ymlFile.getKeys(false)) {
                    List<String> toPut;
                    if (ymlFile.isList(messageKey)) {
                        toPut = ymlFile.getStringList(messageKey);
                    } else {
                        toPut = Collections.singletonList(ymlFile.getString(messageKey));
                    }
                    result.put(messageKey, new ArrayList<>(toPut));
                }
            }
        } catch (IOException e) {
            Log.warn("Could not load language file: " + file.getAbsolutePath());
        }

        // Do conversion (after block above closed the reader)
        if (convertFromTransifex) {
            if (!Transifex.convertFrom(file)) {
                Log.warn("Failed to convert " + file.getName() + " from the Transifex layout to the AreaShop layout, check the errors above");
            }
            return loadLanguage(key, false);
        }

        return result;
    }
}
