# Compilar o Controle do Robô

## ESP32-WROOM-32 / DOIT DEVKIT V1 (4 MB)

1. Instale o pacote ESP32 da Espressif e a biblioteca ESP32Servo no Arduino IDE.
2. Disponibilize `firmware/libraries/LedController` em `libraries` do Sketchbook.
3. Abra `firmware/HeadController/HeadController.ino`, mantendo Head.cpp, Head.h, Motion.h, ControlArbiter.h e WebControl.h na mesma pasta.
4. Selecione **ESP32 Dev Module** no menu de placas ESP32 Arduino.
5. Selecione **Flash Size: 4MB (32Mb)**, **Partition Scheme: Huge APP (3MB No OTA/1MB SPIFFS)** e **Flash Mode: DIO**.
6. Escolha a porta correta, verifique e carregue por USB.
7. Abra o Monitor Serial em **115200 baud**. Deve aparecer `Controle web: http://192.168.4.1`.

O perfil DOIT padrão reserva apenas 1.310.720 bytes para o programa; o firmware com Bluetooth e Wi-Fi compilado pelo usuário ocupou 1.674.851 bytes. Huge APP reserva 3.145.728 bytes. Essa configuração não oferece atualização OTA. Não há binário ESP32 pré-compilado nesta entrega.

## Android

O APK existente foi preservado e não requer recompilação para Bluetooth. Código em `android/`; JDK 17 ou 21 e Android SDK 34.

```bash
cd android
./gradlew testControleRoboDebugUnitTest assembleControleRoboDebug
```

O aplicativo continua chamado Controle_Robo, versão 1.5. A assinatura do APK distribuído é a original de desenvolvimento; compilar em outro ambiente pode mudar a assinatura.

## Testes locais

```bash
bash firmware/tests/run.sh
cd apk
sha256sum -c SHA256SUMS
```

Os testes locais não substituem a validação na placa. Os dois olhos voltaram a funcionar após desligar e ligar, conforme confirmação do usuário.
