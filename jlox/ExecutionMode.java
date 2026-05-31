package jlox;

enum ExecutionMode {
    EXECUTE,
    PRINT;

    static ExecutionMode from(String value) {
		return ExecutionMode.valueOf(value.toUpperCase());
    }
}
