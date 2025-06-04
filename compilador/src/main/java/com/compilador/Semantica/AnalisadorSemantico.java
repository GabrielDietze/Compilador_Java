package com.compilador.Semantica;

import java.util.HashMap;
import java.util.Map; // Sua classe de tratamento de erros
import java.util.Set;

import com.compilador.Execptions.ErroSemantico;
import com.compilador.Execptions.ErroSintatico;
import com.compilador.Execptions.ExcecaoCompilador;
import com.compilador.Lexica.Token;
import com.compilador.Table.TabelaSimbolos;

public class AnalisadorSemantico {

    private final TabelaSimbolos tabelaDeSimbolosRaiz;
    private final Map<String, Token> definicoesDeSimbolosLocais;

    private Token tokenEmAnalise;
    private int ponteiroParaTokenAtual;

    private static final Set<String> PALAVRAS_CHAVE_DE_TIPO = Set.of("int", "string", "boolean", "byte");
    private static final Set<String> OPERADORES_DE_COMPARACAO = Set.of("==", "<", "<=", ">", ">=", "<>");
    private static final Set<String> OPERADORES_LOGICOS_CONJUTIVOS = Set.of("and", "or");
    private static final Set<String> TODOS_OPERADORES_LOGICOS_RECONHECIDOS = Set.of("==", "<", "<=", ">", ">=", "<>", "and", "or", "not");
    private static final Set<String> OPERADORES_MATEMATICOS_BASICOS = Set.of("+", "-", "*", "/");

    public AnalisadorSemantico(TabelaSimbolos tabelaPrincipal) {
        this.tabelaDeSimbolosRaiz = tabelaPrincipal;
        this.definicoesDeSimbolosLocais = new HashMap<>();
        this.ponteiroParaTokenAtual = 0;
        if (this.tabelaDeSimbolosRaiz != null && this.tabelaDeSimbolosRaiz.tamanho() > 0) {
            this.tokenEmAnalise = this.tabelaDeSimbolosRaiz.tokenAtual(this.ponteiroParaTokenAtual);
        } else {
            this.tokenEmAnalise = null;
        }
    }

    private void consumirProximoToken() {
        if (this.tabelaDeSimbolosRaiz != null && ponteiroParaTokenAtual < this.tabelaDeSimbolosRaiz.tamanho() - 1) {
            ponteiroParaTokenAtual++;
            tokenEmAnalise = this.tabelaDeSimbolosRaiz.tokenAtual(ponteiroParaTokenAtual);
        } else {
            tokenEmAnalise = null;
        }
    }

    private void retornarTokenAnterior() {
        if (ponteiroParaTokenAtual > 0) {
            ponteiroParaTokenAtual--;
            tokenEmAnalise = this.tabelaDeSimbolosRaiz.tokenAtual(ponteiroParaTokenAtual);
        }
    }

