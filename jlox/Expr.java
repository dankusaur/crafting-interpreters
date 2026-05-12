package jlox;

abstract class Expr {

    abstract <R> R accept(Visitor<R> visitor);

    interface Visitor<R> {
        R visitAssign(Assign expr);
        R visitTernary(Ternary expr);
        R visitBinary(Binary expr);
        R visitGrouping(Grouping expr);
        R visitLiteral(Literal expr);
        R visitUnary(Unary expr);
        R visitVariable(Variable expr);
    }

    static class Assign extends Expr {
        final Token name;
        final Expr value;

        Assign(final Token name, final Expr value) {
            this.name = name;
            this.value = value;
        }

        @Override
        <R> R accept(final Visitor<R> visitor) {
            return visitor.visitAssign(this);
        }
    }

    static class Ternary extends Expr {
        final Expr condition;
        final Expr thenBranch;
        final Expr elseBranch;

        Ternary(final Expr condition, final Expr thenBranch, final Expr elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        <R> R accept(final Visitor<R> visitor) {
            return visitor.visitTernary(this);
        }
    }

    static class Binary extends Expr {
        final Expr left;
        final Token operator;
        final Expr right;

        Binary(final Expr left, final Token operator, final Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R accept(final Visitor<R> visitor) {
            return visitor.visitBinary(this);
        }
    }

    static class Grouping extends Expr {
        final Expr expression;

        Grouping(final Expr expression) {
            this.expression = expression;
        }

        @Override
        <R> R accept(final Visitor<R> visitor) {
            return visitor.visitGrouping(this);
        }
    }

    static class Literal extends Expr {
        final Object value;

        Literal(final Object value) {
            this.value = value;
        }

        @Override
        <R> R accept(final Visitor<R> visitor) {
            return visitor.visitLiteral(this);
        }
    }

    static class Unary extends Expr {
        final Token operator;
        final Expr right;

        Unary(final Token operator, final Expr right) {
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R accept(final Visitor<R> visitor) {
            return visitor.visitUnary(this);
        }
    }

    static class Variable extends Expr {
        final Token name;

        Variable(final Token name) {
            this.name = name;
        }

        @Override
        <R> R accept(final Visitor<R> visitor) {
            return visitor.visitVariable(this);
        }
    }
}
