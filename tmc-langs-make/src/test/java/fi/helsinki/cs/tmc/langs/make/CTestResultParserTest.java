package fi.helsinki.cs.tmc.langs.make;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import fi.helsinki.cs.tmc.langs.TestResult;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class CTestResultParserTest {

    private ArrayList<CTestCase> oneOfEachTest;

    public CTestResultParserTest() {
    }

    @Before
    public void setUp() {
        oneOfEachTest = new ArrayList<CTestCase>();
        oneOfEachTest.add(new CTestCase("passing", "success", "Passed", null));
        oneOfEachTest.add(new CTestCase("failing", "failure", "This test should've failed", null));
    }

    @Test(expected = IllegalStateException.class)
    public void testParsingWithNoTests() throws Exception {
        CTestResultParser cpar = null;
        File tmp = mkTempFile("test_output", ".xml");
        try {
            cpar = new CTestResultParser(tmp, null, null);
            cpar.parseTestOutput();
        } finally {
            tmp.delete();
        }
        assertTrue(cpar.getTestResults().isEmpty());
    }

    @Test
    public void testParsingWithOneSuccessfulTest() {
        CTestResultParser cpar = null;
        try {
            ArrayList<CTestCase> testCases = new ArrayList<CTestCase>();
            testCases.add(oneOfEachTest.get(0));
            File tmp = constructTestOutput(testCases);
            cpar = new CTestResultParser(tmp, emptyValgrindOutput(), tmpFolder());
            cpar.parseTestOutput();
            tmp.delete();

        } catch (Exception e) {
            fail("Error creating or parsing mock output file: " + e.getMessage());
        }
        List<TestResult> results = cpar.getTestResults();
        assertEquals("There should only be one test result", 1, results.size());
        TestResult result = results.get(0);
        assertTrue("The test should be successful", result.passed);
        assertEquals("The name of the test should be \"passing\"", "passing", result.name);
    }

    @Test
    public void testParsingWithOneFailedTest() {
        CTestResultParser cpar = null;
        try {
            ArrayList<CTestCase> testCases = new ArrayList<CTestCase>();
            testCases.add(oneOfEachTest.get(1));
            File tmp = constructTestOutput(testCases);
            cpar = new CTestResultParser(tmp, null, null);
            cpar.parseTestOutput();
            tmp.delete();

        } catch (Exception e) {
            fail("Error creating or parsing mock output file: " + e.getMessage());
        }
        List<TestResult> results = cpar.getTestResults();
        assertEquals("There should only be one test result", 1, results.size());
        TestResult result = results.get(0);
        assertFalse("The test should not be successful", result.passed);
        assertEquals("The test should contain the message: This test should've failed",
            "This test should've failed", result.errorMessage);
        assertEquals("The name of the test should be \"failing\"", "failing", result.name);
    }

    @Test
    public void testParsingWithOneFailingAndOnePassing() {
        CTestResultParser cpar = null;
        try {
            File tmp = constructTestOutput(oneOfEachTest);
            cpar = new CTestResultParser(tmp, emptyValgrindOutput(), tmpFolder());
            cpar.parseTestOutput();
            tmp.delete();

        } catch (Exception e) {
            fail("Error creating or parsing mock output file: " + e.getMessage());
        }
        List<TestResult> results = cpar.getTestResults();
        assertEquals("There should be two test results", 2, results.size());
        assertTrue("The first should be passing", results.get(0).passed);
        assertFalse("The second should be failing", results.get(1).passed);
    }

    @Test
    public void testParsingWithEmptyValgrindOutput() {
        CTestResultParser cpar = null;
        try {
            ArrayList<CTestCase> testCases = new ArrayList<CTestCase>();
            testCases.add(oneOfEachTest.get(1));
            File ttmp = constructTestOutput(testCases);
            cpar = new CTestResultParser(ttmp, null, null);
            cpar.parseTestOutput();
            ttmp.delete();
            File vtmp = constructNotMemoryFailingValgrindOutput(testCases);
            vtmp.delete();

        } catch (Exception e) {
            fail("Error creating or parsing mock output file: " + e.getMessage());
        }
        List<TestResult> results = cpar.getTestResults();
        assertEquals("There should only be one test result", 1, results.size());
        TestResult result = results.get(0);
        assertFalse("The test should not be successful", result.passed);
        assertEquals("The test should contain the message: This test should've failed",
            "This test should've failed", result.errorMessage);
        assertEquals("The name of the test should be \"failing\"", "failing", result.name);
    }

    @Test
    public void testParsingWithValgrindOutput() {
        CTestResultParser cpar = null;
        try {
            File ttmp = constructTestOutput(oneOfEachTest);
            File vtmp = constructMemoryFailingValgrindOutput();

            cpar = new CTestResultParser(ttmp, vtmp, null);
            cpar.parseTestOutput();
            vtmp.delete();
            ttmp.delete();
        } catch (Exception e) {
            fail("Error creating or parsing mock output file: " + e.getMessage());
        }
        List<TestResult> results = cpar.getTestResults();
        assertEquals("There should be two test results", 2, results.size());
        assertNotNull("Valgrind errors should go in backtrace",
                results.get(0).backtrace);
        assertTrue("Valgrind errors should go in backtrace",
                results.get(0).backtrace.contains("==1== 1"));
        assertEquals("Valgrind output should go into backtrace if there were not errors", 0,
                results.get(1).backtrace.size());
    }

    @Test
    public void testTestsPassWhenNoMemoryErrors() {
        CTestResultParser cpar = null;
        try {
            File ttmp = constructTestOutput(oneOfEachTest);
            File vtmp = constructNotMemoryFailingValgrindOutput(oneOfEachTest);

            cpar = new CTestResultParser(ttmp, vtmp, null);
            cpar.parseTestOutput();
            vtmp.delete();
            ttmp.delete();
        } catch (Exception e) {
            fail("Error creating or parsing mock output file: " + e.getMessage());
        }
        List<TestResult> results = cpar.getTestResults();
        assertEquals("There should be two test results", 2, results.size());
        for (TestResult r : results) {
            assertEquals("Valgrind output should be empty when there was no error", 0,
                    r.backtrace.size());
        }
    }

    private File constructMemoryTestOutput(ArrayList<CTestCase> testCases) throws IOException {
        File tmp = mkTempFile("test_memory", ".txt");
        PrintWriter pw = new PrintWriter(tmp, "UTF-8");
        for (CTestCase t : testCases) {
            pw.println(t.getName() + " " + (t.isCheckedForMemoryLeaks() ? "1" : "0") + " "
                + t.getMaxBytesAllocated());
        }
        pw.close();
        return tmp;
    }

    private File constructTestOutput(ArrayList<CTestCase> testCases) throws IOException {
        File tmp = mkTempFile("test_output", ".xml");
        PrintWriter pw = new PrintWriter(tmp, "UTF-8");
        pw.println("<?xml version=\"1.0\"?>");
        pw.println("<testsuites xmlns=\"http://check.sourceforge.net/ns\">");
        pw.println("  <datetime>2013-02-14 14:57:08</datetime>");
        pw.println("  <suite>");
        pw.println("    <title>tests</title>");
        for (CTestCase t : testCases) {
            pw.println("    <test result=\"" + t.getResult() + "\">");
            pw.println("      <path>.</path>");
            pw.println("      <fn>test.c:1</fn>");
            pw.println("      <id>" + t.getName() + "</id>");
            pw.println("      <iteration>0</iteration>");
            pw.println("      <description>" + t.getName() + "</description>");
            pw.println("      <message>" + t.getMessage() + "</message>");
            pw.println("    </test>");
        }

        pw.println("  </suite>");
        pw.println("  <duration>0.000000</duration>");
        pw.println("</testsuites>");
        pw.flush();
        pw.close();
        return tmp;
    }

    private File constructNotMemoryFailingValgrindOutput(ArrayList<CTestCase> testCases)
        throws IOException {
        File tmp = mkTempFile("valgrind", ".log");
        PrintWriter pw = new PrintWriter(tmp);
        pw.println("==" + testCases.size() * 2 + 1 + "== Main process");
        int counter = 2;
        for (CTestCase t : testCases) {
            pw.println("==" + counter * 2 + "== " + (counter - 1));
            pw.println("Some crap that should be ignored");
            pw.println("==" + counter * 2 + "== ERROR SUMMARY: 0 errors from 0 contexts");
            pw.println("==" + counter * 2 + "== LEAK SUMMARY:");
            pw.println("==" + counter * 2 + "==   definitely lost: 0 bytes in 0 blocks");
            counter++;
        }
        pw.println("==" + testCases.size() * 2 + 1 + "== Done");

        pw.flush();
        pw.close();
        return tmp;
    }

    private File constructMemoryFailingValgrindOutput() throws IOException {
        File tmp = mkTempFile("valgrind", ".log");
        PrintWriter pw = new PrintWriter(tmp);
        pw.println("==10== Main process");
        pw.println("==1== 1");
        pw.println("Some crap that should be ignore");
        pw.println("==1== ERROR SUMMARY: 1 errors from 1 contexts");
        pw.println("==1== LEAK SUMMARY:");
        pw.println("==1==   definitely lost: 0 bytes in 0 blocks");
        pw.println("==1== HEAP SUMMARY:");
        pw.println("==1==   total heap usage: 624 allocs, 237 frees, 0 bytes allocated");
        pw.println("==2== 2");
        pw.println("Some crap that should be ignore");
        pw.println("==2== ERROR SUMMARY: 0 errors from 0 contexts");
        pw.println("==2== LEAK SUMMARY:");
        pw.println("==2==   definitely lost: 124 bytes in 3 blocks");
        pw.println("==2== HEAP SUMMARY:");
        pw.println("==2==   total heap usage: 624 allocs, 237 frees, 0 bytes allocated");
        pw.println("==3== 3");
        pw.println("Some crap that should be ignore");
        pw.println("==3== ERROR SUMMARY: 0 errors from 0 contexts");
        pw.println("==3== LEAK SUMMARY:");
        pw.println("==3==   definitely lost: 0 bytes in 0 blocks");
        pw.println("==3== HEAP SUMMARY:");
        pw.println("==3==   total heap usage: 624 allocs, 237 frees, 100 bytes allocated");
        pw.println("==10== Done");

        pw.flush();
        pw.close();
        return tmp;
    }

    private File emptyValgrindOutput() throws IOException {
        File tmp = mkTempFile("valgrind", ".log");
        PrintWriter pw = new PrintWriter(tmp);
        pw.println("Nothing");
        pw.flush();
        pw.close();
        return tmp;
    }

    private File mkTempFile(String prefix, String suffix) throws IOException {
        File tmp = File.createTempFile(prefix, suffix);
        tmp.deleteOnExit();
        return tmp;
    }

    private File tmpFolder() {
        return new File(System.getProperty("java.io.tmpdir"));
    }
}