package jlox;

import java.util.ArrayList;
import java.util.List;

interface ErrorReporter {

    void syntaxError(final int line, final String message);

    void syntaxError(final Token token, final String message);

    void runtimeError(final RuntimeError error);

    class StandardErrorReporter implements ErrorReporter {

        boolean hadError = false;
        boolean hadRuntimeError = false;

        public void syntaxError(final int line, final String message) {
            report(line, "", message);
        }

        public void syntaxError(final Token token, final String message) {
            if (token.type == TokenType.EOF) {
                report(token.line, " at end", message);
            } else {
                report(token.line, " at '" + token.lexeme + "'", message);
            }
        }

        public void runtimeError(final RuntimeError error) {
            System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
            hadRuntimeError = true;
        }

        private void report(final int line, final String where, final String message) {
            System.err.println("[line" + line + "] Error" + where + ": " + message);
            hadError = true;
        }
    }

    class DelayedErrorReporter implements ErrorReporter {

        private List<String> errors = new ArrayList<>();

        boolean hadError = false;
        boolean hadRuntimeError = false;

        public void syntaxError(final int line, final String message) {
            report(line, "", message);
        }

        public void syntaxError(final Token token, final String message) {
            if (token.type == TokenType.EOF) {
                report(token.line, " at end", message);
            } else {
                report(token.line, " at '" + token.lexeme + "'", message);
            }
        }

        public void runtimeError(final RuntimeError error) {
            final String errorMessage = error.getMessage() + "\n[line " + error.token.line + "]";
            errors.add(errorMessage);
            hadRuntimeError = true;
        }

        public void flush() {
            for (final String errorMessage: errors) {
                System.err.println(errorMessage);
            }
            hadError = false;
            hadRuntimeError = false;
        }

        public void clear() {
            errors.clear();
            hadError = false;
            hadRuntimeError = false;
        }

        private void report(final int line, final String where, final String message) {
            final String errorMessage = "[line" + line + "] Error" + where + ": " + message;
            errors.add(errorMessage);
            hadError = true;
        }
    }
}
