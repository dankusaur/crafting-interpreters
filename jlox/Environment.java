package jlox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, Object> values = new HashMap<>();
    private final Environment enclosing;

    static enum PrimitiveValue {
        UNINITIALIZED
    }

    Environment() {
        enclosing = null;
    }

    Environment(final Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(final String name, final Object value) {
        values.put(name, value);
    }

    void define(final String name) {
        values.put(name, PrimitiveValue.UNINITIALIZED);
    }

    Object get(final Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }
        if (enclosing != null) {
            return enclosing.get(name);
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    public void assign(final Token name, final Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}
