package jlox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jlox.ErrorReporter.StandardErrorReporter;

import static jlox.TokenType.*;

public class Scanner {

    private static final Map<String, TokenType> keywords = new HashMap<>();
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 0;

    private final ErrorReporter errorReporter = new StandardErrorReporter();

    static {
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
    }

    Scanner(final String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        final char c = advance();
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '?': addToken(QUESTION_MARK); break;
            case ':': addToken(COLON); break;
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '/':
                if (match('/')) {
                    lineComment();
                } else if (match('*')) {
                    blockComment();
                } else {
                    addToken(SLASH);
                }
                break;
            case '"':
                string();
                break;
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                ++line;
                break;
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    errorReporter.syntaxError(line, "Unexpected character.");
                }
                break;
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }
        final String word = source.substring(start, current);
        final TokenType type = keywords.getOrDefault(word, IDENTIFIER);
        addToken(type);
    }

    private void number() {
        consumeDigits();
        // Handle decimal literals.
        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            consumeDigits();
        }
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void consumeDigits() {
        while (isDigit(peek())) {
            advance();
        }
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                ++line;
            }
            advance();
        }
        if (isAtEnd()) {
            errorReporter.syntaxError(line, "Unterminated string.");
            return;
        }

        // Consume the closing quote.
        advance();
        // Trim the quotes.
        final String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private void lineComment() {
        while (peek() != '\n' && !isAtEnd()) {
            advance();
        }
        addToken(COMMENT);
    }

    private void blockComment() {
        while (peek() != '*' && peekNext() != '/' && !isAtEnd()) {
            if (peek() == '\n') {
                ++line;
            }
            advance();
        }
        if (isAtEnd()) {
            errorReporter.syntaxError(line, "Unterminated block comment.");
            return;
        }

        // Consume the terminating asterisk forward slash.
        advance();
        advance();
        addToken(COMMENT);
    }

    private boolean match(char expected) {
        if (peek() != expected) {
            return false;
        }
        advance();
        return true;
    }

    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(current + 1);
    }

    private char advance() {
        final char nextChar = source.charAt(current);
        ++current;
		return nextChar;
	}

    private void addToken(TokenType type) {
		addToken(type, null);
	}

	private void addToken(TokenType type, Object literal) {
		final String lexeme = source.substring(start, current);
        tokens.add(new Token(type, lexeme, literal, line));
	}

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isDigit(c) || isAlpha(c);
    }

	private boolean isAtEnd() {
        return current >= source.length();
    }
}
