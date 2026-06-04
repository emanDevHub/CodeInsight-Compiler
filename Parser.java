import java.util.*;

public class Parser {

    private final ArrayList<Token> tokens;
    private int current = 0;
    private final ArrayList<String> errors = new ArrayList<>();

    public Parser(ArrayList<Token> tokens) {
        this.tokens = tokens;
    }

    public ArrayList<String> getErrors() {
        return errors;
    }

    private Token currentToken() {
        if (current >= tokens.size()) {
            return new Token("EOF", "EOF");
        }
        return tokens.get(current);
    }

    private void advance() {
        current++;
    }

    private void match(String expected) {
        if (currentToken().value.equals(expected)) {
            advance();
        } else {
            errors.add("Expected: '" + expected + "'  but Found: '" + currentToken().value + "'");
            // skip ahead to recover and continue finding more errors
            if (!currentToken().value.equals("EOF")) {
                advance();
            }
        }
    }

    public void parse() {
        if (tokens.isEmpty()) {
            errors.add("No tokens found. Please Scan first.");
            return;
        }
        for (Token t : tokens) {
            if ("Invalid Token".equals(t.type)) {
                errors.add("Invalid token found: '" + t.value + "'");
            }
        }

        classRule();

        if (current < tokens.size()) {
            errors.add("Unexpected token at end: '" + currentToken().value + "'");
        }
    }

    // class -> class_signature { data_declaration constructor method }
    private void classRule() {
        classSignature();
        match("{");

        // Keep processing data declarations, and also catch incomplete ones
        boolean keepGoing = true;
        while (keepGoing) {
            if (isDataDeclaration()) {
                dataDeclaration();
            } else if (isIncompleteDataDeclaration()) {
                // e.g. private static count;  (missing data type like int)
                int save = current;
                accessModifierSilent();
                if (currentToken().value.equals("static")) advance();
                // next should be data type but it's an identifier directly
                errors.add("Data type (int/byte/short/long) missing before '" + currentToken().value + "'");
                // skip until semicolon to recover
                while (!currentToken().value.equals(";") && !currentToken().value.equals("EOF")) advance();
                if (currentToken().value.equals(";")) advance();
            } else if (isMissingSemicolonDeclaration()) {
                // e.g. private static int count  (missing semicolon)
                accessModifierSilent();
                if (currentToken().value.equals("static")) advance();
                dataTypeSilent();
                String varName = currentToken().value;
                identifierSilent();
                errors.add("Missing ';' after variable declaration '" + varName + "'");
            } else {
                keepGoing = false;
            }
        }

        if (isConstructor()) {
            constructor();
        } else {
            errors.add("Constructor expected but found: '" + currentToken().value + "'");
        }

        while (isMethod()) {
            method();
        }

        match("}");
    }

    // Detects: access_modifier [static] identifier ; (missing data type)
    private boolean isIncompleteDataDeclaration() {
        if (!isAccessModifier(currentToken().value)) return false;
        int save = current;
        try {
            accessModifierSilent();
            if (currentToken().value.equals("static")) advance();
            // identifier directly (no data type before it)
            if (!currentToken().type.equals("Identifier")) return false;
            identifierSilent();
            return currentToken().value.equals(";");
        } finally {
            current = save;
        }
    }

    private boolean isDataDeclaration() {
        if (!isAccessModifier(currentToken().value)) {
            return false;
        }
        int save = current;
        try {
            accessModifierSilent();
            if (currentToken().value.equals("static")) {
                advance();
            }
            if (!isDataType(currentToken().value)) {
                return false;
            }
            dataTypeSilent();
            identifierSilent();
            return currentToken().value.equals(";");
        } finally {
            current = save;
        }
    }

    // Detects: access_modifier [static] data_type identifier (missing semicolon)
    private boolean isMissingSemicolonDeclaration() {
        if (!isAccessModifier(currentToken().value)) return false;
        int save = current;
        try {
            accessModifierSilent();
            if (currentToken().value.equals("static")) advance();
            if (!isDataType(currentToken().value)) return false;
            dataTypeSilent();
            if (!currentToken().type.equals("Identifier")) return false;
            identifierSilent();
            // semicolon NOT present — next is something else (like public/private)
            return !currentToken().value.equals(";");
        } finally {
            current = save;
        }
    }

    private boolean isConstructor() {
        if (!isAccessModifier(currentToken().value)) return false;
        int save = current;
        try {
            accessModifierSilent();
            if (!currentToken().type.equals("Identifier")) return false;
            identifierSilent();
            return currentToken().value.equals("(");
        } finally {
            current = save;
        }
    }

    private boolean isMethod() {
        if (!isAccessModifier(currentToken().value)) {
            return false;
        }
        int save = current;
        try {
            accessModifierSilent();
            if (currentToken().value.equals("void")) {
                advance();
            } else if (isDataType(currentToken().value)) {
                dataTypeSilent();
            } else {
                return false;
            }
            identifierSilent();
            return currentToken().value.equals("(");
        } finally {
            current = save;
        }
    }

    // Silent helpers for lookahead (no error recording)
    private void accessModifierSilent() { if (isAccessModifier(currentToken().value)) advance(); }
    private void dataTypeSilent()       { if (isDataType(currentToken().value)) advance(); }
    private void identifierSilent()     { if (currentToken().type.equals("Identifier")) advance(); }

    private void classSignature() {
        match("public");
        match("class");
        identifier();
    }

    // data_declaration -> access_modifier [static] data_type identifier;
    private void dataDeclaration() {
        accessModifier();
        if (currentToken().value.equals("static")) {
            advance();
        }
        dataType();
        identifier();
        match(";");
    }

