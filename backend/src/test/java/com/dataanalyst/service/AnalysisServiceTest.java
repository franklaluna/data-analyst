package com.dataanalyst.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AnalysisService chart data generation.
 * Focuses on the escapePythonString method that fixes the column name escaping issue.
 */
class AnalysisServiceTest {

    /**
     * Helper to call the private static escapePythonString method via reflection.
     */
    private String callEscapePythonString(String input) throws Exception {
        Method method = AnalysisService.class.getDeclaredMethod("escapePythonString", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, input);
    }

    @Test
    @DisplayName("Should preserve column names with parentheses")
    void testColumnWithParentheses() throws Exception {
        String result = callEscapePythonString("Sales (2023)");
        assertEquals("Sales (2023)", result);
    }

    @Test
    @DisplayName("Should preserve column names with dots")
    void testColumnWithDots() throws Exception {
        String result = callEscapePythonString("col.name");
        assertEquals("col.name", result);
    }

    @Test
    @DisplayName("Should preserve column names with brackets")
    void testColumnWithBrackets() throws Exception {
        String result = callEscapePythonString("Value [USD]");
        assertEquals("Value [USD]", result);
    }

    @Test
    @DisplayName("Should preserve Chinese column names")
    void testChineseColumnName() throws Exception {
        String result = callEscapePythonString("销售额");
        assertEquals("销售额", result);
    }

    @Test
    @DisplayName("Should preserve column names with spaces")
    void testColumnWithSpaces() throws Exception {
        String result = callEscapePythonString("First Name");
        assertEquals("First Name", result);
    }

    @Test
    @DisplayName("Should escape single quotes to prevent code injection")
    void testEscapeSingleQuote() throws Exception {
        String result = callEscapePythonString("O'Brien");
        assertEquals("O\\'Brien", result);
    }

    @Test
    @DisplayName("Should escape backslashes")
    void testEscapeBackslash() throws Exception {
        String result = callEscapePythonString("path\\to\\file");
        assertEquals("path\\\\to\\\\file", result);
    }

    @Test
    @DisplayName("Should handle null input")
    void testNullInput() throws Exception {
        String result = callEscapePythonString(null);
        assertNull(result);
    }

    @Test
    @DisplayName("Should handle empty string")
    void testEmptyString() throws Exception {
        String result = callEscapePythonString("");
        assertEquals("_col", result);
    }

    @Test
    @DisplayName("Should preserve simple alphanumeric column names")
    void testSimpleColumnName() throws Exception {
        String result = callEscapePythonString("amount");
        assertEquals("amount", result);
    }

    @Test
    @DisplayName("Should preserve column names with hyphens")
    void testColumnWithHyphens() throws Exception {
        String result = callEscapePythonString("col-1");
        assertEquals("col-1", result);
    }

    @Test
    @DisplayName("Should preserve column names with underscores")
    void testColumnWithUnderscores() throws Exception {
        String result = callEscapePythonString("col_1");
        assertEquals("col_1", result);
    }

    @Test
    @DisplayName("Should handle complex column name with multiple special chars")
    void testComplexColumnName() throws Exception {
        String result = callEscapePythonString("Sales (2023) - Total [USD]");
        assertEquals("Sales (2023) - Total [USD]", result);
    }

    @Test
    @DisplayName("Should handle column name with both single quote and backslash")
    void testQuoteAndBackslash() throws Exception {
        String result = callEscapePythonString("O'Brien\\Data");
        assertEquals("O\\'Brien\\\\Data", result);
    }
}
