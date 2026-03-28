package jlox;

import jlox.Expr.Binary;
import jlox.Expr.Grouping;
import jlox.Expr.Literal;
import jlox.Expr.Ternary;
import jlox.Expr.Unary;

public class Interpreter implements Expr.Visitor<Object> {

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
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }
                break;
            case MINUS:
                return (double) left - (double) right;
            case STAR:
                return (double) left * (double) right;
            case SLASH:
                return (double) left / (double) right;
            case GREATER:
                return (double) left > (double) right;
            case GREATER_EQUAL:
                return (double) left >= (double) right;
            case LESS:
                return (double) left < (double) right;
            case LESS_EQUAL:
                return (double) left <= (double) right;
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case BANG_EQUAL:
                return !isEqual(left, right);
            default:
                break;
        }
        // Unreachable?
        return null;
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
                return -(double) value;
            case BANG:
                return !isTruthy(value);
            default:
                break;
        }
        // Unreachable.
        return null;
    }

    private Object evaluate(final Expr expression) {
        return expression.accept(this);
    }

    private boolean isTruthy(Object object) {
        if (object == null) {
            return false;
        }
        if (object instanceof Boolean) {
            return (boolean) object;
        }
        return false;
    }
    
    private boolean isEqual(Object object1, Object object2) {
        if (object1 == null && object2 == null) {
            return true;
        }
        if (object1 == null) {
            return false;
        }
        return object1.equals(object2);
    }
}
