package com.navigation.result;

public class ErrorResult {
    private String errorCode;
    private String errorMessage;

    private ErrorResult(Builder builder) {
        this.errorCode = builder.errorCode;
        this.errorMessage = builder.errorMessage;
    }

    public static class Builder {
        private String errorCode;
        private String errorMessage;

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ErrorResult build() {
            return new ErrorResult(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }


    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}