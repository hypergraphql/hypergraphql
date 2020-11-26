package org.hypergraphql.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.val;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.hypergraphql.config.system.HGQLConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApplicationConfigurationServiceTest {

    @Mock
    private HGQLConfigService hgqlConfigService;

    private ApplicationConfigurationService service = new ApplicationConfigurationService();

    @Test
    @Disabled
    void should_parse_file_paths_as_files() {

        val tempDirectory = copyTestResourcesToFileSystem();
        final List<HGQLConfig> configurations = service.getConfigFiles(tempDirectory.getAbsolutePath());
        assertNotNull(configurations);

        final List<HGQLConfig> expectedConfigs = new ArrayList<>();
        final String[] filenames = tempDirectory.list((directory, filename) -> filename.endsWith(".json"));
        Arrays.stream(filenames).forEach(filename -> {
            val file = new File(tempDirectory, filename);
                expectedConfigs.add(service.getConfigurationsFromFile(file.getAbsolutePath()).get(0));
            }
        );

        final List<String> expected = readConfigs(expectedConfigs);
        final List<String> actual = readConfigs(configurations);

        assertIterableEquals(expected, actual);
    }

//    @Test
//    void should_parse_individual_file() throws Exception {
//
//        val tempFile = copyTestResourceToFileSystem("test_configurations/config4.json");
//        final List<HGQLConfig> configurations = service.getConfigFiles(tempFile.getAbsolutePath());
//        assertNotNull(configurations);
//
//        final List<InputStream> expected = Collections.singletonList(new FileInputStream(tempFile));
//
//        assertEquals(readConfigs(expected), readConfigs(configurations));
//    }

//    @Test
//    void should_load_classpath_resources() {
//
//        final String[] resources = {
//                "test_configurations/config1.json",
//                "test_configurations/config2.json",
//                "test_configurations/config3.json",
//                "test_configurations/config4.json"
//        };
//        final List<HGQLConfig> actual = service.getConfigResources(resources);
//
//        assertEquals(4, actual.size());
//    }

    private File copyTestResourcesToFileSystem() {

        val tempDirectory = createTempDirectory();
        // copy input streams to temp files
        List.of(
                "test_configurations/config1.json",
                "test_configurations/config2.json",
                "test_configurations/config3.json",
                "test_configurations/config4.json",
                "test_configurations/schema1.graphql",
                "test_configurations/schema2.graphql",
                "test_configurations/schema3.graphql",
                "test_configurations/schema4.graphql"
        ).forEach(path -> copyClasspathResourceToTemp(tempDirectory, path));
        return tempDirectory;
    }

//    private File copyTestResourceToFileSystem(final String path) {
//
//        val tempDirectory = createTempDirectory();
//        copyClasspathResourceToTemp(tempDirectory, path);
//        return new File(tempDirectory, new File(path).getName());
//    }

    private void copyClasspathResourceToTemp(final File tempDirectory, final String path) {

        try {

            val tempFile = File.createTempFile("hgql_", ".json", tempDirectory);
            val in = getClass().getClassLoader().getResourceAsStream(path);
            IOUtils.copy(in, new FileOutputStream(tempFile));
            tempFile.renameTo(new File(tempDirectory, FilenameUtils.getName(path)));
            tempFile.deleteOnExit();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File createTempDirectory() {

        val tmpDir = System.getProperty("java.io.tmpdir");
        val tempDirectory = new File(tmpDir + (tmpDir.endsWith("/") ? "" : "/") + "hgql");
        tempDirectory.mkdirs();
        tempDirectory.deleteOnExit();
        return tempDirectory;
    }

    private List<String> readConfigs(final List<HGQLConfig> configs) {

        final List<String> strings = new ArrayList<>();
        configs.forEach(config -> {
            strings.add(config.getName() + config.getSchemaFile());
        });
        return strings;
    }
}
