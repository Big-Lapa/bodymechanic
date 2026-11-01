package com.lapin.bodymechanic.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * .env.example is copied verbatim by people following the README, so it must not
 * contain a usable admin password.
 */
class EnvExampleTest {

    private static final Path ENV_EXAMPLE = Paths.get(".env.example");

    private List<String> lines() throws Exception {
        return Files.readAllLines(ENV_EXAMPLE);
    }

    @Test
    void adminPasswordIsEmptyInTheExample() throws Exception {
        List<String> adminLines = lines().stream()
                .filter(l -> l.startsWith("ADMIN_PASSWORD="))
                .collect(Collectors.toList());

        assertEquals(1, adminLines.size(), "ADMIN_PASSWORD must be listed exactly once");
        assertEquals("ADMIN_PASSWORD=", adminLines.get(0).trim(),
                "ADMIN_PASSWORD must be empty so a copied example cannot create a known admin");
    }

    @Test
    void adminPasswordIsDocumentedByAComment() throws Exception {
        List<String> lines = lines();
        int idx = lines.indexOf(lines.stream()
                .filter(l -> l.startsWith("ADMIN_PASSWORD=")).findFirst().orElse(""));

        assertFalse(idx <= 0, "ADMIN_PASSWORD must exist and have a comment above it");
        assertEquals('#', lines.get(idx - 1).trim().charAt(0));
    }
}
