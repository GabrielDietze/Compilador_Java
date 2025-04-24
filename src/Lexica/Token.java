public class Token {
    private String tipo; // Ex: "IDENTIFICADOR", "PALAVRA_RESERVADA", "OPERADOR"
    private String lexema; // O texto real encontrado no código
    private int linha; // Linha onde o token foi encontrado

    public Token(String tipo, String lexema, int linha) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linha = linha;
    }

    public String getTipo() { return tipo; }
    public String getLexema() { return lexema; }
    public int getLinha() { return linha; }

    @Override
    public String toString() {
        return String.format("Token{tipo='%s', lexema='%s', linha=%d}", tipo, lexema, linha);
    }
}
