package jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import jlox.ErrorReporter.DelayedErrorReporter;
import jlox.ErrorReporter.StandardErrorReporter;

/**
 * Currently doing 3 jobs of parsing args, determining executors, and error reporting.
 */
class Jlox {

    private static final String EXIT = "exit";
    private static boolean hadError = false;
    private static boolean hadRuntimeError = false;

    public static void main(final String[] args) throws IOException {
        if (args.length > 2) {
            exitWithHelp();
        }
        final Optional<String> scriptPath = getScriptPath(args);

        final Consumer<String> executor = getExecutor(scriptPath.isPresent());

        if (scriptPath.isPresent()) {
            runFile(scriptPath.get(), executor);
        } else {
            runPrompt(executor);
        }
    }

    private static Consumer<String> getExecutor(final boolean filePathBased) {
        if (filePathBased) {
            return getFilePathExecutor();
        } else {
            return getReplExecutor();
        }
    }

    private static Consumer<String> getFilePathExecutor() {
        final StandardErrorReporter errorReporter = new StandardErrorReporter();
        final Function<String, List<Stmt>> parse = (program) -> {
            final Scanner scanner = new Scanner(program, errorReporter);
            final List<Token> tokens = scanner.scanTokens();
            final Parser parser = new Parser(tokens, errorReporter);
            final List<Stmt> statements = parser.parse();
            if (errorReporter.hadError) {
                System.exit(65);
            }
            return statements;
        };
        return (program) -> {
            final List<Stmt> statements = parse.apply(program);
            final Interpreter interpreter = new Interpreter(errorReporter);
            interpreter.interpret(statements);
            if (errorReporter.hadRuntimeError) {
                System.exit(70);
            }
        };
    }

    private static Consumer<String> getReplExecutor() {
        final DelayedErrorReporter delayedErrorReporter = new DelayedErrorReporter();
        final StandardErrorReporter standardErrorReporter = new StandardErrorReporter();
        final Function<String, Optional<List<Stmt>>> sharedParsing = (statementOrExpr) -> {
            final Scanner scanner = new Scanner(statementOrExpr, delayedErrorReporter);
            final List<Token> tokens = scanner.scanTokens();
            final Parser parser = new Parser(tokens, delayedErrorReporter);
            final List<Stmt> statements = parser.parse();
            if (delayedErrorReporter.hadError) {
                return Optional.empty();
            }
            return Optional.of(statements);
        };
        final Function<String, Optional<Expr>> fallbackParsing = (statementOrExpr) -> {
            final Scanner scanner = new Scanner(statementOrExpr, delayedErrorReporter);
            final List<Token> tokens = scanner.scanTokens();
            final Parser parser = new Parser(tokens, delayedErrorReporter);
            final Expr expression = parser.parseExpression();
            if (delayedErrorReporter.hadError) {
                return Optional.empty();
            }
            return Optional.of(expression);
        };
        final Interpreter interpreter = new Interpreter(standardErrorReporter);
        return (statementOrExpr) -> {
            final Optional<List<Stmt>> singletonStatement = sharedParsing.apply(statementOrExpr);
            if (singletonStatement.isPresent()) {
                interpreter.interpret(singletonStatement.get());
                standardErrorReporter.clear();
            } else {
                delayedErrorReporter.clear();
                final Optional<Expr> expression = fallbackParsing.apply(statementOrExpr);
                if (expression.isPresent()) {
                    final Object value = interpreter.interpretExpression(expression.get());
                    if (!standardErrorReporter.hadError && !standardErrorReporter.hadRuntimeError) {
                        System.out.println(value);
                    }
                } else {
                    delayedErrorReporter.flush();
                }
            }
        };
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

    private static void runFile(final String path, final Consumer<String> executor) throws IOException {
        final byte[] bytes = Files.readAllBytes(Paths.get(path));
        executor.accept(new String(bytes, Charset.defaultCharset()));
        if (hadError) {
            System.exit(65);
        }
        if (hadRuntimeError) {
            System.exit(70);
        }
    }

    private static void runPrompt(final Consumer<String> executor) throws IOException {
        final InputStreamReader input = new InputStreamReader(System.in);
        final BufferedReader reader = new BufferedReader(input);
        for (;;) {
            System.out.print("> ");
            final String line = reader.readLine();
            if (line == null || EXIT.equals(line) || (EXIT + "()").equals(line)) {
                System.out.println("\nGoodbye.");
                break;
            }
            executor.accept(line);
            hadError = false;
        }
    }
}
