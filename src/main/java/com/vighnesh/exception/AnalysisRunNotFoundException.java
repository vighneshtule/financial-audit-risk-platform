package com.vighnesh.exception;

public class AnalysisRunNotFoundException extends RuntimeException {

    public AnalysisRunNotFoundException(long analysisRunId) {
        super("Analysis run not found: " + analysisRunId);
    }
}