   private void processarInstrucaoWriteln() throws ExcecaoCompilador {
    Token instrucaoWritelnToken = tokenEmAnalise; // Para mensagens de erro contextualizadas
    consumirProximoToken(); // Consome a palavra-chave 'writeln'

    // Trata vírgula imediatamente após o 'writeln'
    if (tokenEmAnalise != null && ",".equals(tokenEmAnalise.getNome())) {
        consumirProximoToken(); // Consome a vírgula
    } else {
        ErroSintatico.erroSintatico("Esperado ',' após a instrução '" + instrucaoWritelnToken.getNome() + "'",
                                    tokenEmAnalise != null ? tokenEmAnalise : instrucaoWritelnToken);
        return; // Ou avançar para tentar recuperar
    }

    // Verifica se há pelo menos uma expressão para escrever
    if (tokenEmAnalise == null || ";".equals(tokenEmAnalise.getNome())) { // Pode ser writeln, ; para linha em branco
         // Se for só writeln, ; (para pular linha), não precisa de expressão.
         // Se sua linguagem permitir writeln,VARIAVEL; então este if é diferente.
         // Assumindo que writeln, expressao; é o padrão como o write.
        ErroSintatico.erroSintatico("Expressão esperada após ',' na instrução '" + instrucaoWritelnToken.getNome() + "'",
                                    instrucaoWritelnToken); // Ou o token da vírgula como referência
        // Se writeln, ; for válido para pular linha, não gere erro aqui.
        // A lógica abaixo já trataria isso se não encontrar expressões.
    }

    // Se o token após a vírgula não for um ponto e vírgula (indicando writeln,;)
    // então processamos as expressões.
    if (tokenEmAnalise != null && !";".equals(tokenEmAnalise.getNome())) {
        // Processa a primeira expressão
        resolverTipoDeExpressao("QUALQUER_TIPO_VALIDO_PARA_WRITE"); // Mesmo tipo de validação do write

        // Loop para processar expressões subsequentes separadas por vírgula
        while (tokenEmAnalise != null && ",".equals(tokenEmAnalise.getNome())) {
            consumirProximoToken(); // Consome a vírgula ','

            if (tokenEmAnalise == null || ";".equals(tokenEmAnalise.getNome())) { // Vírgula solta no final
                Token tokenReferenciaErro = (ponteiroParaTokenAtual > 0 &&
                                             tabelaDeSimbolosRaiz.tokenAtual(ponteiroParaTokenAtual - 1).getNome().equals(",")) ?
                                             tabelaDeSimbolosRaiz.tokenAtual(ponteiroParaTokenAtual - 1) : instrucaoWritelnToken;
                ErroSintatico.erroSintatico("Expressão esperada após ',' na instrução '" + instrucaoWritelnToken.getNome() + "'", tokenReferenciaErro);
                return; // Ou avançar
            }
            resolverTipoDeExpressao("QUALQUER_TIPO_VALIDO_PARA_WRITE");
        }
    }
    // Espera-se um ponto e vírgula para finalizar a instrução
    if (tokenEmAnalise == null || !";".equals(tokenEmAnalise.getNome())) {
        ErroSintatico.erroSintatico("';' esperado para finalizar a instrução '" + instrucaoWritelnToken.getNome() + "'",
                                    tokenEmAnalise != null ? tokenEmAnalise : instrucaoWritelnToken);
    } else {
        consumirProximoToken(); // Consome o ';'
    }
}
    private void processarInstrucaoWrite() throws ExcecaoCompilador {
    Token instrucaoWriteToken = tokenEmAnalise; // Para mensagens de erro contextualizadas
    consumirProximoToken(); // Consome a palavra-chave 'write'

    // Trata vírgula imediatamente após o 'write'
    if (tokenEmAnalise != null && ",".equals(tokenEmAnalise.getNome())) {
        consumirProximoToken(); // Consome a vírgula
    } else {
        ErroSintatico.erroSintatico("Esperado ',' após a instrução '" + instrucaoWriteToken.getNome() + "'", tokenEmAnalise != null ? tokenEmAnalise : instrucaoWriteToken);
        return;
    }

    // Verifica se há pelo menos uma expressão para escrever
    if (tokenEmAnalise == null) {
        ErroSintatico.erroSintatico("Expressão esperada após ',' na instrução '" + instrucaoWriteToken.getNome() + "'", instrucaoWriteToken);
        return;
    }

    // Processa a primeira expressão
    resolverTipoDeExpressao("QUALQUER_TIPO_VALIDO_PARA_WRITE");

    // Loop para processar expressões subsequentes separadas por vírgula
    while (tokenEmAnalise != null && ",".equals(tokenEmAnalise.getNome())) {
        consumirProximoToken(); // Consome a vírgula ','

        if (tokenEmAnalise == null) {
            Token tokenReferenciaErro = (ponteiroParaTokenAtual > 0 && 
                                       tabelaDeSimbolosRaiz.tokenAtual(ponteiroParaTokenAtual - 1).getNome().equals(",")) ?
                                       tabelaDeSimbolosRaiz.tokenAtual(ponteiroParaTokenAtual - 1) : instrucaoWriteToken;
            ErroSintatico.erroSintatico("Expressão esperada após ',' na instrução '" + instrucaoWriteToken.getNome() + "'", tokenReferenciaErro);
            return;
        }

        resolverTipoDeExpressao("QUALQUER_TIPO_VALIDO_PARA_WRITE");
    }

    // Espera-se um ponto e vírgula para finalizar a instrução
    if (tokenEmAnalise == null || !";".equals(tokenEmAnalise.getNome())) {
        ErroSintatico.erroSintatico("';' esperado para finalizar a instrução '" + instrucaoWriteToken.getNome() + "'", 
                                    tokenEmAnalise != null ? tokenEmAnalise : instrucaoWriteToken);
    } else {
        consumirProximoToken(); // Consome o ';'
    }
}

    private boolean ehPalavraChaveDeTipo(Token token) {
        return token != null && PALAVRAS_CHAVE_DE_TIPO.contains(token.getNome().toLowerCase());
    }

    private boolean ehOperadorLogicoReconhecido(Token token) {
        return token != null && TODOS_OPERADORES_LOGICOS_RECONHECIDOS.contains(token.getNome());
    }

