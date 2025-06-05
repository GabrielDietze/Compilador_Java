package com.compilador.Assembly;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet; // Para a lista de argumentos do write
import java.util.List; // Para strings literais únicas
import java.util.Map;   // Para strings literais únicas
import java.util.Set;   // Para variaveisDeclaradasNoData

import com.compilador.Lexica.Token; // Para variaveisDeclaradasNoData
import com.compilador.Table.TabelaSimbolos;

public class GeradorAssembly {

    private final TabelaSimbolos tabelaSimbolos;
    private final List<Token> tokens;
    private int position = 0;
    private Token current;

    private final StringBuilder header = new StringBuilder();
    private final StringBuilder sectionData = new StringBuilder();
    private final StringBuilder sectionText = new StringBuilder();

    private int labelCounter = 0; // Para gerar labels únicos
    private final Map<String, String> stringLiterals = new HashMap<>(); // Para armazenar strings literais únicas e seus labels

    private final String outputDir = "build/out"; // Você pode querer tornar isso configurável
    private final String outputFile;

    // Labels para dados auxiliares comuns
    private final String SZCRLF_LABEL = "szCrLf";
    private final String TEMP_INT_STR_LABEL = "tempIntStr";
    private final String TRUE_STR_LABEL = "trueStr";
    private final String FALSE_STR_LABEL = "falseStr";

    public GeradorAssembly(TabelaSimbolos tabelaSimbolos, String outputName) {
        this.tabelaSimbolos = tabelaSimbolos;
        // Assumindo que TabelaSimbolos tem um método getTabelaSimbolos() que retorna List<Token>
        this.tokens = tabelaSimbolos.getTabelaSimbolos(); 
        this.outputFile = outputName + ".asm";
        if (this.tokens != null && !this.tokens.isEmpty()) {
            this.current = this.tokens.get(0);
        } else {
            this.current = null;
        }
    }

    public void generate() {
        if (tokens == null || tokens.isEmpty()) {
            System.err.println("GeradorAssembly: Nenhum token para processar.");
            return;
        }
        makeDir();
        buildHeader();
        // Pré-define strings auxiliares no data segment para que buildDataSegment não as duplique
        // e para que buildTextSegment possa referenciá-las.
        addCommonDataStrings();
        buildDataSegment(); // Declara variáveis globais
        buildTextSegment(); // Constrói o código das instruções
        saveToFile();
    }

