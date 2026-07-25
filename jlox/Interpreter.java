package jlox;

import java.util.List;

import jlox.Environment.PrimitiveValue;
import jlox.Expr.Assign;
import jlox.Expr.Binary;
import jlox.Expr.Grouping;
import jlox.Expr.Literal;
import jlox.Expr.Logical;
import jlox.Expr.Ternary;
import jlox.Expr.Unary;
import jlox.Expr.Variable;
import jlox.Stmt.Block;
import jlox.Stmt.Expression;
import jlox.Stmt.If;
import jlox.Stmt.Print;
import jlox.Stmt.VarStmt;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private final ErrorReporter errorReporter;

    private Environment environment = new Environment();

    Interpreter(final ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    void interpret(final List<Stmt> statements) {
        try {
            for (final Stmt statement: statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            errorReporter.runtimeError(error);
        }
    }

    Object interpretExpression(final Expr expr) {
        try {
            return evaluate(expr);
        } catch (RuntimeError error) {
            errorReporter.runtimeError(error);
        }
        return null;
    }

    @Override
    public Void visitExpression(final Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrint(final Print stmt) {
        final Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(final VarStmt stmt) {
        Object initialValue = PrimitiveValue.UNINITIALIZED;
        if (stmt.initializer != null) {
            initialValue = evaluate(stmt.initializer);
        }
        environment.define(stmt.var.lexeme, initialValue);
        return null;
    }

    @Override
    public Void visitBlock(Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Object visitAssign(final Assign expr) {
        final Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitVariable(final Variable expr) {
        final Object value = environment.get(expr.name);
        if (value == PrimitiveValue.UNINITIALIZED) {
            throw new RuntimeError(expr.name, "Variable '" + expr.name.lexeme + "' " + "is not initialized.");
        }
        return value;
    }

    @Override
    public Object visitTernary(final Ternary expr) {
        Object condition = evaluate(expr.condition);
        if (isTruthy(condition)) {
            return evaluate(expr.thenBranch);
        }
        return evaluate(expr.elseBranch);
    }

    @Override
    public Object visitBinary(final Binary expr) {
        final Object left = evaluate(expr.left);
        final Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }
                if (left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }
                throw new RuntimeError(expr.operator, "Operands can only consist of numbers or strings.");
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if ((double) right == 0) {
                    throw new RuntimeError(expr.operator, "Division by zero.");
                }
                return (double) left / (double) right;
            case GREATER:
                checkComparableOperands(expr.operator, left, right);
                return compare(left, right) > 0;
            case GREATER_EQUAL:
                checkComparableOperands(expr.operator, left, right);
                return compare(left, right) >= 0;
            case LESS:
                checkComparableOperands(expr.operator, left, right);
                return compare(left, right) < 0;
            case LESS_EQUAL:
                checkComparableOperands(expr.operator, left, right);
                return compare(left, right) <= 0;
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case BANG_EQUAL:
                return !isEqual(left, right);
            default:
                break;
        }
        throw new AssertionError("Operand with no handling in runtime.");
    }

    @Override
    public Object visitGrouping(final Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteral(final Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnary(final Unary expr) {
        final Object value = evaluate(expr.right);
        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, value);
                return -(double) value;
            case BANG:
                return !isTruthy(value);
            default:
                break;
        }
        // Unreachable.
        return null;
    }

    private void execute(final Stmt statement) {
        statement.accept(this);
    }

    private Object evaluate(final Expr expression) {
        return expression.accept(this);
    }

    private boolean isTruthy(final Object object) {
        if (object == null) {
            return false;
        }
        if (object instanceof Boolean) {
            return (boolean) object;
        }
        return false;
    }

    private boolean isEqual(final Object object1, final Object object2) {
        if (object1 == null && object2 == null) {
            return true;
        }
        if (object1 == null) {
            return false;
        }
        return object1.equals(object2);
    }

    private void checkNumberOperand(final Token operator, final Object operand) {
        if (operand instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(final Token operator, final Object left, final Object right) {
        if (left instanceof Double && right instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private void checkComparableOperands(final Token operator,
            final Object left, final Object right) {
        if (left instanceof Double && right instanceof Double) {
            return;
        }
        if (left instanceof String && right instanceof String) {
            return;
        }
        throw new RuntimeError(operator, "Operands must be of a comparable type: numbers or strings.");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private int compare(Object left, Object right) {
        return ((Comparable) left).compareTo(right);
    }

    private String stringify(final Object value) {
        if (value == null) {
            return "nil";
        }
        if (value instanceof Double) {
            final String text = value.toString();
            if (text.endsWith(".0")) {
                return text.substring(0, text.length() - 2);
            }
        }
        return value.toString();
    }

    private void executeBlock(final List<Stmt> statements, final Environment enclosedEnv) {
        final Environment enclosing = environment;
        try {
            environment = enclosedEnv;
            for (final Stmt statement: statements) {
                execute(statement);
            }
        } finally {
            environment = enclosing;
        }
    }

    @Override
    public Void visitIf(final If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Object visitLogical(final Logical expr) {
        final Object left = evaluate(expr.left);
        System.out.println("evaluation: " + left.toString());
        switch (expr.operator.type) {
            case OR:
                return isTruthy(left) ? left : evaluate(expr.right);
            case AND:
                return !isTruthy(left) ? left : evaluate(expr.right);
            default:
                break;
        }
        // Unreachable
        return null;
    }
}
