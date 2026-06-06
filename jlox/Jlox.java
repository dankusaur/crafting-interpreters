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
        final ExecutionMode executionMode = executionMode(args);
        final Optional<String> scriptPath = getScriptPath(args);

        final Consumer<String> executor = getExecutor(executionMode, scriptPath.isPresent());

        if (scriptPath.isPresent()) {
            runFile(scriptPath.get(), executor);
        } else {
            runPrompt(executor);
        }
    }

    private static ExecutionMode executionMode(String[] args) {
        for (final String arg : args) {
            if (!arg.startsWith("--")) {
                continue;
            }
            final String option = arg.substring(2);
            try {
                return ExecutionMode.from(option);
            } catch (IllegalArgumentException _) {
                System.out.println("Unknown option provided: " + option);
                exitWithHelp();
            }
        }
        return ExecutionMode.EXECUTE;
    }

    private static Consumer<String> getExecutor(final ExecutionMode executionMode, final boolean filePathBased) {
        if (filePathBased) {
            return getFilePathExecutor(executionMode);
        } else {
            return getReplExecutor(executionMode);
        }
    }

    private static Consumer<String> getFilePathExecutor(final ExecutionMode executionMode) {
        final StandardErrorReporter errorReporter = new StandardErrorReporter();
        final Function<String, List<Stmt>> sharedParsing = (program) -> {
            final Scanner scanner = new Scanner(program, errorReporter);
            final List<Token> tokens = scanner.scanTokens();
            final Parser parser = new Parser(tokens, errorReporter);
            final List<Stmt> statements = parser.parse();
            if (errorReporter.hadError) {
                System.exit(65);
            }
            return statements;
        };
        switch (executionMode) {
            case EXECUTE:
                return (program) -> {
                    final List<Stmt> statements = sharedParsing.apply(program);
                    final Interpreter interpreter = new Interpreter(errorReporter);
                    interpreter.interpret(statements);
                    if (errorReporter.hadRuntimeError) {
                        System.exit(70);
                    }
                };
            case PRINT:
                return (program) -> {
                    final List<Stmt> statements = sharedParsing.apply(program);
                    final AstPrinter printer = new AstPrinter();
                    printer.print(statements);
                };
            default:
                throw new IllegalStateException("Unkown execution mode");
        }
    }

    private static Consumer<String> getReplExecutor(final ExecutionMode executionMode) {
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
        switch (executionMode) {
            case EXECUTE:
                return (statementOrExpr) -> {
                    final Optional<List<Stmt>> singletonStatement = sharedParsing.apply(statementOrExpr);
                    final Interpreter interpreter = new Interpreter(standardErrorReporter);
                    if (singletonStatement.isPresent()) {
                        interpreter.interpret(singletonStatement.get());
                    } else {
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
            case PRINT:
                return (statementOrExpr) -> {
                    final Optional<List<Stmt>> singletonStatement = sharedParsing.apply(statementOrExpr);
                    final AstPrinter printer = new AstPrinter();
                    if (singletonStatement.isPresent()) {
                        printer.print(singletonStatement.get());
                    } else {
                        final Optional<Expr> expression = fallbackParsing.apply(statementOrExpr);
                        if (expression.isPresent()) {
                            printer.printExpression(expression.get());
                        } else {
                            delayedErrorReporter.flush();
                        }
                    }
                };
            default:
                throw new IllegalStateException("Unkown execution mode");
        }
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
