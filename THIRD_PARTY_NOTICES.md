# Componentes de terceiros

## Firmware

- **LedController 2.0.2**, de Noa Sakurajin: [repositório](https://github.com/noah1510/LedController). A cópia incluída em `firmware/libraries/LedController` declara LGPLv3. Consulte o [arquivo de licença distribuído](firmware/libraries/LedController/License) e preserve os avisos dos fontes.
- **ESP32Servo**, de Kevin Harrington e John K. Bennett: [repositório](https://github.com/madhephaestus/ESP32Servo). Instalada separadamente; a instalação local consultada é a 3.2.1. Aplicam-se os avisos e termos da versão utilizada.
- **Arduino-ESP32**, da Espressif: [repositório](https://github.com/espressif/arduino-esp32). Fornece a plataforma e as bibliotecas `BluetoothSerial`, `WiFi` e `WebServer`. A instalação consultada é a 3.3.12, cujo manifesto declara LGPL-2.1-or-later; componentes incorporados podem ter termos próprios. O pacote não é distribuído neste repositório.

## Aplicativo e ferramentas

- **Kotlin**: [projeto](https://kotlinlang.org/), utilizado no aplicativo.
- **Android SDK e ferramentas Android**: [documentação](https://developer.android.com/), instalados separadamente e sujeitos aos termos dos respectivos componentes.
- **Gradle Wrapper**: [projeto](https://gradle.org/). Arquivos de ferramenta distribuídos com seus avisos originais; consulte também os cabeçalhos de `android/gradlew` e `android/gradlew.bat`.
- **JUnit 4.13.2**: [projeto](https://github.com/junit-team/junit4), dependência de testes obtida durante a compilação.

Este documento registra as dependências usadas e não substitui nem altera suas licenças. Uma eventual licença das contribuições próprias do projeto não se estende automaticamente aos componentes de terceiros.