    private boolean ehOperadorMatematicoBasico(Token token) {
        return token != null && OPERADORES_MATEMATICOS_BASICOS.contains(token.getNome());
    }

    private boolean identificadorFoiPreviamenteDeclarado(String nomeIdentificador) {
        return definicoesDeSimbolosLocais.containsKey(nomeIdentificador);
    }

    private String determinarTipoDeDadoSemantico(Token token) throws ExcecaoCompilador {
        if (token == null) {
            // ErroLexico não tem um "erro fatal" genérico. Lançando ExcecaoCompilador diretamente.
            // O método erroLexico espera um lexema, não uma mensagem genérica.
            throw new ExcecaoCompilador("Erro Interno: Tentativa de determinar tipo de token nulo.");
        }

        if ("CONST".equalsIgnoreCase(token.getTipo())) {
            return token.getClassificacao().toLowerCase();
        } else if ("ID".equalsIgnoreCase(token.getClassificacao())) {
            Token definicaoLocal = definicoesDeSimbolosLocais.get(token.getNome());
            if (definicaoLocal != null && definicaoLocal.getTipo() != null) {
                String tipoResolvido = definicaoLocal.getTipo();
                if (!"ID".equalsIgnoreCase(tipoResolvido) && !"pendente_final".equalsIgnoreCase(tipoResolvido)) {
                    return tipoResolvido.toLowerCase();
                }
            }
            // Renomeado na sua classe ErroLexico: erroSemanticoTookenInvalido -> erroSemanticoTokenInvalido
            // Usando erroSemanticoNaoDeclarado aqui, que parece mais apropriado.
            ErroSemantico.erroSemanticoNaoDeclarado(token);
            return "tipo_id_nao_resolvido_erro";
        }

        // Se chegou aqui, o token não é uma constante nem um ID válido como operando.
        // Na sua classe ErroLexico, o método é erroSemanticoTookenInvalido (com 'k'). Corrija o nome se necessário.
        // Vou usar erroSemanticoTookenInvalido assumindo que o nome está assim na sua classe.
        ErroSemantico.erroSemanticoTookenInvalido(token);
        return "tipo_operando_invalido_erro";
    }

    private boolean tokenAtualConfiguraUmOperando() {
        if (tokenEmAnalise == null) {
            return false;
        }
        return "CONST".equalsIgnoreCase(tokenEmAnalise.getTipo())
                || "ID".equalsIgnoreCase(tokenEmAnalise.getClassificacao());
    }

    public void executarVerificacaoSemanticaCompleta() throws ExcecaoCompilador {
        try {
            processarBlocoDeDeclaracoes();
            propagarTiposParaTabelaDeSimbolosGlobal();
            validarInstrucoesEFluxoDeDados();
        } catch (ExcecaoCompilador e) {
            throw e;
        } catch (Exception e) {
            int linha = tokenEmAnalise != null ? tokenEmAnalise.getLinha() : -1;
            int coluna = tokenEmAnalise != null ? tokenEmAnalise.getColuna() : -1;
            String nomeTokenStr = tokenEmAnalise != null ? tokenEmAnalise.getNome() : "N/A";

            // ErroLexico não tem "gerarErroFatal". Lançando ExcecaoCompilador diretamente.
            String mensagemDetalhada = "Exceção crítica no verificador semântico: " + e.getClass().getSimpleName()
                    + " - " + e.getMessage()
                    + " (Último token: " + nomeTokenStr + " L" + linha + ":C" + coluna + ")";
            throw new ExcecaoCompilador(mensagemDetalhada, e);
        }
    }

