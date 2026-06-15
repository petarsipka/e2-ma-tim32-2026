package com.example.slagalica.util;

import java.util.ArrayList;
import java.util.List;

public class ExpressionEvaluator {

    public static class Result {
        public final int value;
        public final boolean valid;
        public final String error;
        public final List<Integer> usedNumbers;

        public Result(int value, boolean valid, String error, List<Integer> usedNumbers) {
            this.value = value;
            this.valid = valid;
            this.error = error;
            this.usedNumbers = usedNumbers;
        }
    }

    public static Result evaluate(String expression, List<Integer> availableNumbers) {
        if (expression == null || expression.trim().isEmpty()) {
            return new Result(0, false, "Prazan izraz", new ArrayList<>());
        }

        String clean = expression.replaceAll("\\s+", "");
        List<Token> tokens = tokenize(clean);
        if (tokens.isEmpty()) {
            return new Result(0, false, "Prazan izraz", new ArrayList<>());
        }

        List<Integer> usedNumbers = new ArrayList<>();
        for (Token t : tokens) {
            if (t.type == TokenType.NUMBER) usedNumbers.add(t.value);
        }

        List<Integer> availableCopy = new ArrayList<>(availableNumbers);
        for (int num : usedNumbers) {
            if (!availableCopy.remove((Integer) num)) {
                return new Result(0, false, "Broj već iskorišćen: " + num, usedNumbers);
            }
        }

        try {
            Parser parser = new Parser(tokens);
            int value = parser.parse();
            return new Result(value, true, null, usedNumbers);
        } catch (Exception e) {
            return new Result(0, false, "Nevalidan izraz", usedNumbers);
        }
    }

    private static List<Token> tokenize(String s) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                int j = i;
                while (j < s.length() && Character.isDigit(s.charAt(j))) j++;
                tokens.add(new Token(TokenType.NUMBER, Integer.parseInt(s.substring(i, j))));
                i = j;
            } else if (c == '+') { tokens.add(new Token(TokenType.PLUS, 0)); i++;
            } else if (c == '-') { tokens.add(new Token(TokenType.MINUS, 0)); i++;
            } else if (c == '*' || c == '×') { tokens.add(new Token(TokenType.MULTIPLY, 0)); i++;
            } else if (c == '/' || c == '÷') { tokens.add(new Token(TokenType.DIVIDE, 0)); i++;
            } else if (c == '(') { tokens.add(new Token(TokenType.LPAREN, 0)); i++;
            } else if (c == ')') { tokens.add(new Token(TokenType.RPAREN, 0)); i++;
            } else { i++; }
        }
        return tokens;
    }

    private enum TokenType { NUMBER, PLUS, MINUS, MULTIPLY, DIVIDE, LPAREN, RPAREN }

    private static class Token {
        final TokenType type;
        final int value;
        Token(TokenType type, int value) { this.type = type; this.value = value; }
    }

    private static class Parser {
        final List<Token> tokens;
        int pos = 0;
        Parser(List<Token> tokens) { this.tokens = tokens; }

        int parse() {
            int value = parseExpression();
            if (pos < tokens.size()) throw new RuntimeException("Unexpected token");
            return value;
        }

        int parseExpression() {
            int value = parseTerm();
            while (pos < tokens.size()) {
                Token t = tokens.get(pos);
                if (t.type == TokenType.PLUS) { pos++; value += parseTerm(); }
                else if (t.type == TokenType.MINUS) { pos++; value -= parseTerm(); }
                else break;
            }
            return value;
        }

        int parseTerm() {
            int value = parseFactor();
            while (pos < tokens.size()) {
                Token t = tokens.get(pos);
                if (t.type == TokenType.MULTIPLY) { pos++; value *= parseFactor(); }
                else if (t.type == TokenType.DIVIDE) {
                    pos++;
                    int d = parseFactor();
                    if (d == 0) throw new RuntimeException("Division by zero");
                    value /= d;
                } else break;
            }
            return value;
        }

        int parseFactor() {
            if (pos >= tokens.size()) throw new RuntimeException("Unexpected end");
            Token t = tokens.get(pos);
            if (t.type == TokenType.NUMBER) { pos++; return t.value; }
            else if (t.type == TokenType.LPAREN) {
                pos++;
                int value = parseExpression();
                if (pos >= tokens.size() || tokens.get(pos).type != TokenType.RPAREN)
                    throw new RuntimeException("Missing )");
                pos++;
                return value;
            } else if (t.type == TokenType.MINUS) {
                pos++;
                return -parseFactor();
            } else {
                throw new RuntimeException("Unexpected token");
            }
        }
    }
}