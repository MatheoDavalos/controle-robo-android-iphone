#include <cassert>
#include <iostream>
#include "../HeadController/ControlArbiter.h"

using HybridControl::Arbiter;
using HybridControl::Source;

int main() {
  Arbiter arbiter;

  // Heartbeats parados nao reservam o robo.
  assert(arbiter.accept(Source::BLUETOOTH, 0x00, 0));
  assert(arbiter.owner(0) == Source::NONE);

  // O primeiro movimento ganha a concessao.
  assert(arbiter.accept(Source::WEB, 0xFC, 10));
  assert(arbiter.owner(10) == Source::WEB);
  assert(!arbiter.accept(Source::BLUETOOTH, 0x6C, 20));
  assert(!arbiter.accept(Source::BLUETOOTH, 0x00, 30));

  // O dono pode atualizar e liberar imediatamente com parada.
  assert(arbiter.accept(Source::WEB, 0xF4, 100));
  assert(arbiter.accept(Source::WEB, 0x00, 120));
  assert(arbiter.owner(120) == Source::NONE);
  assert(arbiter.accept(Source::BLUETOOTH, 0x6C, 121));

  // Uma fonte silenciosa perde a concessao automaticamente.
  assert(arbiter.owner(121 + Arbiter::LEASE_MS - 1) == Source::BLUETOOTH);
  assert(arbiter.owner(121 + Arbiter::LEASE_MS) == Source::NONE);
  assert(arbiter.accept(Source::WEB, 0xFC, 800));

  // Bits reservados nunca sao aceitos.
  assert(!arbiter.accept(Source::WEB, 0xFF, 801));
  assert(!arbiter.accept(Source::USB_SERIAL, 0xFC, 802));
  std::cout << "control_arbiter_test: OK\n";
}
