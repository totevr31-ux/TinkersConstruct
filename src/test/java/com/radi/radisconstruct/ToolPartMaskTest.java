package com.radi.radisconstruct;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Checks that source grayscale masks match original Hephaestus bytes. */
public class ToolPartMaskTest {
    @Test void allSourceMasksAreIdenticalToUpstream() throws Exception {
        var report = JsonParser.parseString(Files.readString(
            Path.of("build/portedResources/assets/radisconstruct/original_part_masks.json")))
            .getAsJsonObject().getAsJsonObject("masks");
        assertEquals(14, report.size());
        for (var entry : report.entrySet()) {
            var mask = entry.getValue().getAsJsonObject();
            Path source = Path.of(mask.get("source").getAsString());
            String path = mask.get("sprite").getAsString().replace(":", "/textures/") + ".png";
            Path staged = Path.of("build/portedResources/assets").resolve(path);
            assertTrue(Files.isRegularFile(staged), entry.getKey());
            byte[] original = Files.readAllBytes(source);
            assertArrayEquals(original, Files.readAllBytes(staged), entry.getKey());
            assertEquals(mask.get("sha256").getAsString(),
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original)));
        }
    }
}