    private void processarBlocoDeDeclaracoes() throws ExcecaoCompilador {
    while (tokenEmAnalise != null && (ehPalavraChaveDeTipo(tokenEmAnalise) || "final".equals(tokenEmAnalise.getNome().toLowerCase()))) {
        boolean isFinalConstant = false;
        String declaredTypeForId; // Este será o tipo real: "int", "string", etc.
        Token tokenForErrorReporting = tokenEmAnalise; // Token para referência em mensagens de erro

        if ("final".equals(tokenEmAnalise.getNome().toLowerCase())) {
            isFinalConstant = true;
            consumirProximoToken(); // Consome "final"

            if (tokenEmAnalise == null) {
                ErroSintatico.erroSintatico("Tipo ou Identificador após 'final'", tokenForErrorReporting);
                return; // Ou lançar exceção para parar
            }

            // Após "final", pode vir um tipo explícito (ex: final int ID) ou diretamente o ID (ex: final ID = valor)
            if (ehPalavraChaveDeTipo(tokenEmAnalise)) { // Caso: final TIPO ID ...
                declaredTypeForId = tokenEmAnalise.getNome().toLowerCase();
                consumirProximoToken(); // Consome o tipo (int, string, etc.)
            } else if ("ID".equalsIgnoreCase(tokenEmAnalise.getClassificacao())) { // Caso: final ID = valor; (tipo será inferido)
                declaredTypeForId = null; // Sinaliza que o tipo deve ser inferido da atribuição
            } else {
                ErroSintatico.erroSintatico("Tipo ou Identificador válido após 'final'", tokenEmAnalise);
                return;
            }
        } else { // Declaração de variável normal (não final)
            // isFinalConstant continua false
            declaredTypeForId = tokenEmAnalise.getNome().toLowerCase();
            consumirProximoToken(); // Consome o tipo (int, string, etc.)
        }

        // Neste ponto, o tokenEmAnalise DEVE ser o Identificador
        Token idToken;
        if (tokenEmAnalise != null && "ID".equalsIgnoreCase(tokenEmAnalise.getClassificacao())) {
            idToken = tokenEmAnalise;
        } else {
            ErroSintatico.erroSintatico("Identificador", tokenEmAnalise != null ? tokenEmAnalise : tokenForErrorReporting);
            return;
        }
        String idName = idToken.getNome();

        if (identificadorFoiPreviamenteDeclarado(idName)) {
            throw new ExcecaoCompilador("Erro Semântico: Identificador '" + idName + "' já foi declarado anteriormente. Linha " + idToken.getLinha() + ", coluna " + idToken.getColuna() + ".");
        }

        consumirProximoToken(); // Consome o ID, esperando '=' ou ';'

        String actualTypeOfId = declaredTypeForId; // Pode ser null se 'final ID = valor' e o tipo ainda não foi inferido

        if (tokenEmAnalise != null && "=".equals(tokenEmAnalise.getNome())) { // Atribuição encontrada
            consumirProximoToken(); // Consome o '='
            if (tokenEmAnalise == null) {
                ErroSintatico.erroSintatico("Valor (literal ou identificador) para atribuição", idToken);
                return;
            }
            Token assignedValueToken = tokenEmAnalise;
            String typeOfAssignedValue = determinarTipoDeDadoSemantico(assignedValueToken);

            if (actualTypeOfId == null) { // O tipo precisava ser inferido (caso: final ID = valor;)
                if (!isFinalConstant) {
                    // Isso seria um erro de lógica interna ou uma construção de linguagem não prevista.
                    // Variáveis normais geralmente requerem tipo explícito se não inicializadas.
                    throw new ExcecaoCompilador("Erro Interno: Tentativa de inferir tipo para variável não constante sem tipo explícito.");
                }
                actualTypeOfId = typeOfAssignedValue; // O tipo da constante é o tipo do valor atribuído
            } else {
                // O tipo foi declarado explicitamente (ex: int x = valor; ou final int x = valor;)
                // Agora, a comparação correta (equivalente à sua antiga linha 186):
                if (!actualTypeOfId.equalsIgnoreCase(typeOfAssignedValue)) {
                    throw new ExcecaoCompilador(
                        "Erro Semântico: atribuição incompatível! Variável '" + idName +
                        "' (tipo '" + actualTypeOfId + "') não pode receber valor do tipo '" + typeOfAssignedValue +
                        "'. Linha " + assignedValueToken.getLinha() + ", coluna " + assignedValueToken.getColuna() + "."
                    );
                }
            }

            // Armazena o símbolo com seu tipo CORRETO.
            // Você pode querer adicionar uma flag 'isConstant' ao Token se precisar distinguir
            // `int` de `final int` para outras verificações (ex: tentativa de reatribuição).
            Token tokenToStore = new Token(idName, actualTypeOfId, idToken.getClassificacao(), idToken.getLinha(), idToken.getColuna() /*, isFinalConstant */);
            definicoesDeSimbolosLocais.put(idName, tokenToStore);

            consumirProximoToken(); // Consome o valor atribuído
        } else { // Sem atribuição imediata (ex: int x; ou final int x;)
            if (isFinalConstant) {
                // Constantes devem ser inicializadas na declaração.
                // Se actualTypeOfId é null aqui, significa `final ID;` (sem tipo explícito, sem valor) -> Erro.
                // Se actualTypeOfId não é null, significa `final TIPO ID;` (com tipo explícito, mas sem valor) -> Erro.
                throw new ExcecaoCompilador("Erro Semântico: Constante 'final' '" + idName + "' deve ser inicializada na declaração. Linha " + idToken.getLinha() + ", coluna " + idToken.getColuna() + ".");
            }
            if (actualTypeOfId == null) {
                 // Caso de declaração de variável sem tipo e sem inicialização (ex: "variavel;")
                 // Se sua linguagem não permitir isso, lance um erro.
                throw new ExcecaoCompilador("Erro Semântico: Variável '" + idName + "' declarada sem tipo explícito e sem inicialização. Linha " + idToken.getLinha() + ", coluna " + idToken.getColuna() + ".");
            }
            // Declaração de variável sem inicialização (ex: int x;)
            Token tokenToStore = new Token(idName, actualTypeOfId, idToken.getClassificacao(), idToken.getLinha(), idToken.getColuna());
            definicoesDeSimbolosLocais.put(idName, tokenToStore);
            // Não consome token aqui, o próximo token deve ser ';'
        }

        // Verifica o ponto e vírgula finalizador
        if (tokenEmAnalise == null || !";".equals(tokenEmAnalise.getNome())) {
            ErroSintatico.erroSintatico(";", tokenEmAnalise != null ? tokenEmAnalise : idToken);
            avancarAteProximaInstrucaoOuDeclaracao(); // Tenta se recuperar do erro
        } else {
            consumirProximoToken(); // Consome ';'
        }
    }
    }

private void propagarTiposParaTabelaDeSimbolosGlobal() {
    for (Token tokenDefinidoLocalmente : definicoesDeSimbolosLocais.values()) {
        for (int i = 0; i < tabelaDeSimbolosRaiz.tamanho(); i++) {
            Token tokenNaTabelaGlobal = tabelaDeSimbolosRaiz.tokenAtual(i);
            if (tokenNaTabelaGlobal.getNome().equals(tokenDefinidoLocalmente.getNome())) {
                if (tokenNaTabelaGlobal.getTipo() == null || !tokenNaTabelaGlobal.getTipo().equals(tokenDefinidoLocalmente.getTipo())) {
                    Token novoToken = new Token(
                        tokenDefinidoLocalmente.getNome(),
                        tokenDefinidoLocalmente.getTipo(),
                        tokenDefinidoLocalmente.getClassificacao(),
                        tokenDefinidoLocalmente.getLinha(),
                        tokenDefinidoLocalmente.getColuna()
                    );
                    tabelaDeSimbolosRaiz.subistituirToken(i, novoToken); 
                }
                break;
            }
        }
    }
}

