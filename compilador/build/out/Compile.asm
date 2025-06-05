.686
.model flat, stdcall
option casemap:none

include \masm32\include\windows.inc
include \masm32\include\kernel32.inc
include \masm32\include\masm32.inc
includelib \masm32\lib\kernel32.lib
includelib \masm32\lib\masm32.lib

.data
    szCrLf db 13, 10, 0
    tempIntStr db 12 dup(0)
    trueStr db "true", 0
    falseStr db "false", 0
    n dd 0
    nome db 256 dup(0) ; Buffer para string
    naoTerminou db 0
    MAXITER dd 0

.code
start:
    invoke StdIn, addr nome, 256
    invoke ExitProcess, 0
end start
