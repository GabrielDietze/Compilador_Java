package com.compilador.Execptions;

import com.compilador.Lexica.Token;

public class ErroLexico {
    
    public static void lexicalError(String lexeme, int line, int column) throws ExcecaoCompilador {
    throw new ExcecaoCompilador(buildLexicalMessage(lexeme, line, column));
}

public static void syntaxError(String expected, Token found) throws ExcecaoCompilador {
    throw new ExcecaoCompilador(buildSyntaxMessage(expected, found));
}

public static void syntaxErrorAssignment(Token found) throws ExcecaoCompilador{
    throw new ExcecaoCompilador(buildSyntaxAssignmentMessage(found));
}

public static void semanticErrorAssignment(Token wrongToken, Token declaredToken) throws ExcecaoCompilador{
    throw new ExcecaoCompilador(buildSemanticAssignmentMessage(wrongToken, declaredToken));
}

public static void semanticErrorNotDeclared(Token token) throws ExcecaoCompilador{
    throw new ExcecaoCompilador(buildSemanticNotDeclaredMessage(token));
}

public static void semanticErrorBadComparation(Token token, String expectedType) throws ExcecaoCompilador{
    throw new ExcecaoCompilador(buildSemanticBadComparationMessage(token, expectedType));
}

public static void semanticErrorInvalidExpression(String expectedType, String actualType, Token token) throws ExcecaoCompilador{
    throw new ExcecaoCompilador(buildSemanticInvalidExpressionMessage(expectedType, actualType, token));
}

public static void semanticErrorInvalidToken(Token token) throws ExcecaoCompilador{
    throw new ExcecaoCompilador(
            "Erro Semântico: expressão inválida com o token '" + token.getName() +
                    "' na linha " + token.getLine() + ", coluna " + token.getColumn() + "."
    );
}

public static void semanticErrorExpectedOperandAfter(String operator, Token token) throws ExcecaoCompilador{
    throw new ExcecaoCompilador(
            "Erro Semântico: era esperado um operando após o operador '" + operator +
                    "' na linha " + token.getLine() + ", coluna " + token.getColumn() + "."
    );
}

public static void semanticErrorInvalidExpressionAfterControl(Token token) throws ExcecaoCompilador{
    throw new ExcecaoCompilador(
            "Erro Semântico: expressão inválida após comando de controle (if/while), token '" + token.getName() +
                    "' na linha " + token.getLine() + ", coluna " + token.getColumn() + "."
    );
}

private static String buildLexicalMessage(String lexeme, int line, int column) {
    return "Erro Léxico: token não reconhecido '" + lexeme + "' na linha " + line + ", coluna " + column + ".";
}

private static String buildSyntaxMessage(String expected, Token found) {
    return "Erro Sintático: era esperado '" + expected + "', mas encontrado '" +
            found.getName() + "' na linha " + found.getLine() + ", coluna " + found.getColumn() + ".";
}

private static String buildSyntaxAssignmentMessage(Token found) {
    return "Erro Sintático: era esperada uma atribuição de valor (de variável ou literal), mas encontrado '" +
            found.getName() + "' na linha " + found.getLine() + ", coluna " + found.getColumn() + ".";
}

private static String buildSemanticAssignmentMessage(Token wrongToken, Token declaredToken) {
    return "Erro Semântico: atribuição incorreta! Foi atribuído um valor do tipo '" + wrongToken.getType() +
            "' à variável '" + declaredToken.getName() +
            "', mas era esperado o tipo '" + declaredToken.getType() +
            "'. Linha " + wrongToken.getLine() + ", coluna " + wrongToken.getColumn() + ".";
}

private static String buildSemanticNotDeclaredMessage(Token token) {
    return "Erro Semântico: a variável '" + token.getName() + "' não foi declarada. Linha " +
            token.getLine() + ", coluna " + token.getColumn() + ".";
}

private static String buildSemanticBadComparationMessage(Token token, String expectedType) {
    return "Erro Semântico: não é possível comparar um valor do tipo '" + token.getType() +
            "' com um valor do tipo '" + expectedType + "'. Linha " + token.getLine() +
            ", coluna " + token.getColumn() + ".";
}

private static String buildSemanticInvalidExpressionMessage(String expectedType, String invalidType, Token token) {
    return "Erro Semântico: operação inválida! Era esperado um tipo '" + expectedType +
            "', mas foi utilizado um tipo '" + invalidType + "'. Linha " + token.getLine() + ".";
}


}
