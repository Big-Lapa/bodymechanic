package com.lapin.bodymechanic.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MvcConfigTest {

    @TempDir
    Path tmp;

    /** Exposes the protected handler mapping built by the registry. */
    private static class ExposedRegistry extends ResourceHandlerRegistry {
        ExposedRegistry() {
            super(new GenericApplicationContext(), new MockServletContext());
        }

        AbstractHandlerMapping mapping() {
            return getHandlerMapping();
        }
    }

    private List<String> imgLocations(Path uploadDir) {
        MvcConfig config = new MvcConfig();
        ReflectionTestUtils.setField(config, "uploadPath", uploadDir.toString());

        ExposedRegistry registry = new ExposedRegistry();
        config.addResourceHandlers(registry);

        SimpleUrlHandlerMapping mapping = (SimpleUrlHandlerMapping) registry.mapping();
        ResourceHttpRequestHandler handler = (ResourceHttpRequestHandler) mapping.getUrlMap().get("/img/**");
        assertNotNull(handler, "/img/** must be registered");
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) ReflectionTestUtils.getField(handler, "locationValues");
        return values;
    }

    @Test
    void imgLocation_endsWithSlash_whenUploadDirDoesNotExistYet() {
        Path missing = tmp.resolve("uploads-missing");

        List<String> locations = imgLocations(missing);

        assertEquals(1, locations.size());
        assertTrue(locations.get(0).endsWith("/"),
                "location must end with '/', otherwise relative resources resolve to the parent directory: "
                        + locations.get(0));
    }

    @Test
    void imgLocation_endsWithSlash_whenUploadDirExists() throws Exception {
        Path present = Files.createDirectories(tmp.resolve("uploads-present"));

        List<String> locations = imgLocations(present);

        assertTrue(locations.get(0).endsWith("/"), locations.get(0));
    }

    @Test
    void imgLocation_pointsInsideTheUploadDirectory() {
        Path missing = tmp.resolve("uploads-missing");

        String location = imgLocations(missing).get(0);

        assertTrue(location.contains("uploads-missing"), location);
    }
}
