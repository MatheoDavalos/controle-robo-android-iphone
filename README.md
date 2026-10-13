# Controle Híbrido — Android e iPhone

Controle do robô ESP32 por **Bluetooth no Android** ou **Wi-Fi no navegador do iPhone**.

## Android: baixar o aplicativo

[**Baixar Controle_Robo.apk**](https://github.com/MatheoDavalos/controle-hibrido/raw/refs/heads/main/apk/Controle_Robo.apk)

Requer Android 8.0 ou superior. Baixe usando internet, abra o APK e permita a instalação pela fonte escolhida se solicitado. Pareie **RobotUA** nas configurações Bluetooth, abra **Controle_Robo** e conecte. O aplicativo é o mesmo da versão normal, com a assinatura original preservada; o modo Wi-Fi está no firmware do robô.

![Baixar APK Android](feira/qr-android.png)

## iPhone: controlar pelo Wi-Fi

Não precisa instalar aplicativo. O robô precisa estar ligado e com o firmware híbrido carregado.

1. Leia o QR de Wi-Fi abaixo ou entre em **Ajustes > Wi-Fi > RobotUA-Controle**. Senha: **controle123**.
2. Permaneça conectado mesmo que o iPhone indique que a rede não tem internet.
3. Abra **http://192.168.4.1** no Safari, ou leia o QR de controle.
4. Opcional: **Compartilhar > Adicionar à Tela de Início**.

| 1 — Entrar no Wi-Fi | 2 — Abrir o controle |
|---|---|
| ![Conectar ao Wi-Fi](feira/qr-wifi.png) | ![Abrir controle](feira/qr-controle-hibrido.png) |

O endereço do controle só funciona conectado ao Wi-Fi do robô. A página está no ESP32 e não depende do GitHub ou da internet. Android também pode usar esse modo pelo navegador.

## Na feira

[Cartaz para impressão A4](feira/cartaz-feira.png) · [Instruções](feira/INSTALAR.md)

Combine a vez com o responsável: um celular opera por vez. Solte os controles e desconecte ao terminar. O APK e o navegador usam o mesmo protocolo de movimento.

## Código e montagem

- `android/`: código do aplicativo Android.
- `apk/`: aplicativo e checksum SHA-256.
- `firmware/HeadController/`: sketch híbrido completo, incluindo a página web.
- `firmware/libraries/`: biblioteca de LEDs do projeto original.
- `feira/`: cartaz e QR Codes em PNG/SVG.
- `docs/`: compilação, hardware e validação.

Abra **firmware/HeadController/HeadController.ino** no Arduino IDE mantendo todos os arquivos dessa pasta juntos. Para ESP32-WROOM-32 de 4 MB, selecione **ESP32 Dev Module**, **Flash Size: 4MB**, **Partition Scheme: Huge APP (3MB No OTA/1MB SPIFFS)** e **Flash Mode: DIO**. A configuração DOIT padrão de 1,31 MB não comporta o firmware híbrido. Consulte [compilação](docs/COMPILAR.md).

## Estado da versão híbrida

Os testes de lógica passaram, mas a validação física conjunta de Bluetooth/Wi-Fi ainda está pendente. O usuário confirmou que os dois olhos funcionam após desligar e ligar o robô; a causa da falha inicial não foi confirmada. O histórico de validação herdado da versão normal não certifica o firmware híbrido.

A arbitragem atual separa Bluetooth, web e serial, mas não distingue dois navegadores. O timeout físico é de 800 ms; a concessão de controle expira em 600 ms. O transporte web implementado usa HTTP, não WebSocket. Use apenas um operador por vez.

## Créditos

Derivado do [controle normal](https://github.com/MatheoDavalos/controle-robo). Autoria de Luciano Neris e créditos do projeto preservados nos fontes. Veja [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
