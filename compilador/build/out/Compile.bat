@echo off
set projectName=Compile

C:\masm32\bin\ml /c /coff %projectName%.asm
C:\masm32\bin\Link /SUBSYSTEM:CONSOLE %projectName%.obj

%projectName%.exe
pause
