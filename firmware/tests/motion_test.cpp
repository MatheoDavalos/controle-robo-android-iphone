#include "../HeadController/Motion.h"
#include <cassert>
#include <cstdlib>
#include <iostream>
using namespace Motion;
int target(uint8_t command, bool left) {
  const int level = (command >> (left ? 5 : 2)) & 3;
  const bool forward = (command & (left ? 0x80 : 0x10)) != 0;
  const int decreasing[] = {0, 77, 45, 0}, increasing[] = {0, 102, 135, 180};
  if (!level) return left ? 93 : 90;
  return (left ? forward : !forward) ? decreasing[level] : increasing[level];
}
int pulse(int angle) { return 500 + angle * 2000 / 180; }
void settle(Controller& m, uint32_t& now, int command) {
  assert(m.accept(command));
  for (int i = 0; i < 200; ++i) {
    const int l = m.leftPulseUs(), r = m.rightPulseUs();
    now += 5; m.tick(now);
    assert(std::abs(m.leftPulseUs() - l) <= 23);
    assert(std::abs(m.rightPulseUs() - r) <= 23);
    assert(m.leftPulseUs() >= 500 && m.leftPulseUs() <= 2500);
    assert(m.rightPulseUs() >= 500 && m.rightPulseUs() <= 2500);
    assert(m.accept(command)); // Repetir o comando não reinicia a rampa.
  }
  assert(m.leftPulseUs() == pulse(target(command, true)));
  assert(m.rightPulseUs() == pulse(target(command, false)));
}
int main() {
  for (int a = 0; a < 256; a += 4) for (int b = 0; b < 256; b += 4) {
    Controller m; uint32_t now = 0;
    settle(m, now, a); settle(m, now, b);
  }
  // Mesma evolução física mesmo que o agendador execute em 1, 5, 10 ou 20 ms.
  for (unsigned dt : {1u, 5u, 10u, 20u}) for (int cmd : {0xfc, 0x6c}) {
    Controller m; m.accept(cmd); uint32_t now = 0;
    for (; now < 200; ) { now += dt; m.tick(now); }
    assert(m.curL == (cmd == 0xfc ? 53 : 133));
    for (; now < 500; ) { now += dt; m.tick(now); }
    assert(m.leftPulseUs() == pulse(target(cmd, true)));
    m.accept(cmd == 0xfc ? 0xf4 : 0x64);
    const int beforeCurve = m.rightPulseUs(); now += dt; m.tick(now);
    assert(m.rightPulseUs() != beforeCurve);
    m.accept(0); const auto stoppedAt = now;
    const int l = m.leftPulseUs(); now += dt; m.tick(now);
    assert(m.leftPulseUs() != l); // Frenagem começa no primeiro ciclo.
    while (now - stoppedAt < 240) { now += dt; m.tick(now); m.accept(0); }
    assert(m.leftPulseUs() == pulse(93) && m.rightPulseUs() == pulse(90));
    assert(m.face() == STOPPED);
  }
  Controller delayed;
  delayed.accept(0xfc); delayed.tick(5); assert(delayed.curL == 92);
  delayed.tick(500); assert(delayed.curL == 88); // Recuperação limitada a 20 ms.
  const int previous = delayed.leftPulseUs();
  for (int i = 0; i < 100; ++i) delayed.tick(500);
  assert(delayed.leftPulseUs() == previous);
  assert(!delayed.accept(0xff));
  delayed.timeout(); delayed.tick(505);
  assert(delayed.leftPulseUs() == pulse(93) && delayed.face() == STOPPED);
  Controller wrapping(UINT32_MAX - 2);
  wrapping.accept(0xfc); wrapping.tick(2); assert(wrapping.curL == 92);
  // Nunca cruza o neutro para inverter sem antes atingi-lo.
  Controller reverse; uint32_t now = 0; settle(reverse, now, 0xfc);
  reverse.accept(0x6c); bool reachedNeutral = false;
  for (int i = 0; i < 200; ++i) {
    now += 5; reverse.tick(now);
    if (reverse.leftPulseUs() == pulse(93)) reachedNeutral = true;
    if (reverse.leftPulseUs() > pulse(93)) assert(reachedNeutral);
  }
  const int commands[] = {0xfc, 0x6c, 0xf4, 0xbc, 0x64, 0x2c};
  const Face faces[] = {FORWARD, BACKWARD, RIGHT, LEFT, BACKWARD, BACKWARD};
  for (int i = 0; i < 6; ++i) {
    Controller m; uint32_t t = 0; settle(m, t, commands[i]); assert(m.face() == faces[i]);
  }
  std::cout << "OK: 4096 transicoes, periodos 1/5/10/20 ms, frenagem, PWM, timeout e inversao\n";
}
