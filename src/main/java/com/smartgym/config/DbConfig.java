package com.smartgym.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads database settings from src/main/resources/db.properties.
 * If the environment variables SMARTGYM_DB_URL, SMARTGYM_DB_USER or
 * SMARTGYM_DB_PASSWORD are set, they override the file values.
 * This way credentials are never hard-coded inside the Java code.
 */
public final class DbConfig {

    private static final String FILE_NAME = "db.properties";
    private static final Properties props = new Properties();

    // static block: runs once when the class is first used
    static {
        try (InputStream in = DbConfig.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
            if (in != null) {
                props.load(in);
            } else {
                System.err.println("WARNING: " + FILE_NAME + " was not found in src/main/resources");
            }
        } catch (IOException e) {
            System.err.println("Could not read " + FILE_NAME + ": " + e.getMessage());
        }
    }

    private DbConfig() { }   // utility class - no objects

    public static String getUrl()      { return pick("SMARTGYM_DB_URL", "db.url"); }
    public static String getUser()     { return pick("SMARTGYM_DB_USER", "db.user"); }
    public static String getPassword() { return pick("SMARTGYM_DB_PASSWORD", "db.password"); }

    private static String pick(String envName, String propName) {
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return props.getProperty(propName, "").trim();
    }
}
