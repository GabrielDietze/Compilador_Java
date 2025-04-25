
package com.compilador.Lexica;

public class Token {
    private String nome; 
    private String tipo; 
    private String classificacao;
    private int linha;
    private int coluna;


    public Token(String nome, String tipo, String classificacao, int linha, int coluna) {
        this.nome = nome;
        this.tipo = tipo;
        this.classificacao = classificacao;
        this.linha = linha;
        this.coluna = coluna;
    }
    public String getNome() { return nome; }
    public String getTipo() { return tipo; }
    public String getClassificacao() { return classificacao; }
    public int getLinha() { return linha; }
    public int getColuna() { return coluna; }

    @Override
    public String toString() {
        return String.format("Nome: %s, Classificacao: %s, Tipo: %s, Linha: %d, Coluna: %d]", this.nome, this.classificacao,
                this.tipo, this.linha, this.coluna);
    }
}