package org.hypergraphql;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApplicationConfigurationServiceTest {

    private ApplicationConfigurationService service = new ApplicationConfigurationService();

    @Test
    void should_parse_file_paths_as_files() {

        final File tempDirectory = copyTestResourcesToFileSystem();
        final List<File> configFiles = service.getConfigFiles(tempDirectory.getAbsolutePath());
        assertNotNull(configFiles);

        final List<File> expected = Arrays.asList(tempDirectory.listFiles(file -> file.getName().endsWith(".json")));

        assertIterableEquals(expected, configFiles);
    }

    @Test
    void should_parse_individual_file() {

        final File tempFile = copyTestResourceToFileSystem("test_config_resources/config4.json");
        final List<File> configFiles = service.getConfigFiles(tempFile.getAbsolutePath());
        assertNotNull(configFiles);

        final List<File> expected = Collections.singletonList(tempFile);

        assertEquals(expected, configFiles);
    }

    @Test
    void should_load_classpath_resources() {

        final String[] resources = {
                "test_config_resources/config1.json",
                "test_config_resources/config2.json",
                "test_config_resources/config3.json",
                "test_config_resources/config4.json"
        };
        final List<File> actual = service.getConfigResources(resources);

        assertEquals(4, actual.size());
    }

    private File copyTestResourcesToFileSystem() {

        final File tempDirectory = createTempDirectory();
        // copy input streams to temp files
        Arrays.asList(
                "test_config_resources/config1.json",
                "test_config_resources/config2.json",
                "test_config_resources/config3.json",
                "test_config_resources/config4.json"
        ).forEach(path -> copyClasspathResourceToTemp(tempDirectory, path));
        return tempDirectory;
    }

    private File copyTestResourceToFileSystem(final String path) {

        final File tempDirectory = createTempDirectory();
        copyClasspathResourceToTemp(tempDirectory, path);
        return new File(tempDirectory, new File(path).getName());
    }

    private void copyClasspathResourceToTemp(final File tempDirectory, final String path) {

        try {
//            System.out.println("Trying to create temp file at:\n" + tempDirectory.getAbsolutePath());

            final File tempFile = File.createTempFile("hgql_", ".json", tempDirectory);
            final InputStream in = getClass().getClassLoader().getResourceAsStream(path);
            IOUtils.copy(in, new FileOutputStream(tempFile));
            tempFile.deleteOnExit();

//            System.out.println("Created:\n" + tempFile.getAbsolutePath() + "\nFrom: " + path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File createTempDirectory() {

        final File tempDirectory = new File(System.getenv("TMPDIR") + "hgql");
        tempDirectory.mkdirs();
        tempDirectory.deleteOnExit();
        return tempDirectory;
    }
}