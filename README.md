# Controle do Robô

Projeto de controle de um robô com duas rodas e expressões em matrizes de LEDs, baseado no ESP32-WROOM-32. Integra aplicativo móvel, interface web hospedada no próprio ESP32 e firmware para os motores e a face.

## Funcionalidades

- Comunicação por Bluetooth Classic/SPP e Wi-Fi local.
- Controle de movimento e direção com dois joysticks.
- Protocolo de comandos de um byte, compartilhado pelos canais de comunicação.
- Aceleração e frenagem graduais dos servos.
- Expressões da face associadas ao movimento.
- Arbitragem entre canais e parada por ausência de comandos.

## Estrutura

- `android/`: código-fonte do aplicativo móvel.
- `apk/`: aplicativo compilado e checksum SHA-256.
- `firmware/HeadController/`: firmware dos motores, face e interface web.
- `firmware/libraries/`: biblioteca de controle das matrizes de LEDs.
- `firmware/tests/`: testes de lógica e simulação das tarefas do firmware.
- `docs/`: documentação de compilação, hardware e validação.
- `feira/`: materiais de divulgação e instruções de uso.

## Hardware e compilação

O projeto utiliza ESP32-WROOM-32 de 4 MB, dois servos de rotação contínua e quatro matrizes de LEDs 8×8 com MAX7219.

Abra `firmware/HeadController/HeadController.ino` no Arduino IDE, mantendo todos os arquivos dessa pasta juntos. Selecione **ESP32 Dev Module**, **Flash Size: 4MB**, **Partition Scheme: Huge APP (3MB No OTA/1MB SPIFFS)** e **Flash Mode: DIO**. A partição padrão do perfil DOIT não comporta o firmware com Bluetooth e Wi-Fi.

Consulte [compilação](docs/COMPILAR.md) e [hardware e protocolo](docs/HARDWARE.md) para dependências, conexões e detalhes técnicos.

## Comunicação

O protocolo codifica o sentido e a velocidade de cada roda em um byte. Bluetooth e interface web encaminham os comandos ao mesmo controlador de movimento. A interface web usa HTTP e é servida pelo ESP32, sem dependência de internet.

A arbitragem distingue os canais Bluetooth, web e serial, mas não diferencia navegadores individuais. A concessão de controle expira após 600 ms sem comandos do canal ativo; o timeout dos motores é de 800 ms. A operação deve ser feita por um controlador de cada vez.

## Testes e validação

Execute os testes locais do firmware:

```bash
bash firmware/tests/run.sh
```

Os testes cobrem movimento, rampa, timeout, expressões da face, recepção e arbitragem. A validação física conjunta de Bluetooth e Wi-Fi permanece pendente. Consulte [validação](docs/VALIDACAO.md) para o histórico e os limites dos testes.

## Créditos

Derivado do [controle original](https://github.com/MatheoDavalos/controle-robo). Autoria de Luciano Neris e créditos do projeto preservados nos fontes. Veja [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
