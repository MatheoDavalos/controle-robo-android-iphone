#pragma once
#include <stdint.h>

namespace Motion {
constexpr unsigned PERIOD_MS = 1; // Consulta o comando mais recente.
constexpr unsigned ACCEL_DEGREES_PER_SECOND = 200;
constexpr unsigned BRAKE_DEGREES_PER_SECOND = 400;
constexpr unsigned MAX_STEP_MS = 20; // Não recupera pausas longas em um salto.
constexpr unsigned TIMEOUT_MS = 800; // Proteção já existente no projeto.
enum Face : uint8_t { STOPPED, FORWARD, BACKWARD, LEFT, RIGHT };

// Funções do colega; apenas os nomes RAPIDO/MEDIO/LENTO foram substituídos
// pelos respectivos valores para manter este arquivo independente do Arduino.
static int speedToAngle(uint8_t vel, bool negativeDir, int stopAngle)
{
  switch (vel) {
    case 3: return negativeDir ? 0   : 180;
    case 2:  return negativeDir ? 45  : 135;
    case 1:  return negativeDir ? 77  : 102;
    default:     return stopAngle;          // PARADO
  }
}


class Controller {
 public:
  explicit Controller(uint32_t nowMs = 0) : lastStepMs(nowMs) {}
  uint8_t curL = 93, curR = 90;
  bool accept(uint8_t command) {
    if (command & 3) return false;
    bool LeftFlag = (command >> 7) & 0x01;
    uint8_t LeftVelocity = (command >> 5) & 0x03;
    bool RightFlag = (command >> 4) & 0x01;
    uint8_t RightVelocity = (command >> 2) & 0x03;
    tgtL = speedToAngle(LeftVelocity, LeftFlag, 93);
    tgtR = speedToAngle(RightVelocity, !RightFlag, 90);
    // Mantém as caras atuais; ao frear, só mostra parado ao chegar ao neutro.
    if (LeftVelocity || RightVelocity) {
      if ((!LeftVelocity || !LeftFlag) && (!RightVelocity || !RightFlag))
        movingFace = BACKWARD;
      else if (LeftVelocity > RightVelocity) movingFace = RIGHT;
      else if (LeftVelocity < RightVelocity) movingFace = LEFT;
      else movingFace = FORWARD;
    }
    return true;
  }
  void timeout() {
    curL = tgtL = 93; curR = tgtR = 90;
    milliL = 93000; milliR = 90000;
  }
  void tick(uint32_t nowMs) {
    uint32_t elapsed = static_cast<uint32_t>(nowMs - lastStepMs);
    lastStepMs = nowMs;
    if (elapsed > MAX_STEP_MS) elapsed = MAX_STEP_MS;
    milliL = advance(milliL, tgtL * 1000, 93000, elapsed);
    milliR = advance(milliR, tgtR * 1000, 90000, elapsed);
    curL = milliL / 1000;
    curR = milliR / 1000;
  }
  int leftPulseUs() const { return 500 + milliL * 2000 / 180000; }
  int rightPulseUs() const { return 500 + milliR * 2000 / 180000; }
  Face face() const {
    return milliL == 93000 && milliR == 90000 ? STOPPED : movingFace;
  }
 private:
  // Milésimos de grau: mantém a fração entre ciclos e evita arredondar
  // repetidamente para zero quando a tarefa é executada mais rapidamente.
  static int advance(int current, int target, int neutral, uint32_t elapsed) {
    const int offset = current - neutral;
    const int wanted = target - neutral;
    const bool reversing = (offset < 0 && wanted > 0) || (offset > 0 && wanted < 0);
    if (reversing) target = neutral; // Primeiro freia; não cruza o neutro em um passo.
    const int distance = offset < 0 ? -offset : offset;
    const int targetDistance = target < neutral ? neutral - target : target - neutral;
    const bool braking = targetDistance < distance;
    const int step = elapsed * (braking ? BRAKE_DEGREES_PER_SECOND : ACCEL_DEGREES_PER_SECOND);
    if (current < target) return target - current < step ? target : current + step;
    return current - target < step ? target : current - step;
  }
  int milliL = 93000, milliR = 90000;
  uint32_t lastStepMs = 0;
  uint8_t tgtL = 93, tgtR = 90;
  Face movingFace = STOPPED;
};
}
