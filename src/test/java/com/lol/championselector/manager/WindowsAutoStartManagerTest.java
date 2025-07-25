package com.lol.championselector.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WindowsAutoStartManagerTest {

    private WindowsAutoStartManager manager;

    @BeforeEach
    void setUp() {
        manager = new WindowsAutoStartManager();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testIsSupported_OnWindows() {
        // On Windows, should return true
        assertTrue(manager.isSupported());
    }

    @Test
    void testPermissionDiagnosticCreation() {
        WindowsAutoStartManager.PermissionDiagnostic diagnostic = 
            new WindowsAutoStartManager.PermissionDiagnostic(
                false, 
                "Test issue", 
                "Test solution", 
                WindowsAutoStartManager.PermissionDiagnostic.PermissionType.REGISTRY_READ
            );
        
        assertFalse(diagnostic.hasPermission());
        assertEquals("Test issue", diagnostic.getIssue());
        assertEquals("Test solution", diagnostic.getSolution());
        assertEquals(WindowsAutoStartManager.PermissionDiagnostic.PermissionType.REGISTRY_READ, diagnostic.getType());
    }

    @Test
    void testDiagnosePermissions_ReturnsCorrectNumberOfChecks() {
        List<WindowsAutoStartManager.PermissionDiagnostic> diagnostics = manager.diagnosePermissions();
        
        // Should check all 5 permission types
        assertEquals(5, diagnostics.size());
        
        // Verify all permission types are checked
        long registryReadCount = diagnostics.stream()
            .filter(d -> d.getType() == WindowsAutoStartManager.PermissionDiagnostic.PermissionType.REGISTRY_READ)
            .count();
        assertEquals(1, registryReadCount);
        
        long registryWriteCount = diagnostics.stream()
            .filter(d -> d.getType() == WindowsAutoStartManager.PermissionDiagnostic.PermissionType.REGISTRY_WRITE)
            .count();
        assertEquals(1, registryWriteCount);
        
        long fileAccessCount = diagnostics.stream()
            .filter(d -> d.getType() == WindowsAutoStartManager.PermissionDiagnostic.PermissionType.FILE_ACCESS)
            .count();
        assertEquals(1, fileAccessCount);
        
        long javaExecCount = diagnostics.stream()
            .filter(d -> d.getType() == WindowsAutoStartManager.PermissionDiagnostic.PermissionType.JAVA_EXECUTABLE)
            .count();
        assertEquals(1, javaExecCount);
        
        long systemCmdCount = diagnostics.stream()
            .filter(d -> d.getType() == WindowsAutoStartManager.PermissionDiagnostic.PermissionType.SYSTEM_COMMAND)
            .count();
        assertEquals(1, systemCmdCount);
    }

    @Test
    void testGetPermissionReport_WithNoIssues() {
        // Mock manager to return all successful diagnostics
        WindowsAutoStartManager mockManager = mock(WindowsAutoStartManager.class);
        List<WindowsAutoStartManager.PermissionDiagnostic> mockDiagnostics = List.of(
            new WindowsAutoStartManager.PermissionDiagnostic(true, null, null, 
                WindowsAutoStartManager.PermissionDiagnostic.PermissionType.REGISTRY_READ),
            new WindowsAutoStartManager.PermissionDiagnostic(true, null, null, 
                WindowsAutoStartManager.PermissionDiagnostic.PermissionType.REGISTRY_WRITE),
            new WindowsAutoStartManager.PermissionDiagnostic(true, null, null, 
                WindowsAutoStartManager.PermissionDiagnostic.PermissionType.FILE_ACCESS),
            new WindowsAutoStartManager.PermissionDiagnostic(true, null, null, 
                WindowsAutoStartManager.PermissionDiagnostic.PermissionType.JAVA_EXECUTABLE),
            new WindowsAutoStartManager.PermissionDiagnostic(true, null, null, 
                WindowsAutoStartManager.PermissionDiagnostic.PermissionType.SYSTEM_COMMAND)
        );
        
        when(mockManager.diagnosePermissions()).thenReturn(mockDiagnostics);
        when(mockManager.getPermissionReport()).thenCallRealMethod();
        
        String report = mockManager.getPermissionReport();
        
        assertTrue(report.contains("All permissions are available"));
        assertTrue(report.contains("Auto-Start Permission Diagnostic Report"));
    }

    @Test
    void testGetPermissionReport_WithIssues() {
        // Mock manager to return some failed diagnostics
        WindowsAutoStartManager mockManager = mock(WindowsAutoStartManager.class);
        List<WindowsAutoStartManager.PermissionDiagnostic> mockDiagnostics = List.of(
            new WindowsAutoStartManager.PermissionDiagnostic(false, "Registry read failed", "Run as admin", 
                WindowsAutoStartManager.PermissionDiagnostic.PermissionType.REGISTRY_READ),
            new WindowsAutoStartManager.PermissionDiagnostic(true, null, null, 
                WindowsAutoStartManager.PermissionDiagnostic.PermissionType.REGISTRY_WRITE),
            new WindowsAutoStartManager.PermissionDiagnostic(false, "File not found", "Build project", 
                WindowsAutoStartManager.PermissionDiagnostic.PermissionType.FILE_ACCESS),
            new WindowsAutoStartManager.PermissionDiagnostic(true, null, null, 
                WindowsAutoStartManager.PermissionDiagnostic.PermissionType.JAVA_EXECUTABLE),
            new WindowsAutoStartManager.PermissionDiagnostic(true, null, null, 
                WindowsAutoStartManager.PermissionDiagnostic.PermissionType.SYSTEM_COMMAND)
        );
        
        when(mockManager.diagnosePermissions()).thenReturn(mockDiagnostics);
        when(mockManager.getPermissionReport()).thenCallRealMethod();
        
        String report = mockManager.getPermissionReport();
        
        assertTrue(report.contains("Registry read failed"));
        assertTrue(report.contains("File not found"));
        assertTrue(report.contains("Run as admin"));
        assertTrue(report.contains("Build project"));
        assertTrue(report.contains("Common Solutions:"));
    }

    @Test
    void testAttemptPermissionFixes_NoIssues() {
        // Mock manager to return all successful diagnostics
        WindowsAutoStartManager mockManager = mock(WindowsAutoStartManager.class);
        List<WindowsAutoStartManager.PermissionDiagnostic> mockDiagnostics = List.of(
            new WindowsAutoStartManager.PermissionDiagnostic(true, null, null, 
                WindowsAutoStartManager.PermissionDiagnostic.PermissionType.REGISTRY_READ)
        );
        
        when(mockManager.diagnosePermissions()).thenReturn(mockDiagnostics);
        when(mockManager.attemptPermissionFixes()).thenCallRealMethod();
        
        List<String> fixes = mockManager.attemptPermissionFixes();
        
        assertEquals(1, fixes.size());
        assertTrue(fixes.get(0).contains("No permission issues detected"));
    }

    @Test
    void testGetManualFixInstructions_ContainsAllSections() {
        String instructions = manager.getManualFixInstructions();
        
        assertTrue(instructions.contains("Manual Fix Instructions"));
        assertTrue(instructions.contains("General Tips"));
    }

    @Test
    void testGetJarLocation_ReturnsString() {
        String jarLocation = manager.getJarLocation();
        assertNotNull(jarLocation);
    }

    @Test
    void testGetRegistryCommand_ReturnsValidFormat() {
        String command = manager.getRegistryCommand();
        assertNotNull(command);
        
        if (!command.equals("JAR file not found")) {
            // If JAR exists, command should contain java and jar
            boolean hasJavaCommand = command.contains("java") || command.contains("javaw");
            boolean hasJarExtension = command.contains(".jar") || command.contains("test-classes");
            boolean hasMinimizedFlag = command.contains("--minimized");
            
            assertTrue(hasJavaCommand, "Command should contain java or javaw: " + command);
            assertTrue(hasJarExtension, "Command should contain .jar or test path: " + command);
            assertTrue(hasMinimizedFlag, "Command should contain --minimized flag: " + command);
        }
    }

    @Test
    void testPermissionTypeEnumValues() {
        WindowsAutoStartManager.PermissionDiagnostic.PermissionType[] types = 
            WindowsAutoStartManager.PermissionDiagnostic.PermissionType.values();
        
        assertEquals(5, types.length);
        
        // Verify all expected types exist
        boolean hasRegistryRead = false, hasRegistryWrite = false, hasFileAccess = false,
                hasJavaExec = false, hasSystemCmd = false;
        
        for (WindowsAutoStartManager.PermissionDiagnostic.PermissionType type : types) {
            switch (type) {
                case REGISTRY_READ: hasRegistryRead = true; break;
                case REGISTRY_WRITE: hasRegistryWrite = true; break;
                case FILE_ACCESS: hasFileAccess = true; break;
                case JAVA_EXECUTABLE: hasJavaExec = true; break;
                case SYSTEM_COMMAND: hasSystemCmd = true; break;
            }
        }
        
        assertTrue(hasRegistryRead);
        assertTrue(hasRegistryWrite);
        assertTrue(hasFileAccess);
        assertTrue(hasJavaExec);
        assertTrue(hasSystemCmd);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testRealPermissionCheck_OnWindows() {
        // This test runs actual permission checks on Windows
        List<WindowsAutoStartManager.PermissionDiagnostic> diagnostics = manager.diagnosePermissions();
        
        assertNotNull(diagnostics);
        assertEquals(5, diagnostics.size());
        
        // At least system command and registry read should typically work
        boolean hasSystemCommand = diagnostics.stream()
            .anyMatch(d -> d.getType() == WindowsAutoStartManager.PermissionDiagnostic.PermissionType.SYSTEM_COMMAND 
                        && d.hasPermission());
        
        // System command should generally work on most Windows systems
        assertTrue(hasSystemCommand, "System command execution should be available");
    }
}