    private void validarInstrucoesEFluxoDeDados() throws ExcecaoCompilador {
    while (tokenEmAnalise != null) {
        String nomeAtual = tokenEmAnalise.getNome().toLowerCase(); // Converter para minúsculas para ser case-insensitive
        String classificacaoAtual = tokenEmAnalise.getClassificacao();

        if ("ID".equalsIgnoreCase(classificacaoAtual)) {
            checarAtribuicaoParaIdentificador(tokenEmAnalise);
        } else if ("while".equals(nomeAtual) || "if".equals(nomeAtual)) {
            Token tokenEstruturaControle = tokenEmAnalise;
            consumirProximoToken();
            if (tokenEmAnalise == null) {
                ErroSemantico.erroSemanticoExpressaoInvalidaAposControle(tokenEstruturaControle);
                return;
            }
            String tipoExprCondicional = resolverTipoDeExpressao("boolean");
            if (!"boolean".equalsIgnoreCase(tipoExprCondicional)) {
                ErroSemantico.erroSemanticoExpressaoInvalida("boolean", tipoExprCondicional, tokenEstruturaControle);
            }
        } else if ("write".equals(nomeAtual)) { // <--- NOVA CONDIÇÃO
            processarInstrucaoWrite();
        } else if (";".equals(nomeAtual)) {
            consumirProximoToken();
        } else if (deveIgnorarTokenAtualNaValidacao(nomeAtual)) {
            consumirProximoToken();
        } else if ("readln".equals(nomeAtual)) {
            processarInstrucaoReadln();
        } else if ("writeln".equals(nomeAtual)) {
            processarInstrucaoWriteln();
        }
            else {
            // Se não for instrução reconhecida.
            ErroSintatico.erroSintatico("Início de instrução válida ou fim de bloco", tokenEmAnalise);
            consumirProximoToken();
        }
    }
}

private void processarInstrucaoReadln() throws ExcecaoCompilador {
    Token instrucaoReadlnToken = tokenEmAnalise; // Para mensagens de erro
    consumirProximoToken(); // Consome a palavra-chave 'readln'

    // Espera uma vírgula após 'readln'
    if (tokenEmAnalise != null && ",".equals(tokenEmAnalise.getNome())) {
        consumirProximoToken(); // Consome a vírgula
    } else {
        ErroSintatico.erroSintatico("Esperado ',' após a instrução '" + instrucaoReadlnToken.getNome() + "'",
                                    tokenEmAnalise != null ? tokenEmAnalise : instrucaoReadlnToken);
        return; // Ou avançar para tentar recuperar
    }

    // Espera um identificador (a variável que receberá o valor)
    if (tokenEmAnalise != null && "ID".equalsIgnoreCase(tokenEmAnalise.getClassificacao())) {
        Token idToken = tokenEmAnalise;

        // Verificação semântica: o identificador foi declarado?
        if (!identificadorFoiPreviamenteDeclarado(idToken.getNome())) {
            ErroSemantico.erroSemanticoNaoDeclarado(idToken); // Lança exceção
            // Não precisa consumir token aqui se erroSemanticoNaoDeclarado já lança e para
            return; // Adicionado para clareza, embora a exceção pare o fluxo
        }

        // TODO: Adicionar outras verificações semânticas se necessário
        // Ex: A variável é de um tipo que pode receber input? É uma constante 'final'?

        consumirProximoToken(); // Consome o ID da variável
    } else {
        ErroSintatico.erroSintatico("Identificador esperado após ',' na instrução '" + instrucaoReadlnToken.getNome() + "'",
                                    tokenEmAnalise != null ? tokenEmAnalise : instrucaoReadlnToken);
        return; // Ou avançar
    }

    // Espera-se um ponto e vírgula para finalizar a instrução
    if (tokenEmAnalise == null || !";".equals(tokenEmAnalise.getNome())) {
        ErroSintatico.erroSintatico("';' esperado para finalizar a instrução '" + instrucaoReadlnToken.getNome() + "'",
                                    tokenEmAnalise != null ? tokenEmAnalise : instrucaoReadlnToken);
    } else {
        consumirProximoToken(); // Consome o ';'
    }
}

