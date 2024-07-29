package org.languagetool.tools.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import static org.languagetool.constans.LanguageToolConstants.Config.CONFIGURATION_PROPERTIES_FILE_NAME;

public class ConfigService {

  private final Properties properties;

  public ConfigService() {

    final InputStream propertiesInput = ConfigService.class.getClassLoader().getResourceAsStream(CONFIGURATION_PROPERTIES_FILE_NAME);
    properties = new Properties();
      try {
          properties.load(propertiesInput);
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
  }

  public String getProperty(String key) {

    return Optional.ofNullable(System.getenv(mapToEnvVarKey(key)))
      .orElseGet(() -> properties.getProperty(key));
  }

  public String getProperty(String key, String defaultValue) {

    return Optional.ofNullable(System.getenv(mapToEnvVarKey(key)))
      .orElseGet(() -> properties.getProperty(key, defaultValue));
  }

  private String mapToEnvVarKey(String key) {

    return key.replaceAll("\\.", "_").toUpperCase();
  }
}
