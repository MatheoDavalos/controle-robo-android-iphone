# Controle Robô

Aplicativo Android e firmware ESP32 para controlar um robô com duas rodas,
expressões em matrizes de LEDs e aceleração/frenagem graduais.

Este repositório contém somente a versão final aprovada no projeto.

## Instalar o aplicativo

Baixe [Controle_Robo.apk](apk/Controle_Robo.apk?raw=true) no celular Android.
O APK requer **Android 8.0 ou superior** e Bluetooth. Não é compatível com iPhone.

1. Abra o APK baixado e autorize a instalação por essa fonte, se o Android solicitar.
2. Ligue o robô e pareie o dispositivo **RobotUA** nas configurações Bluetooth.
3. Abra **Controle_Robo**, permita o acesso ao Bluetooth e conecte ao robô.
4. Use o controle esquerdo para frente/trás e o direito para direção.
5. Soltar movimento inicia a frenagem das duas rodas. Soltar apenas direção,
   mantendo movimento pressionado, volta a andar reto.

O arquivo distribuído é o APK debug usado na validação, assinado com a chave de
desenvolvimento original. A chave não está neste repositório. Uma compilação em
outra máquina pode ter assinatura diferente e exigir desinstalar a anterior.
O checksum está em [apk/SHA256SUMS](apk/SHA256SUMS).

## Estrutura

```text
android/                       Projeto Android Studio, somente variante final
firmware/HeadController/        Sketch ESP32 e controle dos motores/face
firmware/libraries/LedController/ Biblioteca fornecida no projeto original
firmware/tests/                 Testes C++ e simulação das tarefas do firmware
apk/                           APK final e checksum
docs/                         Instalação, hardware, protocolo e validação
```

## Compilar

- **App:** abra `android/` no Android Studio, configure o Android SDK e use JDK 17
  ou 21. Selecione `controleRoboDebug`.
- **Robô:** abra `firmware/HeadController/HeadController.ino` no Arduino IDE,
  selecione **DOIT ESP32 DEVKIT V1** e a porta correta. Instale ESP32Servo e
  disponibilize LedController conforme [docs/COMPILAR.md](docs/COMPILAR.md).

Os quatro arquivos `.ino`, `.cpp` e `.h` do sketch devem ficar juntos.
A identificação da versão final na serial é `elapsed-ramp-v3`.

## Testar

```bash
bash firmware/tests/run.sh
cd android
./gradlew testControleRoboDebugUnitTest assembleControleRoboDebug lintControleRoboDebug
```

A organização do repositório preserva os fontes finais do app e firmware. Não
inclui variantes antigas, APKs anteriores, binários antigos do ESP32, caches,
configurações pessoais, chaves de assinatura nem histórico de desenvolvimento.

Consulte [hardware e protocolo](docs/HARDWARE.md), [compilação](docs/COMPILAR.md)
e [validação](docs/VALIDACAO.md).

## Créditos

O controlador de cabeça deriva do trabalho de Luciano Neris, com autoria
preservada nos arquivos. A lógica de rampa integra contribuições do projeto e
a revisão final de temporização. A biblioteca LedController mantém sua licença
e créditos próprios. Veja [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