    private boolean deveIgnorarTokenAtualNaValidacao(String nomeToken) {
        return "begin".equals(nomeToken) || "end".equals(nomeToken) || "else".equals(nomeToken)
                || (tokenEmAnalise != null && ehPalavraChaveDeTipo(tokenEmAnalise))
                || "final".equals(nomeToken);
    }

    private void checarAtribuicaoParaIdentificador(Token idLadoEsquerdo) throws ExcecaoCompilador {
        if (!identificadorFoiPreviamenteDeclarado(idLadoEsquerdo.getNome())) {
            ErroSemantico.erroSemanticoNaoDeclarado(idLadoEsquerdo);
            avancarAteProximaInstrucaoOuDeclaracao(Set.of(";"));
            return;
        }

        Token definicaoDoId = definicoesDeSimbolosLocais.get(idLadoEsquerdo.getNome());
        String tipoEsperadoPeloId = definicaoDoId.getTipo();

        consumirProximoToken();

        if (tokenEmAnalise != null && "=".equals(tokenEmAnalise.getNome())) {
            consumirProximoToken();
            if (tokenEmAnalise == null) {
                ErroSintatico.erroSintatico("Expressão/Valor", idLadoEsquerdo); // O erro é após o idLadoEsquerdo (e o igual)
                return;
            }

            Token primeiroTokenExprDireita = tokenEmAnalise;
            String tipoCalculadoExprDireita = resolverTipoDeExpressao(tipoEsperadoPeloId);

            if (!tipoEsperadoPeloId.equalsIgnoreCase(tipoCalculadoExprDireita)) {
                // Mesma questão do erroSemanticoAtribuicao, usando ExcecaoCompilador para clareza
                throw new ExcecaoCompilador(
                        "Erro Semântico: atribuição incompatível! Variável '" + idLadoEsquerdo.getNome()
                        + "' (tipo '" + tipoEsperadoPeloId + "') não pode receber valor do tipo '" + tipoCalculadoExprDireita
                        + "'. Linha " + primeiroTokenExprDireita.getLinha() + ", coluna " + primeiroTokenExprDireita.getColuna() + "."
                );
            }

            if (tokenEmAnalise == null || !";".equals(tokenEmAnalise.getNome())) {
                ErroSintatico.erroSintatico(";", tokenEmAnalise != null ? tokenEmAnalise : idLadoEsquerdo);
            } else {
                consumirProximoToken();
            }
        } else {
            Token tokenInesperado = tokenEmAnalise; // O token que não é '='
            retornarTokenAnterior(); // Volta para o ID, para a mensagem de erro fazer sentido sobre o que veio APÓS o ID.
            ErroSintatico.erroSintatico("=", tokenInesperado != null ? tokenInesperado : idLadoEsquerdo); // Esperava '=' mas encontrou 'tokenInesperado'
        }
    }

