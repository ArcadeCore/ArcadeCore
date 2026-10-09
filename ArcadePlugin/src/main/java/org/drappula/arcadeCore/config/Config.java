package org.drappula.arcadeCore.config;

import org.drappula.arcadeCore.util.Log;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import dev.dejvokep.boostedyaml.spigot.SpigotSerializer;
import org.drappula.arcadeCore.ArcadeCore;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public abstract class Config {
    private final String fileName;
    private YamlDocument document;

    protected Config(String fileName) {
        this.fileName = fileName;
    }

    protected YamlDocument getDocument() {
        return document;
    }

    protected void load() {
        try {
            document = YamlDocument.create(
                    new File(ArcadeCore.get().getDataFolder(), fileName), Objects.requireNonNull(ArcadeCore.get().getResource(fileName)),
                    GeneralSettings.builder().setSerializer(SpigotSerializer.getInstance()).build(),
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("version")).build()
            );
        } catch (IOException e) {
            Log.error("An error occurred while setting up {} configuration.", fileName, e);
        }
    }
}
