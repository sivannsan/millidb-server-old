package com.sivannsan.millidb;

import com.sivannsan.millidata.MilliMap;
import com.sivannsan.millidata.MilliValue;
import com.sivannsan.millifile.MilliDocument;
import com.sivannsan.millifile.MilliFiles;

public class MilliDBConfigurations {
    private static MilliDocument configuration;
    private static MilliMap content;

    public static void load() {
        MilliDBLogs.info("Loading configuration...");
        configuration = MilliFiles.load("millidb.mll").asMilliDocument();
        if (!configuration.getContent().isMilliMap()) {
            configuration.setContent(new MilliMap(), 4);
        }
        content = configuration.getContent().asMilliMap();
        if (!content.get("port").isMilliValue()) {
            content.put("port", new MilliValue(31771));
            configuration.setContent(content, 4);
        }
        MilliDBLogs.info("Configuration loaded!");
    }

    public static int getPort() {
        return content.get("port").asMilliValue(new MilliValue(31771)).asInteger32();
    }
}
