package com.example.demo.config;

import com.example.demo.dto.BaseConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;

@Component
public class JsonLoader {

    public BaseConfig getBaseConfig() {
        ObjectMapper objectMapper = new ObjectMapper();
        File configFile = new File("C:\\Users\\20352201\\Downloads\\2.5.4\\Library\\config\\config.json");

        try {
            // Ensure the parent directories and file exist
            File parentDir = configFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs(); // Create parent directories if they don't exist
            }
            if (!configFile.exists()) {
                configFile.createNewFile(); // Create the file if it doesn't exist

                // Write a default BaseConfig object to the new file
                BaseConfig defaultConfig = new BaseConfig();
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFile, defaultConfig);
                System.out.println("File created with default configuration: " + configFile.getAbsolutePath());
            }

            // Read the JSON file and map it to BaseConfig object
            FileReader fileReader = new FileReader(configFile);
            BaseConfig baseConfigObject = objectMapper.readValue(fileReader, BaseConfig.class);
            System.out.println(baseConfigObject);
            return baseConfigObject;

        } catch (Exception e) {
            System.out.println("""
                    Error while creating or reading the file config.json.\s
                    Please check the file and restart the application.
                    Shutting Down......
                    """ + e.getMessage());
            System.exit(1);
        }
        return null;
    }

    public void loadBaseConfig(BaseConfig baseConfig) {
        ObjectMapper objectMapper = new ObjectMapper();
        File configFile = new File("C:\\Users\\20352201\\Downloads\\2.5.4\\Library\\config\\config.json");

        try {
            // Ensure the parent directories exist
            File parentDir = configFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs(); // Create parent directories if they don't exist
            }
            // Create the file if it doesn't exist
            if (!configFile.exists()) {
                configFile.createNewFile();
                System.out.println("File created: " + configFile.getAbsolutePath());
            }

            // Write the updated BaseConfig object back to the JSON file
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFile, baseConfig);
            System.out.println("Updated BaseConfig saved successfully to config.json.");

        } catch (Exception e) {
            System.out.println("""
                    Error while writing to the file config.json.
                    Please check the file path and permissions.
                    """ + e.getMessage());
        }
    }
}