    // constructor -> access_modifier identifier ( parameterList ) { statements }
    private void constructor() {
        accessModifier();
        identifier();
        match("(");
        parameterList();
        match(")");
        match("{");
        statements();
        match("}");
    }

    // method -> access_modifier returnType identifier ( parameterList ) { statements returnStatement }
    private void method() {
        accessModifier();
        returnType();
        identifier();
        match("(");
        parameterList();
        match(")");
        match("{");
        statements();
        returnStatement();
        match("}");
    }

    // statements -> assignment | function_call | if | localDecl | e
    private void statements() {
        while (isStatement()) {
            if (currentToken().value.equals("if")) {
                ifCondition();
            } else if (isDataType(currentToken().value)) {
                localDeclaration();
                match("=");
                expression();
                match(";");
            } else if (
                currentToken().value.equals("this")
                || currentToken().type.equals("Identifier")
            ) {
                int save = current;
                advance();
                if (currentToken().value.equals("=") || currentToken().value.equals(".")) {
                    current = save;
                    assignment();
                } else if (currentToken().value.equals("(")) {
                    current = save;
                    functionCall();
                } else {
                    errors.add("Invalid statement near: '" + currentToken().value + "'");
                    advance();
                }
            }
        }
    }

    private boolean isStatement() {
        String v = currentToken().value;
        return v.equals("if")
            || isDataType(v)
            || v.equals("this")
            || currentToken().type.equals("Identifier");
    }

    // assignment -> [this.] identifier = expression;
    private void assignment() {
        if (currentToken().value.equals("this")) {
            match("this");
            match(".");
        }
        identifier();
        match("=");
        expression();
        match(";");
    }

    // expression -> operand (arith_op operand)*
    private void expression() {
        operand();
        while (isArithOperator(currentToken().value)) {
            advance();
            operand();
        }
    }

    private void operand() {
        if (currentToken().type.equals("Identifier")) {
            identifier();
        } else if (currentToken().type.equals("Number")) {
            advance();
        } else {
            errors.add("Invalid expression — expected identifier or number, found: '" + currentToken().value + "'");
            if (!currentToken().value.equals("EOF")) advance();
        }
    }

    // function_call -> [this.] identifier ( argumentList );
    private void functionCall() {
        if (currentToken().value.equals("this")) {
            match("this");
            match(".");
        }
        identifier();
        match("(");
        argumentList();
        match(")");
        match(";");
    }

    // if_condition -> if ( predicate ) { statements }
    private void ifCondition() {
        match("if");
        match("(");
        predicate();
        match(")");
        match("{");
        statements();
        match("}");
    }

    // predicate -> identifier rel_op (identifier | number)
    private void predicate() {
        identifier();
        relOperator();
        if (currentToken().type.equals("Identifier") || currentToken().type.equals("Number")) {
            advance();
        } else {
            errors.add("Invalid predicate — expected identifier or number after operator, found: '" + currentToken().value + "'");
            if (!currentToken().value.equals("EOF")) advance();
        }
    }

    // return_statement -> return [identifier | number] ; | e
    private void returnStatement() {
        if (currentToken().value.equals("return")) {
            match("return");
            if (currentToken().type.equals("Identifier") || currentToken().type.equals("Number")) {
                advance();
            }
            match(";");
        }
    }

    // parameter_list -> parameter (, parameter)* | e
    private void parameterList() {
        if (isDataType(currentToken().value)) {
            parameter();
            while (currentToken().value.equals(",")) {
                match(",");
                parameter();
            }
        }
    }

    private void parameter() {
        dataType();
        identifier();
    }

    // argument_list -> identifier (, identifier)* | e
    private void argumentList() {
        if (currentToken().type.equals("Identifier")) {
            identifier();
            while (currentToken().value.equals(",")) {
                match(",");
                identifier();
            }
        }
    }

    // local_declaration -> data_type identifier
    private void localDeclaration() {
        dataType();
        identifier();
    }

    // return_type -> void | data_type
    private void returnType() {
        if (currentToken().value.equals("void")) {
            match("void");
        } else {
            dataType();
        }
    }

    private void relOperator() {
        String op = currentToken().value;
        if (op.equals("<") || op.equals("<=") || op.equals(">")
            || op.equals(">=") || op.equals("==") || op.equals("!=")) {
            advance();
        } else {
            errors.add("Relational operator expected, found: '" + op + "'");
            if (!currentToken().value.equals("EOF")) advance();
        }
    }

    private void accessModifier() {
        if (isAccessModifier(currentToken().value)) {
            advance();
        } else {
            errors.add("Access modifier (public/private) expected, found: '" + currentToken().value + "'");
            if (!currentToken().value.equals("EOF")) advance();
        }
    }

    private boolean isAccessModifier(String t) {
        return t.equals("public") || t.equals("private");
    }

    private void dataType() {
        if (isDataType(currentToken().value)) {
            advance();
        } else {
            errors.add("Data type (int/byte/short/long) expected, found: '" + currentToken().value + "'");
            if (!currentToken().value.equals("EOF")) advance();
        }
    }

    private boolean isDataType(String t) {
        return t.equals("byte") || t.equals("short")
            || t.equals("int") || t.equals("long");
    }

    private boolean isArithOperator(String t) {
        return t.equals("+") || t.equals("-") || t.equals("*")
            || t.equals("/") || t.equals("%");
    }

    private void identifier() {
        if (currentToken().type.equals("Identifier")) {
            advance();
        } else {
            errors.add("Identifier expected, found: '" + currentToken().value + "'");
            if (!currentToken().value.equals("EOF")) advance();
        }
    }
}