    private String resolverTipoDeExpressao(String tipoDeContexto) throws ExcecaoCompilador {
        if (tokenEmAnalise == null) {
            throw new ExcecaoCompilador("Erro Sintático: Expressão inesperadamente vazia ou fim de arquivo.");
        }

        String tipoLadoEsquerdoAcumulado;

        if ("(".equals(tokenEmAnalise.getNome())) {
            Token parenAbertura = tokenEmAnalise;
            consumirProximoToken();
            tipoLadoEsquerdoAcumulado = resolverTipoDeExpressao(tipoDeContexto);
            if (tokenEmAnalise == null || !")".equals(tokenEmAnalise.getNome())) {
                ErroSintatico.erroSintatico(")", tokenEmAnalise != null ? tokenEmAnalise : parenAbertura);
            } else {
                consumirProximoToken();
            }
        } else if (tokenAtualConfiguraUmOperando()) {
            if ("ID".equalsIgnoreCase(tokenEmAnalise.getClassificacao()) && !identificadorFoiPreviamenteDeclarado(tokenEmAnalise.getNome())) {
                ErroSemantico.erroSemanticoNaoDeclarado(tokenEmAnalise);
                // consumirProximoToken(); // O erroSemanticoNaoDeclarado já lança exceção, não precisa consumir
                return "tipo_expr_erro_id_nao_declarado";
            }
            tipoLadoEsquerdoAcumulado = determinarTipoDeDadoSemantico(tokenEmAnalise);
            consumirProximoToken();
        } else if ("not".equalsIgnoreCase(tokenEmAnalise.getNome())) {
            Token operadorNotToken = tokenEmAnalise;
            consumirProximoToken();
            String tipoOperandoDoNot = resolverTipoDeExpressao("boolean");
            if (!"boolean".equalsIgnoreCase(tipoOperandoDoNot)) {
                ErroSemantico.erroSemanticoExpressaoInvalida("boolean", tipoOperandoDoNot, operadorNotToken);
            }
            return "boolean";
        } else {
            ErroSintatico.erroSintatico("Operando, '(' ou 'not'", tokenEmAnalise);
            return "tipo_expr_erro_inicio_invalido"; // Não será alcançado
        }

        while (tokenEmAnalise != null && !ehDelimitadorDeExpressaoComum(tokenEmAnalise.getNome())
                && (ehOperadorMatematicoBasico(tokenEmAnalise) || ehOperadorLogicoReconhecido(tokenEmAnalise))) {

            Token tokenOperadorAtual = tokenEmAnalise;
            boolean opMatematico = ehOperadorMatematicoBasico(tokenOperadorAtual);

            consumirProximoToken();

            if (tokenEmAnalise == null) {
                ErroSemantico.erroSemanticoOperandoEsperadoApos(tokenOperadorAtual.getNome(), tokenOperadorAtual);
                return "tipo_expr_erro_fim_abrupto"; // Não será alcançado
            }

            String tipoLadoDireito = resolverProximoFatorOuTermoDaExpressao(tipoDeContexto);

            if (opMatematico) {
                if (!"int".equalsIgnoreCase(tipoLadoEsquerdoAcumulado) || !"int".equalsIgnoreCase(tipoLadoDireito)) {
                    throw new ExcecaoCompilador(
                            "Erro Semântico: Operador '" + tokenOperadorAtual.getNome() + "' requer operandos do tipo 'int', mas recebeu '"
                            + tipoLadoEsquerdoAcumulado + "' e '" + tipoLadoDireito + "'. Linha "
                            + tokenOperadorAtual.getLinha() + ", coluna " + tokenOperadorAtual.getColuna() + "."
                    );
                }
                tipoLadoEsquerdoAcumulado = "int";
            } else {
                if (OPERADORES_DE_COMPARACAO.contains(tokenOperadorAtual.getNome())) {
                    if (!tipoLadoEsquerdoAcumulado.equalsIgnoreCase(tipoLadoDireito)
                            || !(tipoLadoEsquerdoAcumulado.equals("int") || tipoLadoEsquerdoAcumulado.equals("boolean") || tipoLadoEsquerdoAcumulado.equals("string"))) {
                        // erroSemanticoComparacaoInvalida espera (Token token, String tipoEsperado)
                        // O 'token' é o operador. O 'tipoEsperado' seria o tipo do lado esquerdo.
                        ErroSemantico.erroSemanticoComparacaoInvalida(tokenOperadorAtual, tipoLadoEsquerdoAcumulado);
                    }
                } else if (OPERADORES_LOGICOS_CONJUTIVOS.contains(tokenOperadorAtual.getNome())) {
                    if (!"boolean".equalsIgnoreCase(tipoLadoEsquerdoAcumulado) || !"boolean".equalsIgnoreCase(tipoLadoDireito)) {
                        // erroSemanticoExpressaoInvalida(String tipoEsperado, String tipoRecebido, Token token)
                        // Se ambos deveriam ser boolean, o tipoEsperado é "boolean".
                        // Se tipoLadoEsquerdoAcumulado não for boolean, ele é o "tipoRecebido" problemático.
                        String tipoRecebidoProblematico = !"boolean".equalsIgnoreCase(tipoLadoEsquerdoAcumulado) ? tipoLadoEsquerdoAcumulado : tipoLadoDireito;
                        ErroSemantico.erroSemanticoExpressaoInvalida("boolean", tipoRecebidoProblematico, tokenOperadorAtual);
                    }
                }
                tipoLadoEsquerdoAcumulado = "boolean";
            }
        }
        return tipoLadoEsquerdoAcumulado;
    }

