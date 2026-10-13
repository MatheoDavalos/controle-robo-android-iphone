# Hardware e protocolo

Placa: ESP32-WROOM-32 / DOIT ESP32 DEVKIT V1, Bluetooth clássico SPP e
Wi-Fi em modo ponto de acesso simultaneamente.

## Controle híbrido

O aplicativo Android continua conectando ao dispositivo Bluetooth `RobotUA`.
Para usar o controle web no iPhone ou computador:

1. Conecte-se à rede Wi-Fi `RobotUA-Controle`.
2. Use a senha `controle123`.
3. Abra `http://192.168.4.1` no navegador.
4. No iPhone, opcionalmente use **Compartilhar > Adicionar à Tela de Início**.

A página fica armazenada no próprio ESP32 e não precisa de internet. O primeiro
canal que enviar movimento recebe o controle por 600 ms. A concessão é renovada
por cada comando, liberada ao enviar parada e expira se o controlador ficar
silencioso. Bytes de parada enviados por um canal ocioso não bloqueiam o outro
canal. O timeout físico já existente continua levando os servos ao neutro após
800 ms sem um comando aceito.

O firmware usa somente bibliotecas fornecidas pelo pacote Arduino ESP32:
`BluetoothSerial`, `WiFi` e `WebServer`. Não é necessário instalar uma biblioteca
de WebSocket.

| Função | GPIO |
|---|---:|
| Servo da roda esquerda | 21 |
| Servo da roda direita | 19 |
| Matrizes MAX7219 — DIN | 23 |
| Matrizes MAX7219 — CLK | 18 |
| Matrizes MAX7219 — CS | 5 |

A face usa quatro matrizes 8×8: dois olhos e duas metades da boca. Os desenhos
estão orientados para a montagem validada neste projeto.

O comando de movimento tem um byte binário:

| Bits | Significado |
|---|---|
| 7 | Sentido da roda esquerda: 1 frente, 0 ré |
| 6–5 | Velocidade esquerda: 0 parada, 1 baixa, 2 média, 3 alta |
| 4 | Sentido da roda direita: 1 frente, 0 ré |
| 3–2 | Velocidade direita: 0 parada, 1 baixa, 2 média, 3 alta |
| 1–0 | Reservados: zero |

Exemplos: FC frente máxima; F4 frente/direita; BC frente/esquerda; 6C ré máxima;
00 parada. São bytes binários, não texto ASCII enviado pelo Monitor Serial.

A recepção usa apenas o último comando válido. A rampa avança pelo tempo decorrido,
com aceleração de 200 graus/s e frenagem de 400 graus/s. São taxas do comando do
servo, não velocidades físicas medidas. Os pulsos neutros são RE=1533 us e
RD=1500 us. A falta de comandos válidos por 800 ms leva ao neutro.

Expressões: parado neutro; frente sorriso; ré olha para cima com boca O; curvas
à frente usam as duas caras personalizadas. Nas curvas em ré, a expressão de ré
prevalece. Android e navegador podem permanecer conectados ao mesmo tempo, mas a
arbitragem aceita movimento de apenas um controlador por vez.
