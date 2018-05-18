package org.hypergraphql.services;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.services.ApplicationConfigurationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApplicationConfigurationServiceTest {

    @Mock
    private HGQLConfigService hgqlConfigService;

    private ApplicationConfigurationService service = new ApplicationConfigurationService();

//    @Test
//    void should_parse_file_paths_as_files() {
//
//        final File tempDirectory = copyTestResourcesToFileSystem();
//        final List<HGQLConfig> configurations = service.getConfigFiles(tempDirectory.getAbsolutePath());
//        assertNotNull(configurations);
//
//        final List<HGQLConfig> expectedStreams = new ArrayList<>();
//        Arrays.asList(tempDirectory.listFiles(file -> file.getName().endsWith(".json"))).forEach(file -> {
//                    try {
//                        expectedStreams.add(new FileInputStream(file));
//                    } catch (FileNotFoundException e) {
//                        throw new RuntimeException("Test error: file not found", e);
//                    }
//                });
//
//        final List<String> expected = readStreams(expectedStreams);
//        final List<String> actual = readStreams(configurations);
//
//        assertIterableEquals(expected, actual);
//    }

//    @Test
//    void should_parse_individual_file() throws Exception {
//
//        final File tempFile = copyTestResourceToFileSystem("test_config_resources/config4.json");
//        final List<HGQLConfig> configurations = service.getConfigFiles(tempFile.getAbsolutePath());
//        assertNotNull(configurations);
//
//        final List<InputStream> expected = Collections.singletonList(new FileInputStream(tempFile));
//
//        assertEquals(readStreams(expected), readStreams(configurations));
//    }

//    @Test
//    void should_load_classpath_resources() {
//
//        final String[] resources = {
//                "test_config_resources/config1.json",
//                "test_config_resources/config2.json",
//                "test_config_resources/config3.json",
//                "test_config_resources/config4.json"
//        };
//        final List<InputStream> actual = service.getConfigResources(resources);
//
//        assertEquals(4, actual.size());
//    }

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

            final File tempFile = File.createTempFile("hgql_", ".json", tempDirectory);
            final InputStream in = getClass().getClassLoader().getResourceAsStream(path);
            IOUtils.copy(in, new FileOutputStream(tempFile));
            tempFile.renameTo(new File(tempDirectory, FilenameUtils.getName(path)));
            tempFile.deleteOnExit();

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

    private List<String> readStreams(final List<InputStream> streams) {

        final List<String> strings = new ArrayList<>();
        streams.forEach(stream -> {
            try {
                strings.add(IOUtils.toString(stream, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException("Parse to String failed", e);
            }
        });
        return strings;
    }
}