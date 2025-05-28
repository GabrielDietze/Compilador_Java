package com.compilador.Semantica;

import com.compilador.Execptions.ExcecaoCompilador;
import com.compilador.Execptions.ErroLexico; // Sua classe de tratamento de erros
import com.compilador.Lexica.Token;
import com.compilador.Table.TabelaSimbolos;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
            ErroLexico.erroSemanticoNaoDeclarado(token);
            return "tipo_id_nao_resolvido_erro";
        }

        // Se chegou aqui, o token não é uma constante nem um ID válido como operando.
        // Na sua classe ErroLexico, o método é erroSemanticoTookenInvalido (com 'k'). Corrija o nome se necessário.
        // Vou usar erroSemanticoTookenInvalido assumindo que o nome está assim na sua classe.
        ErroLexico.erroSemanticoTookenInvalido(token);
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
        while (tokenEmAnalise != null && (ehPalavraChaveDeTipo(tokenEmAnalise) || "final".equals(tokenEmAnalise.getNome()))) {
            String tipoBase = tokenEmAnalise.getNome();
            Token tokenTipoBase = tokenEmAnalise; // Guardar para referência de linha/coluna
            consumirProximoToken();

            Token idDeclaradoToken;
            if (tokenEmAnalise != null && "ID".equalsIgnoreCase(tokenEmAnalise.getClassificacao())) {
                idDeclaradoToken = tokenEmAnalise;
            } else {
                // O token encontrado é o 'tokenEmAnalise' atual, ou o anterior se 'tokenEmAnalise' for nulo
                Token tokenParaErro = tokenEmAnalise != null ? tokenEmAnalise : tokenTipoBase;
                ErroLexico.erroSintatico("Identificador", tokenParaErro);
                return;
            }
            String nomeIdDeclarado = idDeclaradoToken.getNome();

            if (identificadorFoiPreviamenteDeclarado(nomeIdDeclarado)) {
                // ErroLexico não tem um erro específico para "já declarado".
                throw new ExcecaoCompilador("Erro Semântico: Identificador '" + nomeIdDeclarado
                        + "' já foi declarado anteriormente. Linha " + idDeclaradoToken.getLinha()
                        + ", coluna " + idDeclaradoToken.getColuna() + ".");
            }

            String tipoEfetivoDoId = tipoBase.toLowerCase();
            boolean declaracaoConstante = "final".equals(tipoEfetivoDoId);
            Token tokenParaArmazenarNoMapaLocal;

            if (declaracaoConstante) {
                // Cria um NOVO token com o tipo "pendente_final"
                tokenParaArmazenarNoMapaLocal = new Token(
                        idDeclaradoToken.getNome(),
                        "pendente_final", // Este é o campo 'tipo' do novo Token
                        idDeclaradoToken.getClassificacao(),
                        idDeclaradoToken.getLinha(),
                        idDeclaradoToken.getColuna()
                );
            } else {
                // Cria um NOVO token com o tipo efetivo
                tokenParaArmazenarNoMapaLocal = new Token(
                        idDeclaradoToken.getNome(),
                        tipoEfetivoDoId, // Este é o campo 'tipo' do novo Token
                        idDeclaradoToken.getClassificacao(),
                        idDeclaradoToken.getLinha(),
                        idDeclaradoToken.getColuna()
                );
            }
            definicoesDeSimbolosLocais.put(nomeIdDeclarado, idDeclaradoToken);
            consumirProximoToken();

            if (tokenEmAnalise != null && "=".equals(tokenEmAnalise.getNome())) {
                consumirProximoToken();
                if (tokenEmAnalise == null) {
                    // O valor era esperado após o ID que foi consumido (idDeclaradoToken)
                    ErroLexico.erroSintatico("Valor (literal ou identificador)", idDeclaradoToken);
                    return;
                }

                Token valorInicialToken = tokenEmAnalise;
                String tipoValorInicial = determinarTipoDeDadoSemantico(valorInicialToken);

                if (declaracaoConstante && "pendente_final".equals(idDeclaradoToken.getTipo())) {
                    idDeclaradoToken.setTipo(tipoValorInicial);
                    tipoEfetivoDoId = tipoValorInicial;
                }

                // Ajuste para erroSemanticoAtribuicao:
                // Precisamos que idDeclaradoToken.getTipo() reflita tipoEfetivoDoId
                // e valorInicialToken.getTipo() reflita tipoValorInicial (ou o mais próximo).
                // Se valorInicialToken é um literal, seu getTipo() é "CONST".
                // A mensagem em ErroLexico.erroSemanticoAtribuicao usa tokenIncorreto.getTipo().
                // Isso pode ser problemático se quisermos mostrar "int" vs "string" e não "CONST" vs "string".
                if (!tipoEfetivoDoId.equalsIgnoreCase(tipoValorInicial)) {
                    // Para manter a informação de tipo precisa, lançamos ExcecaoCompilador.
                    // Ou você ajusta ErroLexico.mensagemErroSemanticoAtribuicao para usar uma lógica
                    // similar a determinarTipoDeDadoSemantico para o tokenIncorreto.
                    throw new ExcecaoCompilador(
                            "Erro Semântico: atribuição incompatível! Variável '" + idDeclaradoToken.getNome()
                            + "' (tipo '" + tipoEfetivoDoId + "') não pode receber valor do tipo '" + tipoValorInicial
                            + "'. Linha " + valorInicialToken.getLinha() + ", coluna " + valorInicialToken.getColuna() + "."
                    );
                }
                consumirProximoToken();
            } else if (declaracaoConstante) {
                throw new ExcecaoCompilador("Erro Semântico: Constante 'final' '" + idDeclaradoToken.getNome()
                        + "' deve ser inicializada na declaração. Linha " + idDeclaradoToken.getLinha()
                        + ", coluna " + idDeclaradoToken.getColuna() + ".");
            }

            if (tokenEmAnalise == null || !";".equals(tokenEmAnalise.getNome())) {
                // O token encontrado é o atual 'tokenEmAnalise', ou o 'idDeclaradoToken' se o fim for abrupto.
                ErroLexico.erroSintatico(";", tokenEmAnalise != null ? tokenEmAnalise : idDeclaradoToken);
                avancarAteProximaInstrucaoOuDeclaracao();
            } else {
                consumirProximoToken();
            }
        }
    }

    private void propagarTiposParaTabelaDeSimbolosGlobal() {
        for (Token tokenDefinidoLocalmente : definicoesDeSimbolosLocais.values()) {
            for (int i = 0; i < tabelaDeSimbolosRaiz.tamanho(); i++) {
                Token tokenNaTabelaGlobal = tabelaDeSimbolosRaiz.tokenAtual(i); // Corrigido: tokenAtual()
                if (tokenNaTabelaGlobal.getNome().equals(tokenDefinidoLocalmente.getNome())) {
                    if (tokenNaTabelaGlobal.getTipo() == null
                            || !tokenNaTabelaGlobal.getTipo().equals(tokenDefinidoLocalmente.getTipo())) {
                        tokenNaTabelaGlobal.setTipo(tokenDefinidoLocalmente.getTipo());
                    }
                    break;
                }
            }
        }
    }

    private void validarInstrucoesEFluxoDeDados() throws ExcecaoCompilador {
        while (tokenEmAnalise != null) {
            String nomeAtual = tokenEmAnalise.getNome();
            String classificacaoAtual = tokenEmAnalise.getClassificacao();

            if ("ID".equalsIgnoreCase(classificacaoAtual)) {
                checarAtribuicaoParaIdentificador(tokenEmAnalise);
            } else if ("while".equals(nomeAtual) || "if".equals(nomeAtual)) {
                Token tokenEstruturaControle = tokenEmAnalise;
                consumirProximoToken();
                if (tokenEmAnalise == null) {
                    ErroLexico.erroSemanticoExpressaoInvalidaAposControle(tokenEstruturaControle);
                    return;
                }
                String tipoExprCondicional = resolverTipoDeExpressao("boolean");
                if (!"boolean".equalsIgnoreCase(tipoExprCondicional)) {
                    ErroLexico.erroSemanticoExpressaoInvalida("boolean", tipoExprCondicional, tokenEstruturaControle);
                }
            } else if (";".equals(nomeAtual)) {
                consumirProximoToken();
            } else if (deveIgnorarTokenAtualNaValidacao(nomeAtual)) {
                consumirProximoToken();
            } else {
                // Se não for instrução reconhecida.
                ErroLexico.erroSintatico("Início de instrução válida ou fim de bloco", tokenEmAnalise);
                consumirProximoToken();
            }
        }
    }

    private boolean deveIgnorarTokenAtualNaValidacao(String nomeToken) {
        return "begin".equals(nomeToken) || "end".equals(nomeToken) || "else".equals(nomeToken)
                || (tokenEmAnalise != null && ehPalavraChaveDeTipo(tokenEmAnalise))
                || "final".equals(nomeToken);
    }

    private void checarAtribuicaoParaIdentificador(Token idLadoEsquerdo) throws ExcecaoCompilador {
        if (!identificadorFoiPreviamenteDeclarado(idLadoEsquerdo.getNome())) {
            ErroLexico.erroSemanticoNaoDeclarado(idLadoEsquerdo);
            avancarAteProximaInstrucaoOuDeclaracao(Set.of(";"));
            return;
        }

        Token definicaoDoId = definicoesDeSimbolosLocais.get(idLadoEsquerdo.getNome());
        String tipoEsperadoPeloId = definicaoDoId.getTipo();

        consumirProximoToken();

        if (tokenEmAnalise != null && "=".equals(tokenEmAnalise.getNome())) {
            consumirProximoToken();
            if (tokenEmAnalise == null) {
                ErroLexico.erroSintatico("Expressão/Valor", idLadoEsquerdo); // O erro é após o idLadoEsquerdo (e o igual)
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
                ErroLexico.erroSintatico(";", tokenEmAnalise != null ? tokenEmAnalise : idLadoEsquerdo);
            } else {
                consumirProximoToken();
            }
        } else {
            Token tokenInesperado = tokenEmAnalise; // O token que não é '='
            retornarTokenAnterior(); // Volta para o ID, para a mensagem de erro fazer sentido sobre o que veio APÓS o ID.
            ErroLexico.erroSintatico("=", tokenInesperado != null ? tokenInesperado : idLadoEsquerdo); // Esperava '=' mas encontrou 'tokenInesperado'
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
                ErroLexico.erroSintatico(")", tokenEmAnalise != null ? tokenEmAnalise : parenAbertura);
            } else {
                consumirProximoToken();
            }
        } else if (tokenAtualConfiguraUmOperando()) {
            if ("ID".equalsIgnoreCase(tokenEmAnalise.getClassificacao()) && !identificadorFoiPreviamenteDeclarado(tokenEmAnalise.getNome())) {
                ErroLexico.erroSemanticoNaoDeclarado(tokenEmAnalise);
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
                ErroLexico.erroSemanticoExpressaoInvalida("boolean", tipoOperandoDoNot, operadorNotToken);
            }
            return "boolean";
        } else {
            ErroLexico.erroSintatico("Operando, '(' ou 'not'", tokenEmAnalise);
            return "tipo_expr_erro_inicio_invalido"; // Não será alcançado
        }

        while (tokenEmAnalise != null && !ehDelimitadorDeExpressaoComum(tokenEmAnalise.getNome())
                && (ehOperadorMatematicoBasico(tokenEmAnalise) || ehOperadorLogicoReconhecido(tokenEmAnalise))) {

            Token tokenOperadorAtual = tokenEmAnalise;
            boolean opMatematico = ehOperadorMatematicoBasico(tokenOperadorAtual);

            consumirProximoToken();

            if (tokenEmAnalise == null) {
                ErroLexico.erroSemanticoOperandoEsperadoApos(tokenOperadorAtual.getNome(), tokenOperadorAtual);
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
                        ErroLexico.erroSemanticoComparacaoInvalida(tokenOperadorAtual, tipoLadoEsquerdoAcumulado);
                    }
                } else if (OPERADORES_LOGICOS_CONJUTIVOS.contains(tokenOperadorAtual.getNome())) {
                    if (!"boolean".equalsIgnoreCase(tipoLadoEsquerdoAcumulado) || !"boolean".equalsIgnoreCase(tipoLadoDireito)) {
                        // erroSemanticoExpressaoInvalida(String tipoEsperado, String tipoRecebido, Token token)
                        // Se ambos deveriam ser boolean, o tipoEsperado é "boolean".
                        // Se tipoLadoEsquerdoAcumulado não for boolean, ele é o "tipoRecebido" problemático.
                        String tipoRecebidoProblematico = !"boolean".equalsIgnoreCase(tipoLadoEsquerdoAcumulado) ? tipoLadoEsquerdoAcumulado : tipoLadoDireito;
                        ErroLexico.erroSemanticoExpressaoInvalida("boolean", tipoRecebidoProblematico, tokenOperadorAtual);
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
                ErroLexico.erroSintatico(")", tokenEmAnalise != null ? tokenEmAnalise : tokenReferenciaErro);
            } else {
                consumirProximoToken();
            }
        } else if (tokenAtualConfiguraUmOperando()) {
            if ("ID".equalsIgnoreCase(tokenEmAnalise.getClassificacao()) && !identificadorFoiPreviamenteDeclarado(tokenEmAnalise.getNome())) {
                ErroLexico.erroSemanticoNaoDeclarado(tokenEmAnalise);
                // consumirProximoToken(); // Exceção já foi lançada
                return "tipo_fator_erro_id_nao_declarado";
            }
            tipoResolvido = determinarTipoDeDadoSemantico(tokenEmAnalise);
            consumirProximoToken();
        } else if ("not".equalsIgnoreCase(tokenEmAnalise.getNome())) {
            // erroSemanticoTookenInvalido é o mais próximo para um 'not' mal posicionado aqui.
            ErroLexico.erroSemanticoTookenInvalido(tokenEmAnalise);
            return "tipo_fator_erro_not_mal_posicionado"; // Não será alcançado
        } else {
            ErroLexico.erroSintatico("Operando ou '('", tokenEmAnalise);
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
