package com.thuanbui.foundry.services;

public class FeatureDisabledException extends RuntimeException {
    public FeatureDisabledException(String featureName) {
        super("Feature '" + featureName + "' is currently disabled");
    }
}