    private String resolverProximoFatorOuTermoDaExpressao(String tipoDeContexto) throws ExcecaoCompilador {
        if (tokenEmAnalise == null) {
            throw new ExcecaoCompilador("Erro Sintático: Expressão inesperadamente vazia ao buscar próximo termo/fator.");
        }
        String tipoResolvido;
        Token tokenReferenciaErro = tokenEmAnalise; // Para usar em caso de erro de parêntese

        if ("(".equals(tokenEmAnalise.getNome())) {
            consumirProximoToken();
            tipoResolvido = resolverTipoDeExpressao(tipoDeContexto);
            if (tokenEmAnalise == null || !")".equals(tokenEmAnalise.getNome())) {
                ErroSintatico.erroSintatico(")", tokenEmAnalise != null ? tokenEmAnalise : tokenReferenciaErro);
            } else {
                consumirProximoToken();
            }
        } else if (tokenAtualConfiguraUmOperando()) {
            if ("ID".equalsIgnoreCase(tokenEmAnalise.getClassificacao()) && !identificadorFoiPreviamenteDeclarado(tokenEmAnalise.getNome())) {
                ErroSemantico.erroSemanticoNaoDeclarado(tokenEmAnalise);
                // consumirProximoToken(); // Exceção já foi lançada
                return "tipo_fator_erro_id_nao_declarado";
            }
            tipoResolvido = determinarTipoDeDadoSemantico(tokenEmAnalise);
            consumirProximoToken();
        } else if ("not".equalsIgnoreCase(tokenEmAnalise.getNome())) {
            // erroSemanticoTookenInvalido é o mais próximo para um 'not' mal posicionado aqui.
            ErroSemantico.erroSemanticoTookenInvalido(tokenEmAnalise);
            return "tipo_fator_erro_not_mal_posicionado"; // Não será alcançado
        } else {
            ErroSintatico.erroSintatico("Operando ou '('", tokenEmAnalise);
            return "tipo_fator_erro_invalido"; // Não será alcançado
        }
        return tipoResolvido;
    }

    private boolean ehDelimitadorDeExpressaoComum(String nomeToken) {
        return ";".equals(nomeToken)
                || ")".equals(nomeToken)
                || ",".equals(nomeToken)
                || "then".equals(nomeToken)
                || // Para IF expr THEN ... (se sua linguagem usar THEN)
                "do".equals(nomeToken)
                || // Para WHILE expr DO ... (se sua linguagem usar DO)
                "begin".equals(nomeToken);  // Pode delimitar expressão antes de um bloco
    }

    private void avancarAteProximaInstrucaoOuDeclaracao(Set<String> sincronizadoresAdicionais) throws ExcecaoCompilador {
        if (tokenEmAnalise == null) {
            return;
        }

        while (tokenEmAnalise != null) {
            String nomeAtual = tokenEmAnalise.getNome();
            if (";".equals(nomeAtual)
                    || (sincronizadoresAdicionais != null && sincronizadoresAdicionais.contains(nomeAtual))
                    || ehPalavraChaveDeTipo(tokenEmAnalise)
                    || "final".equals(nomeAtual)
                    || "if".equals(nomeAtual)
                    || "while".equals(nomeAtual)
                    || "begin".equals(nomeAtual)
                    || "end".equals(nomeAtual)) {
                return;
            }
            consumirProximoToken();
        }
    }

    private void avancarAteProximaInstrucaoOuDeclaracao() throws ExcecaoCompilador {
        avancarAteProximaInstrucaoOuDeclaracao(null);
    }
}
