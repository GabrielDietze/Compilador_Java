package com.compilador.Execptions;

import com.compilador.Lexica.Token;

public class ErroSemantico {
    
    public static void erroSemanticoAtribuicao(Token tokenIncorreto, Token tokenDeclarado) throws ExcecaoCompilador {
        throw new ExcecaoCompilador(mensagemErroSemanticoAtribuicao(tokenIncorreto, tokenDeclarado));
    }

    public static void erroSemanticoNaoDeclarado(Token token) throws ExcecaoCompilador {
        throw new ExcecaoCompilador(mensagemErroSemanticoNaoDeclarado(token));
    }

    public static void erroSemanticoComparacaoInvalida(Token token, String tipoEsperado) throws ExcecaoCompilador {
        throw new ExcecaoCompilador(mensagemErroSemanticoComparacaoInvalida(token, tipoEsperado));
    }

    public static void erroSemanticoExpressaoInvalida(String tipoEsperado, String tipoRecebido, Token token) throws ExcecaoCompilador {
        throw new ExcecaoCompilador(mensagemErroSemanticoExpressaoInvalida(tipoEsperado, tipoRecebido, token));
    }

    public static void erroSemanticoTookenInvalido(Token token) throws ExcecaoCompilador {
        throw new ExcecaoCompilador(
                "Erro Semântico: expressão inválida com o token '" + token.getNome() +
                "' na linha " + token.getLinha() + ", coluna " + token.getColuna() + "."
        );
    }

    public static void erroSemanticoOperandoEsperadoApos(String operador, Token token) throws ExcecaoCompilador {
        throw new ExcecaoCompilador(
                "Erro Semântico: era esperado um operando após o operador '" + operador +
                "' na linha " + token.getLinha() + ", coluna " + token.getColuna() + "."
        );
    }

    public static void erroSemanticoExpressaoInvalidaAposControle(Token token) throws ExcecaoCompilador {
        throw new ExcecaoCompilador(
                "Erro Semântico: expressão inválida após comando de controle (if/while), token '" + token.getNome() +
                "' na linha " + token.getLinha() + ", coluna " + token.getColuna() + "."
        );
    }

    

    private static String mensagemErroSemanticoAtribuicao(Token tokenIncorreto, Token tokenDeclarado) {
        return "Erro Semântico: atribuição incorreta! Foi atribuído um valor do tipo '" + tokenIncorreto.getTipo() +
                "' à variável '" + tokenDeclarado.getNome() +
                "', mas era esperado o tipo '" + tokenDeclarado.getTipo() +
                "'. Linha " + tokenIncorreto.getLinha() + ", coluna " + tokenIncorreto.getColuna() + ".";
    }

    private static String mensagemErroSemanticoNaoDeclarado(Token token) {
        return "Erro Semântico: a variável '" + token.getNome() + "' não foi declarada. Linha " +
                token.getLinha() + ", coluna " + token.getColuna() + ".";
    }

    private static String mensagemErroSemanticoComparacaoInvalida(Token token, String tipoEsperado) {
        return "Erro Semântico: não é possível comparar um valor do tipo '" + token.getTipo() +
                "' com um valor do tipo '" + tipoEsperado + "'. Linha " + token.getLinha() +
                ", coluna " + token.getColuna() + ".";
    }

    private static String mensagemErroSemanticoExpressaoInvalida(String tipoEsperado, String tipoRecebido, Token token) {
        return "Erro Semântico: operação inválida! Era esperado um tipo '" + tipoEsperado +
                "', mas foi utilizado um tipo '" + tipoRecebido + "'. Linha " + token.getLinha() + ".";
    }
}
