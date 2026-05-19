package jlox;

import java.util.List;

import jlox.Expr.Binary;
import jlox.Expr.Grouping;
import jlox.Expr.Literal;
import jlox.Expr.Ternary;
import jlox.Expr.Unary;
import jlox.Expr.Variable;
import jlox.Stmt.Expression;
import jlox.Stmt.Print;
import jlox.Stmt.VarStmt;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private final Environment environment = new Environment();

    Void interpret(final List<Stmt> statements) {
        try {
            for (final Stmt statement: statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Jlox.runtimeError(error);
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

    @Override
    public Void visitVarStmt(VarStmt stmt) {
        Object initialValue = null;
        if (stmt.initializer != null) {
            initialValue = evaluate(stmt.initializer);
        }
        environment.define(stmt.var.lexeme, initialValue);
        return null;
    }

    @Override
    public Object visitVariable(Variable expr) {
        return environment.get(expr.name);

    }
}
