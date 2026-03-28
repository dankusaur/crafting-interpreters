package jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Currently doing 3 jobs of parsing args, determining runners, and error reporting.
 */
class Jlox {

    private static final String EXIT = "exit";
    private static boolean hadError = false;
    private static boolean hadRuntimeError = false;

    private static final Interpreter interpreter = new Interpreter();

    private static Function<Expr, String> runner;

    public static void main(final String[] args) throws IOException {
        if (args.length > 2) {
            exitWithHelp();
        }
        runner = getRunner(args);
        final Optional<String> scriptPath = getScriptPath(args);
        if (scriptPath.isPresent()) {
            runFile(scriptPath.get());
        }
        runPrompt();
    }

    private static Function<Expr, String> getRunner(String[] args) {
        for (final String arg : args) {
            if (!arg.startsWith("--")) {
                continue;
            }
            final String option = arg.substring(2);
            switch (option) {
                case "print":
                    return (expr) -> new AstPrinter().print(expr);
                case "run":
                    return (expr) -> interpreter.interpret(expr);
                default:
                    System.out.println("Unknown option provided: " + option);
                    exitWithHelp();
            }
        }
        return (expr) -> interpreter.interpret(expr);
    }

    private static Optional<String> getScriptPath(String[] args) {
        for (final String arg : args) {
            if (!arg.startsWith("--")) {
                return Optional.of(arg);
            }
        }
        return Optional.empty();
    }

    private static void exitWithHelp() {
        System.out
                .println("Usage: jlox [script] [OPTIONS]\nOptions:\n --run Execute Lox (default)\n --print Print AST");
        System.exit(64);
    }

    private static void runFile(final String path) throws IOException {
        final byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if (hadError) {
            System.exit(65);
        }
        if (hadRuntimeError) {
            System.exit(70);
        }
    }

    private static void runPrompt() throws IOException {
        final InputStreamReader input = new InputStreamReader(System.in);
        final BufferedReader reader = new BufferedReader(input);
        for (;;) {
            System.out.print("> ");
            final String line = reader.readLine();
            if (line == null || EXIT.equals(line)) {
                System.out.println("\nGoodbye.");
                break;
            }
            run(line);
            hadError = false;
        }
    }

    private static void run(String content) {
        final Scanner scanner = new Scanner(content);
        final List<Token> tokens = scanner.scanTokens();
        final Parser parser = new Parser(tokens);
        final Expr expression = parser.parse();

        if (hadError) {
            return;
        }

        final String value = runner.apply(expression);
        if (!value.isEmpty()) {
            System.out.println(value);
        }
    }

    public static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line" + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadError = true;
    }
}
