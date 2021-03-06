package fi.helsinki.cs.tmc.langs.utils;

public final class ProcessResult {
    public final int statusCode;
    public final String output;
    public final String errorOutput;

    /**
     * This is returned by Process Runner and has information about the result of the process.
     */
    public ProcessResult(int statusCode, String output, String errorOutput) {
        this.statusCode = statusCode;
        this.output = output;
        this.errorOutput = errorOutput;
    }
}