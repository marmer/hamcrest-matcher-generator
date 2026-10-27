package io.github.marmer.matchergenerator.annotations;

import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Every published jar carries the license, the NOTICE and the list of third-party dependencies.
 */
class LicenseFilesIT {

    @Test
    @DisplayName("The jar contains META-INF/LICENSE, META-INF/NOTICE and THIRD-PARTY.txt")
    void theJarContainsTheLicenseNoticeAndThirdPartyList() throws IOException {
        try (JarFile jar = new JarFile(Path.of(System.getProperty("module.jar")).toFile())) {
            assertNotNull(jar.getEntry("META-INF/LICENSE"), "META-INF/LICENSE");
            assertNotNull(jar.getEntry("META-INF/NOTICE"), "META-INF/NOTICE");
            assertNotNull(jar.getEntry("THIRD-PARTY.txt"), "THIRD-PARTY.txt");
        }
    }
}
