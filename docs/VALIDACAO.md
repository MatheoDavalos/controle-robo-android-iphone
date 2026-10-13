# Validação da versão final

A versão elapsed-ramp-v3 foi escolhida pelo responsável pelo projeto como final.
Esta organização não altera o comportamento do aplicativo ou do robô.

- Android: 30 testes unitários, montagem do APK debug e análise lint.
- Firmware: 4.096 transições de comandos; ciclos de 1/5/10/20 ms; limites PWM;
  frenagem; passagem pelo neutro na inversão; timeout e expressões da face.
- Integração simulada: frente por 1/5/20 s → direita → soltar; lotes de 10.000
  bytes; comando mais recente; descarte de comandos vencidos.
- A frenagem atinge o neutro em até 240 ms nos ciclos simulados até 20 ms.

Os testes de firmware rodam no computador com simulação de relógio, filas e
saída PWM. Não verificam o rádio Bluetooth, a alimentação ou a mecânica. Não há
registro de compilação ESP32 automatizada nesta entrega.

Para conferir no hardware: mantenha frente, adicione direção e solte movimento;
repita nos dois sentidos. Soltar apenas direção deve retornar ao movimento reto.

## Diagnóstico opcional

Defina ROBOT_TRACE como 1 no início do sketch e carregue novamente. As linhas
CTRL na serial (115200 baud) mostram comando, instante de recepção (rx), aceitação
(apply), ângulos e pulsos exatos (usL/usR). Logs são amostrados e descartados se a
saída estiver cheia, para não bloquear o controle.

Se 00 chega tarde, investigue o caminho de recepção. Se os pulsos chegam ao neutro
mas a roda continua, confira calibração, alimentação e servo. O modelo dos servos
não foi informado.
