# Robô Universidade Aberta UFSCar

Projeto de um robô móvel com duas rodas e expressões faciais em matrizes de LEDs, baseado no ESP32-WROOM-32. O repositório reúne o firmware embarcado, o aplicativo móvel, a interface web e a documentação de montagem e validação.

O funcionamento integra **comunicação → comandos de movimento → aceleração e frenagem → expressões da face**. Os canais Bluetooth e Wi-Fi utilizam o mesmo protocolo de um byte.

## Estrutura do repositório

```text
robo-universidade-aberta-ufscar/
  README.md
  THIRD_PARTY_NOTICES.md        # Componentes e licenças de terceiros
  android/                     # Código-fonte do aplicativo móvel
  apk/                         # Aplicativo compilado e checksum SHA-256
  firmware/
    HeadController/
      HeadController.ino       # Inicialização, comunicação e tarefas
      Motion.h                 # Movimento, rampa e timeout
      ControlArbiter.h         # Arbitragem entre canais
      WebControl.h             # Interface web hospedada no ESP32
      Head.h / Head.cpp        # Matrizes e expressões da face
    libraries/LedController/   # Biblioteca das matrizes MAX7219
    tests/                     # Testes C++ e simulação das tarefas
  docs/                        # Hardware, compilação e validação
```

A execução embarcada começa em [`HeadController.ino`](firmware/HeadController/HeadController.ino). Movimento, face, arbitragem e interface web ficam em arquivos separados na mesma pasta do sketch.

## Preparação e execução

O hardware utiliza um ESP32-WROOM-32 de 4 MB, dois servos de rotação contínua e quatro matrizes de LEDs 8×8 com MAX7219. As conexões e o protocolo estão em [hardware e protocolo](docs/HARDWARE.md).

1. Instale o suporte ESP32 da Espressif e a biblioteca ESP32Servo no Arduino IDE.
2. Disponibilize `firmware/libraries/LedController` na pasta de bibliotecas do Sketchbook.
3. Abra `firmware/HeadController/HeadController.ino`, mantendo os arquivos da pasta juntos.
4. Selecione **ESP32 Dev Module**, **Flash Size: 4MB**, **Partition Scheme: Huge APP (3MB No OTA/1MB SPIFFS)** e **Flash Mode: DIO**.
5. Escolha a porta da placa, compile e carregue por USB. O Monitor Serial utiliza **115200 baud**.

A partição padrão do perfil DOIT reserva aproximadamente 1,31 MB e não comporta o programa com Bluetooth e Wi-Fi. Huge APP reserva 3 MB e não oferece atualização OTA.

O aplicativo móvel está em `android/` e utiliza JDK 17 ou 21 e Android SDK 34. Para compilação:

```bash
cd android
./gradlew testControleRoboDebugUnitTest assembleControleRoboDebug
```

Dependências e procedimentos completos estão em [compilação](docs/COMPILAR.md).

## Funcionamento e configuração

1. Bluetooth Classic/SPP, interface web ou serial recebem os comandos.
2. A arbitragem seleciona o canal de controle ativo.
3. O controlador de movimento interpreta sentido e velocidade de cada roda e aplica as rampas de aceleração e frenagem.
4. O estado de movimento seleciona a expressão exibida nas matrizes.

A interface web usa HTTP e fica armazenada no ESP32, sem dependência de internet. Os dois joysticks representam movimento e direção; direção sozinha não movimenta o robô.

| Componente | Configuração atual |
|---|---|
| Protocolo | Um byte com sentido e velocidade de cada roda |
| Concessão do canal | Expira após 600 ms sem comandos do canal ativo |
| Timeout dos motores | Neutro após 800 ms sem comandos aceitos |
| Aceleração | 200 graus/s no comando dos servos |
| Frenagem | 400 graus/s no comando dos servos |
| Face | Dois olhos e duas matrizes para a boca |

As taxas dos servos são parâmetros de comando, não medições de velocidade física do robô. A configuração está nos arquivos `Motion.h`, `ControlArbiter.h` e `HeadController.ino`.

## Testes e limites da validação

Na raiz do repositório:

```bash
bash firmware/tests/run.sh
cd apk
sha256sum -c SHA256SUMS
```

Os testes locais verificam transições de movimento, rampas, timeout, expressões da face, recepção e arbitragem. Eles não substituem a compilação para ESP32 nem a avaliação dos motores na montagem real.

- A validação física conjunta de Bluetooth e Wi-Fi permanece pendente.
- A arbitragem distingue canais, mas não diferencia dois navegadores; a operação deve ser feita por um controlador de cada vez.
- A interface foi inspecionada no navegador de computador; isso não certifica todos os navegadores móveis.
- O histórico da versão anterior está separado em [validação](docs/VALIDACAO.md).

## Referências e dependências

- [LedController](https://github.com/noah1510/LedController): controle das matrizes MAX7219; versão 2.0.2 incluída em `firmware/libraries/LedController`, sob LGPLv3.
- [ESP32Servo](https://github.com/madhephaestus/ESP32Servo): geração dos sinais PWM dos servos; instalada separadamente no Arduino IDE.
- [Arduino-ESP32 — Espressif](https://github.com/espressif/arduino-esp32): plataforma do firmware e bibliotecas `BluetoothSerial`, `WiFi` e `WebServer`.
- [Kotlin](https://kotlinlang.org/) e [Android SDK](https://developer.android.com/): linguagem e plataforma do aplicativo móvel.
- [Gradle](https://gradle.org/): compilação do aplicativo pelo wrapper incluído no projeto.
- [JUnit 4](https://github.com/junit-team/junit4): testes unitários do aplicativo, versão 4.13.2.

Os componentes de terceiros mantêm suas respectivas licenças e avisos. Consulte [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