    private void makeDir() {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                System.err.println("Falha ao criar diretório de saída: " + outputDir);
            }
        }
    }

    private void move() {
        position++;
        if (position < tokens.size()) {
            current = tokens.get(position);
        } else {
            current = null; // Fim dos tokens
        }
    }
    
    // Retorna o token sem avançar
    private Token peek() {
        if (position + 1 < tokens.size()) {
            return tokens.get(position + 1);
        }
        return null;
    }

    private String generateNewLabel(String prefix) {
        return prefix + (labelCounter++);
    }

    private void buildHeader() {
        header.append(".686\n")
              .append(".model flat, stdcall\n")
              .append("option casemap:none\n\n")
              .append("include \\masm32\\include\\windows.inc\n")
              .append("include \\masm32\\include\\kernel32.inc\n")
              .append("include \\masm32\\include\\masm32.inc\n")
              .append("includelib \\masm32\\lib\\kernel32.lib\n")
              .append("includelib \\masm32\\lib\\masm32.lib\n\n");
    }
    
    private void addCommonDataStrings() {
        sectionData.append(String.format("    %s db 13, 10, 0\n", SZCRLF_LABEL)); // Nova linha
        sectionData.append(String.format("    %s db 12 dup(0)\n", TEMP_INT_STR_LABEL)); // Buffer para conversão int->string
        sectionData.append(String.format("    %s db \"true\", 0\n", TRUE_STR_LABEL));
        sectionData.append(String.format("    %s db \"false\", 0\n", FALSE_STR_LABEL));
    }


    private void buildDataSegment() {
        sectionData.insert(0, ".data\n"); // Garante que .data seja a primeira linha desta seção
        Set<String> variaveisDeclaradasNoData = new HashSet<>();
        // Adiciona os labels comuns para não serem redeclarados se aparecerem como ID (improvável)
        variaveisDeclaradasNoData.add(SZCRLF_LABEL);
        variaveisDeclaradasNoData.add(TEMP_INT_STR_LABEL);
        variaveisDeclaradasNoData.add(TRUE_STR_LABEL);
        variaveisDeclaradasNoData.add(FALSE_STR_LABEL);


        for (Token token : this.tokens) { // Itera sobre todos os tokens para encontrar declarações
            // Esta lógica assume que as declarações de variáveis podem aparecer em qualquer lugar na lista de tokens
            // e que o tipo semântico já foi definido no token.
            // Idealmente, o Analisador Semântico forneceria uma lista limpa de variáveis declaradas.
            if (token != null && "ID".equalsIgnoreCase(token.getClassificacao()) &&
                token.getTipo() != null && 
                !token.getTipo().trim().isEmpty() &&
                !variaveisDeclaradasNoData.contains(token.getNome())) {

                // Verifica se o tipo do ID é um tipo de dados conhecido para alocação
                String name = token.getNome();
                String type = token.getTipo().toLowerCase();
                String assemblyDefaultValue = "0";

                boolean isDataType = false;
                switch (type) {
                    case "int":
                        sectionData.append(String.format("    %s dd %s\n", name, assemblyDefaultValue));
                        isDataType = true;
                        break;
                    case "boolean":
                    case "byte":
                        sectionData.append(String.format("    %s db %s\n", name, assemblyDefaultValue));
                        isDataType = true;
                        break;
                    case "string":
                        sectionData.append(String.format("    %s db 256 dup(0) ; Buffer para string\n", name));
                        isDataType = true;
                        break;
                }
                if (isDataType) {
                    variaveisDeclaradasNoData.add(name);
                }
            }
        }
        sectionData.append("\n");
    }
    
    // Adiciona uma string literal ao .data se ainda não existir, e retorna seu label
    private String addStringLiteralToData(String literalValue) {
        if (stringLiterals.containsKey(literalValue)) {
            return stringLiterals.get(literalValue);
        }
        String label = generateNewLabel("str_lit_");
        sectionData.append(String.format("    %s db \"%s\", 0\n", label, literalValue));
        stringLiterals.put(literalValue, label);
        return label;
    }

    private void buildTextSegment() {
        sectionText.append(".code\nstart:\n");
        // TODO: Considerar adicionar setup do stack frame:
        // sectionText.append("    push ebp\n");
        // sectionText.append("    mov ebp, esp\n");
        // sectionText.append("    sub esp, TAMANHO_DAS_VARIAVEIS_LOCAIS_NA_PILHA\n"); // Se houver vars locais na pilha

        // Loop principal para processar tokens como instruções
        // Este loop assume que a lista de tokens representa uma sequência de comandos executáveis
        // após as declarações. O Analisador Sintático deveria ter estruturado isso.
        // Avança até o 'begin' do bloco principal, se houver.
        while(current != null && !current.getNome().equalsIgnoreCase("begin")) {
            move();
        }
        if (current != null && current.getNome().equalsIgnoreCase("begin")) {
            move(); // Pula o 'begin'
        }


        while (current != null && !current.getNome().equalsIgnoreCase("end")) { // Processa até o 'end' do bloco principal
            parseStatement();
            if (current != null && current.getNome().equals(";")){ // Consome ponto e vírgula opcional após statements complexos
                move();
            }
        }
        
        // TODO: Considerar adicionar limpeza do stack frame antes do ExitProcess:
        // sectionText.append("    mov esp, ebp\n");
        // sectionText.append("    pop ebp\n");
        sectionText.append("    invoke ExitProcess, 0\n")
                   .append("end start\n");
    }

    private void parseStatement() {
        if (current == null) return;

        switch (current.getNome().toLowerCase()) { // Usar toLowerCase para palavras-chave
            case "write":
                parseWrite(false);
                break;
            case "writeln":
                parseWrite(true);
                break;
            case "readln":
                parseRead();
                break;
            case "if":
                parseIf();
                break;
            case "while":
                parseWhile();
                break;
            default:
                if ("ID".equalsIgnoreCase(current.getClassificacao())) {
                    parseAssignment();
                } else {
                    // Token inesperado ou não tratado como início de instrução
                    // System.err.println("GeradorAssembly: Token inesperado no início da instrução: " + current.getNome());
                    move(); // Avança para tentar continuar
                }
                break;
        }
    }

    private void parseWrite(boolean lineBreak) {
        Token commandToken = current; // "write" ou "writeln"
        move(); // Pula "write" ou "writeln"

        // Sintaxe LC: write, arg1, arg2, ... ; OU write(arg1, arg2, ...);
        // O código abaixo tenta suportar a versão com parênteses. Ajuste se necessário.
        boolean isParenthesized = false;
        if (current != null && current.getNome().equals("(")) {
            isParenthesized = true;
            move(); // Pula "("
        } else if (current != null && current.getNome().equals(",")){
             move(); // Pula "," se for a sintaxe write, arg;
        } else {
            System.err.println("Erro de Geração: Esperado '(' ou ',' após " + commandToken.getNome());
            consumeUntilSemicolonOrEnd(); return;
        }

        boolean firstArgument = true;
        while (current != null && !(isParenthesized && current.getNome().equals(")")) && !current.getNome().equals(";")) {
            if (!firstArgument) {
                if (current.getNome().equals(",")) {
                    move(); // Pula ","
                } else {
                    System.err.println("Erro de Geração: Esperado ',' entre argumentos do " + commandToken.getNome());
                    consumeUntilSemicolonOrEnd(); return;
                }
            }
            firstArgument = false;
            if (current == null || (isParenthesized && current.getNome().equals(")")) || current.getNome().equals(";")) break;


            if ("ID".equalsIgnoreCase(current.getClassificacao())) {
                String varName = current.getNome();
                // Tentar obter o tipo semântico do ID. Assume que está no próprio token após análise semântica.
                String varType = (current.getTipo() != null) ? current.getTipo().toLowerCase() : "desconhecido";

                switch (varType) {
                    case "string":
                        sectionText.append(String.format("    invoke StdOut, addr %s\n", varName));
                        break;
                    case "int":
                        sectionText.append(String.format("    invoke dwtoa, dword ptr [%s], addr %s\n", varName, TEMP_INT_STR_LABEL));
                        sectionText.append(String.format("    invoke StdOut, addr %s\n", TEMP_INT_STR_LABEL));
                        break;
                    case "boolean":
                        String elseLabel = generateNewLabel("printBool_else_");
                        String endIfLabel = generateNewLabel("printBool_endif_");
                        sectionText.append(String.format("    mov al, byte ptr [%s]\n", varName));
                        sectionText.append("    cmp al, 1\n"); // Compara com true (1)
                        sectionText.append(String.format("    jne %s\n", elseLabel));
                        sectionText.append(String.format("    invoke StdOut, addr %s\n", TRUE_STR_LABEL));
                        sectionText.append(String.format("    jmp %s\n", endIfLabel));
                        sectionText.append(String.format("%s:\n", elseLabel));
                        sectionText.append(String.format("    invoke StdOut, addr %s\n", FALSE_STR_LABEL));
                        sectionText.append(String.format("%s:\n", endIfLabel));
                        break;
                    default:
                        System.err.println("Erro de Geração: Tipo de variável desconhecido para " + commandToken.getNome() + ": " + varName + " (tipo: " + varType + ")");
                        sectionText.append(String.format("    ; Erro: Nao foi possivel imprimir variavel %s de tipo %s\n", varName, varType));
                        break;
                }
                move();
            } else if ("CONST_STRING".equalsIgnoreCase(current.getTipo())) {
                String literalValue = current.getNome();
                if (literalValue.startsWith("\"") && literalValue.endsWith("\"")) {
                    literalValue = literalValue.substring(1, literalValue.length() - 1);
                }
                String strDataLabel = addStringLiteralToData(literalValue);
                sectionText.append(String.format("    invoke StdOut, addr %s\n", strDataLabel));
                move();
            } else if ("CONST_NUM".equalsIgnoreCase(current.getTipo()) || "CONST_INT".equalsIgnoreCase(current.getTipo())) {
                sectionText.append(String.format("    invoke dwtoa, %s, addr %s\n", current.getNome(), TEMP_INT_STR_LABEL));
                sectionText.append(String.format("    invoke StdOut, addr %s\n", TEMP_INT_STR_LABEL));
                move();
            }  else if ("CONST_BOOL".equalsIgnoreCase(current.getTipo())) {
                boolean val = current.getNome().equalsIgnoreCase("true");
                sectionText.append(String.format("    invoke StdOut, addr %s\n", val ? TRUE_STR_LABEL : FALSE_STR_LABEL));
                move();
            }else {
                System.err.println("Erro de Geração: Argumento inesperado para " + commandToken.getNome() + ": " + current.getNome());
                move(); 
            }
        }

        if (isParenthesized) {
            if (current == null || !current.getNome().equals(")")) {
                System.err.println("Erro de Geração: Esperado ')' para fechar " + commandToken.getNome());
                 consumeUntilSemicolonOrEnd(); return;
            }
            move(); // Pula ")"
        }

        if (lineBreak) {
            sectionText.append(String.format("    invoke StdOut, addr %s\n", SZCRLF_LABEL));
        }

        // Ponto e vírgula é consumido pelo loop principal do buildTextSegment ou aqui.
        // Se for a sintaxe com vírgula, o ; já foi pego ou será o próximo.
        if (current != null && current.getNome().equals(";")) {
            // move(); // O loop em buildTextSegment pode consumir
        } else if (!isParenthesized && current != null && !current.getNome().equals(";")){
            // Se não for parentesizado E não for ponto e vírgula, pode ser um erro
            // System.err.println("Erro de Geração: Esperado ';' ao final de " + commandToken.getNome());
        }
    }

    private void parseRead() {
        Token commandToken = current; // "readln"
        move(); // Pula "readln"

        // Sintaxe LC: readln, var; OU readln(var);
        boolean isParenthesized = false;
        if (current != null && current.getNome().equals("(")) {
            isParenthesized = true;
            move(); // Pula "("
        } else if (current != null && current.getNome().equals(",")){
             move(); // Pula ","
        } else {
            System.err.println("Erro de Geração: Esperado '(' ou ',' após " + commandToken.getNome());
            consumeUntilSemicolonOrEnd(); return;
        }

        if (current == null || !"ID".equalsIgnoreCase(current.getClassificacao())) {
            System.err.println("Erro de Geração: Esperado identificador de variável para " + commandToken.getNome());
            consumeUntilSemicolonOrEnd(); return;
        }
        String varName = current.getNome();
        // Assume que a variável é uma string e já foi declarada no .data com tamanho suficiente (ex: 256)
        sectionText.append(String.format("    invoke StdIn, addr %s, 256\n", varName));
        move(); // Pula o nome da variável

        if (isParenthesized) {
            if (current == null || !current.getNome().equals(")")) {
                System.err.println("Erro de Geração: Esperado ')' para fechar " + commandToken.getNome());
                consumeUntilSemicolonOrEnd(); return;
            }
            move(); // Pula ")"
        }
        
        // Ponto e vírgula é consumido pelo loop principal do buildTextSegment
        // if (current == null || !current.getNome().equals(";")) {
        //     System.err.println("Erro de Geração: Esperado ';' ao final de " + commandToken.getNome());
        // } else {
        //     move(); 
        // }
    }

    private void parseAssignment() {
        Token varDestToken = current;
        if (!"ID".equalsIgnoreCase(varDestToken.getClassificacao())) {
            System.err.println("Erro de Geração: Lado esquerdo da atribuição não é um ID.");
            consumeUntilSemicolonOrEnd(); return;
        }
        String varDest = varDestToken.getNome();
        String destType = (varDestToken.getTipo() != null) ? varDestToken.getTipo().toLowerCase() : "desconhecido";
        move(); // Pula ID (variável destino)

        if (current == null || !current.getNome().equals("=")) {
            System.err.println("Erro de Geração: Esperado '=' em uma atribuição para " + varDest);
            consumeUntilSemicolonOrEnd(); return;
        }
        move(); // Pula "="

        Token valueToken = current;
        if (valueToken == null) {
            System.err.println("Erro de Geração: Esperado valor após '=' na atribuição para " + varDest);
            consumeUntilSemicolonOrEnd(); return;
        }

        String valueLexeme = valueToken.getNome();
        String valueClassification = valueToken.getClassificacao();
        String valueType = (valueToken.getTipo() != null) ? valueToken.getTipo().toLowerCase() : "desconhecido";


        if ("ID".equalsIgnoreCase(valueClassification)) {
            // Atribuição de variável para variável: dest = source
            String varSource = valueLexeme;
            // TODO: Verificar compatibilidade de tipos (semântica já deveria ter feito)
            // Assumindo mesmo tamanho por simplicidade.
            if (destType.equals("string") && valueType.equals("string")) {
                 sectionText.append(String.format("    invoke lstrcpy, addr %s, addr %s ; Cuidado: lstrcpy é da API do Windows\n", varDest, varSource));
            } else if (destType.equals("int") && valueType.equals("int")) {
                sectionText.append(String.format("    mov eax, dword ptr [%s]\n", varSource));
                sectionText.append(String.format("    mov dword ptr [%s], eax\n", varDest));
            } else if (destType.equals("boolean") && valueType.equals("boolean")) {
                sectionText.append(String.format("    mov al, byte ptr [%s]\n", varSource));
                sectionText.append(String.format("    mov byte ptr [%s], al\n", varDest));
            } else {
                 System.err.println("Erro de Geração: Atribuição entre tipos incompatíveis ou não suportados: " + destType + " = " + valueType);
            }
        } else if ("CONST_NUM".equalsIgnoreCase(valueType) || "CONST_INT".equalsIgnoreCase(valueType)) {
            if (destType.equals("int")) {
                sectionText.append(String.format("    mov dword ptr [%s], %s\n", varDest, valueLexeme));
            } else {
                System.err.println("Erro de Geração: Atribuindo número a tipo não-int: " + destType);
            }
        } else if ("CONST_BOOL".equalsIgnoreCase(valueType)) {
             if (destType.equals("boolean")) {
                String boolAsmVal = valueLexeme.equalsIgnoreCase("true") ? "1" : "0";
                sectionText.append(String.format("    mov byte ptr [%s], %s\n", varDest, boolAsmVal));
            } else {
                System.err.println("Erro de Geração: Atribuindo booleano a tipo não-booleano: " + destType);
            }
        } else if ("CONST_STRING".equalsIgnoreCase(valueType)) {
             if (destType.equals("string")) {
                String literal = valueLexeme;
                if (literal.startsWith("\"") && literal.endsWith("\"")) {
                    literal = literal.substring(1, literal.length() - 1);
                }
                String strDataLabel = addStringLiteralToData(literal);
                sectionText.append(String.format("    invoke lstrcpy, addr %s, addr %s ; Cuidado: lstrcpy é da API do Windows\n", varDest, strDataLabel));
            } else {
                 System.err.println("Erro de Geração: Atribuindo string literal a tipo não-string: " + destType);
            }
        } else {
            // TODO: Lidar com expressões aritméticas/lógicas (ex: var = val1 + val2)
            // Isso requer uma pilha de avaliação ou uso mais elaborado de registradores.
            // Por enquanto, esta é uma simplificação ENORME.
            System.err.println("Erro de Geração: Lado direito da atribuição não é um ID, número ou booleano literal simples: " + valueLexeme);
             // Tentativa ingênua de mover se for um valor simples (pode não ser assembly válido)
            // sectionText.append(String.format("    mov dword ptr [%s], %s ; ATENCAO: Atribuicao simplificada\n", varDest, valueLexeme));
        }
        move(); // Pula o valor/ID do lado direito

        // Ponto e vírgula é consumido pelo loop principal
        // if (current == null || !current.getNome().equals(";")) {
        //     System.err.println("Erro de Geração: Esperado ';' ao final da atribuição para " + varDest);
        // } else {
        //    move(); 
        // }
    }
    
    // ATENÇÃO: parseIf e parseWhile abaixo são MUITO simplificados e têm limitações
    // significativas, especialmente com aninhamento e expressões complexas.
    // Eles funcionam melhor se o Analisador Sintático já simplificou a estrutura
    // ou se você estivesse trabalhando com uma AST.

    private void parseIf() {
        Token commandToken = current; // "if"
        move(); // Pula "if"

        // Assume sintaxe if (condicao) begin ... end [else begin ... end] end; (ou similar)
        // A condição precisa ser processada para pular corretamente.
        // Esta é uma implementação MUITO básica e provavelmente precisará de mais trabalho.
        // Exemplo: if (var == valor)
        // TODO: Implementar parsing de expressão condicional completa.
        //       O código abaixo é um placeholder MUITO simplificado.
        
        if (current == null || !current.getNome().equals("(")) {
            System.err.println("Erro de Geração: Esperado '(' após if");
            consumeUntilSemicolonOrEnd(); return;
        }
        move(); // Pula "("
        
        // Simplificação grosseira: assume condicao é "ID op VALOR_OU_ID"
        Token leftOp = current; move();
        Token operator = current; move();
        Token rightOp = current; move();

        if (leftOp == null || operator == null || rightOp == null) {
            System.err.println("Erro de Geração: Condição do if mal formada.");
            consumeUntilSemicolonOrEnd(); return;
        }

        if (current == null || !current.getNome().equals(")")) {
            System.err.println("Erro de Geração: Esperado ')' após condição do if");
            consumeUntilSemicolonOrEnd(); return;
        }
        move(); // Pula ")"
        
        // O token 'begin' após if (...) é opcional na sua gramática original?
        // Se for 'if condicao begin ...', o Analisador Sintático deve garantir.
        // Se o 'begin' for obrigatório, verifique-o.
        if (current != null && current.getNome().equalsIgnoreCase("begin")) {
            move(); // Pula "begin" opcional do if
        }


        String elseLabel = generateNewLabel("if_else_");
        String endIfLabel = generateNewLabel("if_endif_");

        // Gera código para avaliar a condição
        // Exemplo para "ID op VALOR" (ex: n < 10)
        if ("ID".equalsIgnoreCase(leftOp.getClassificacao())) {
            sectionText.append(String.format("    mov eax, dword ptr [%s]\n", leftOp.getNome()));
            if ("CONST_NUM".equalsIgnoreCase(rightOp.getTipo()) || "CONST_INT".equalsIgnoreCase(rightOp.getTipo()) ) {
                sectionText.append(String.format("    cmp eax, %s\n", rightOp.getNome()));
            } else if ("ID".equalsIgnoreCase(rightOp.getClassificacao())) {
                 sectionText.append(String.format("    cmp eax, dword ptr [%s]\n", rightOp.getNome()));
            } else {
                 System.err.println("Erro de Geração: Lado direito da condição do if não suportado: " + rightOp.getNome());
            }
        } else {
            System.err.println("Erro de Geração: Lado esquerdo da condição do if deve ser um ID (simplificado): " + leftOp.getNome());
        }
        
        // Mapeia o operador LC para o salto Assembly apropriado (salta para o ELSE se a condição for FALSA)
        String jumpInstruction = "";
        switch (operator.getNome()) {
            case "==": jumpInstruction = "jne"; break; // Pula se não igual
            case "<>": case "!=": jumpInstruction = "je"; break;  // Pula se igual
            case "<":  jumpInstruction = "jge"; break; // Pula se maior ou igual
            case "<=": jumpInstruction = "jg"; break;  // Pula se maior
            case ">":  jumpInstruction = "jle"; break; // Pula se menor ou igual
            case ">=": jumpInstruction = "jl"; break;  // Pula se menor
            default:
                System.err.println("Erro de Geração: Operador de if desconhecido: " + operator.getNome());
                // Nao salta, executa o bloco then por padrao se o operador for invalido
                break; 
        }
        if (!jumpInstruction.isEmpty()) {
            sectionText.append(String.format("    %s %s\n", jumpInstruction, elseLabel));
        }


        // Bloco THEN
        while (current != null && !current.getNome().equalsIgnoreCase("else") && !current.getNome().equalsIgnoreCase("end")) {
            parseStatement();
             if (current != null && current.getNome().equals(";")){ move(); } // Consome ponto e vírgula
        }

        boolean hasElse = false;
        if (current != null && current.getNome().equalsIgnoreCase("else")) {
            hasElse = true;
            sectionText.append(String.format("    jmp %s\n", endIfLabel)); // Pula o bloco ELSE se o THEN foi executado
            sectionText.append(String.format("%s:\n", elseLabel)); // Label para o início do ELSE
            move(); // Pula "else"
            
            if (current != null && current.getNome().equalsIgnoreCase("begin")) {
                move(); // Pula "begin" opcional do else
            }

            // Bloco ELSE
            while (current != null && !current.getNome().equalsIgnoreCase("end")) {
                parseStatement();
                if (current != null && current.getNome().equals(";")){ move(); } // Consome ponto e vírgula
            }
        }

        if (!hasElse) { // Se não houve bloco else, o elseLabel ainda é o destino se a condição falhar
            sectionText.append(String.format("%s:\n", elseLabel));
        }
        sectionText.append(String.format("%s:\n", endIfLabel)); // Label para o fim do IF

        if (current != null && current.getNome().equalsIgnoreCase("end")) {
            move(); // Pula "end"
        } else {
            System.err.println("Erro de Geração: Esperado 'end' para finalizar o if.");
        }
    }


    private void parseWhile() {
        Token commandToken = current; // "while"
        move(); // Pula "while"

        // Assume sintaxe while (condicao_booleana_simples_var) begin ... end;
        // TODO: Implementar parsing de expressão condicional completa.
        //       O código abaixo é um placeholder MUITO simplificado que assume
        //       que a condição é uma única variável booleana.
        if (current == null || !current.getNome().equals("(")) {
            System.err.println("Erro de Geração: Esperado '(' após while");
            consumeUntilSemicolonOrEnd(); return;
        }
        move(); // Pula "("

        if (current == null || !"ID".equalsIgnoreCase(current.getClassificacao())) {
            // Para uma condição mais complexa como "n < MAXITER", você precisaria de um parser de expressão aqui.
            System.err.println("Erro de Geração: Condição do while simplificada espera um ID booleano: " + (current != null ? current.getNome() : "null"));
            consumeUntilSemicolonOrEnd(); return;
        }
        String conditionVar = current.getNome(); // Nome da variável booleana da condição
        move(); // Pula variável da condição

        if (current == null || !current.getNome().equals(")")) {
            System.err.println("Erro de Geração: Esperado ')' após condição do while");
            consumeUntilSemicolonOrEnd(); return;
        }
        move(); // Pula ")"
        
        if (current != null && current.getNome().equalsIgnoreCase("begin")) {
            move(); // Pula "begin"
        } else {
            System.err.println("Erro de Geração: Esperado 'begin' após condição do while");
             consumeUntilSemicolonOrEnd(); return;
        }

        String loopStartLabel = generateNewLabel("while_start_");
        String loopEndLabel = generateNewLabel("while_end_");

        sectionText.append(String.format("%s:\n", loopStartLabel));
        // Verifica a condição (assume que conditionVar é um byte: 1 para true, 0 para false)
        // Se a condição for uma expressão como "n < MAXITER", a lógica aqui seria:
        // mov eax, [n]
        // cmp eax, [MAXITER]
        // jge loopEndLabel ; (Salta se n NÃO FOR MENOR que MAXITER)
        sectionText.append(String.format("    mov al, byte ptr [%s]\n", conditionVar));
        sectionText.append("    cmp al, 1       ; Compara com true (1)\n");
        sectionText.append(String.format("    jne %s      ; Se não for true, sai do loop\n", loopEndLabel));

        // Corpo do LOOP
        while (current != null && !current.getNome().equalsIgnoreCase("end")) {
            parseStatement();
            if (current != null && current.getNome().equals(";")){ move(); } // Consome ponto e vírgula
        }

        sectionText.append(String.format("    jmp %s      ; Volta para o início do loop\n", loopStartLabel));
        sectionText.append(String.format("%s:\n", loopEndLabel));

        if (current != null && current.getNome().equalsIgnoreCase("end")) {
            move(); // Pula "end"
        } else {
            System.err.println("Erro de Geração: Esperado 'end' para finalizar o while.");
        }
    }
    
    private void consumeUntilSemicolonOrEnd() {
        while(current != null && !current.getNome().equals(";") && !current.getNome().equalsIgnoreCase("end")) {
            move();
        }
        if (current != null && current.getNome().equals(";")) {
            move(); // Consome o ponto e vírgula também
        }
    }

    private void saveToFile() {
        StringBuilder finalAssembly = new StringBuilder();
        finalAssembly.append(header.toString());
        finalAssembly.append(sectionData.toString());
        finalAssembly.append(sectionText.toString());

        File f = new File(outputDir + File.separator + outputFile);
        try (FileWriter writer = new FileWriter(f)) {
            writer.write(finalAssembly.toString());
            System.out.println("Assembly salvo em: " + f.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Erro ao salvar assembly: " + e.getMessage());
            e.printStackTrace();
        }
    }
}