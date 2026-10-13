# Compilação

## Android

Requisitos: JDK 17 ou 21, Android SDK 34 e acesso às dependências Gradle na primeira
compilação. Abra a pasta `android` no Android Studio e deixe a IDE configurar o
SDK; `local.properties` é local e não deve ser versionado.

```bash
cd android
./gradlew testControleRoboDebugUnitTest assembleControleRoboDebug lintControleRoboDebug
```

O APK gerado fica em `app/build/outputs/apk/controleRobo/debug/` dentro de android.
A única variante de produto é `controleRobo`. O identificador instalado continua
`br.edu.robotcontroller.finalapp`; nome Controle_Robo, versão Android 1.5 (código 8).
O APK em `apk/` preserva o artefato final usado neste projeto. Reconstruir pode
produzir hash e assinatura diferentes. Nenhuma chave privada é distribuída.

## ESP32

1. Instale o suporte ESP32 da Espressif no Arduino IDE.
2. Instale ESP32Servo pelo gerenciador de bibliotecas.
3. Copie `firmware/libraries/LedController` para a pasta `libraries` do seu
   Sketchbook Arduino. A biblioteca fornecida é LedController 2.0.2.
4. Abra `firmware/HeadController/HeadController.ino`.
5. Selecione DOIT ESP32 DEVKIT V1 e a porta serial. Compile e carregue.
6. Abra o Monitor Serial em 115200 baud. O boot deve identificar
   `RobotControl elapsed-ramp-v3 (200 accel / 400 brake deg/s)`.

Head.cpp, Head.h e Motion.h são compilados automaticamente junto com o sketch.
Não há um binário ESP32 pré-compilado nesta entrega. A identificação dos servos
não foi informada; os pontos de calibração existentes foram preservados.

## Testes sem o robô

Na raiz, execute `bash firmware/tests/run.sh`. Requer Python 3 e g++ com suporte
C++11. Os testes de lógica e as tarefas simuladas não substituem a compilação
para ESP32 nem a avaliação dos motores físicos.
