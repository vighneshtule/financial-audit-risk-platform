package com.vighnesh.controller;

public class ImportResponse {

    private final String message;
    private final int count;

    public ImportResponse(String message, int count) {
        this.message = message;
        this.count = count;
    }

    public String getMessage() {
        return message;
    }

    public int getCount() {
        return count;
    }
}
