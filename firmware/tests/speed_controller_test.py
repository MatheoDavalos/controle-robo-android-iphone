"""Compila a tarefa real com filas, relógio e servos simulados no computador."""
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
sketch = (root / 'HeadController/HeadController.ino').read_text()
start = sketch.index('void SpeedController(void* pvParameters)\n{')
end = sketch.index('\nvoid HeadController(void* pvParameters)\n{', start)
receiver_start = sketch.index('void DataReceiver( void* pvParameters )\n{')
receiver_end = sketch.index('// Recepção externa:', receiver_start)
assert 'queueSC = xQueueCreate(1, sizeof(DRPacket))' in sketch
source = r'''
#include "Motion.h"
#include <cassert>
#include <cstdint>
#include <deque>
#include <iostream>
#include <vector>
#include <functional>
#include <cstdio>
using TickType_t = uint32_t;
struct DRPacket { uint8_t val; uint32_t receivedMs = 0, sequence = 0; DRPacket(uint8_t v = 0) : val(v) {} };
std::deque<DRPacket> commands;
int queueSC = 1, queueHC = 2;
struct Input {
  std::deque<int> bytes;
  int available() { return bytes.size(); }
  int availableForWrite() { return 512; }
  void write(const uint8_t*, int) {}
  int read() { if (bytes.empty()) return -1; int v = bytes.front(); bytes.pop_front(); return v; }
} SerialBT, Serial;
void xQueueOverwrite(int queue, DRPacket* packet) {
  assert(queue == queueSC);
  commands.clear(); commands.push_back(*packet);
}
constexpr int pdPASS = 1;
TickType_t now = 0;
unsigned remaining = 0;
std::function<void()> scheduled;
struct Sample { unsigned ms; int left, right; };
std::vector<Sample> samples;
uint8_t lastFace = 255;
struct Servo {
  int angle = -1, pulse = -1;
  void writeMicroseconds(int value) { pulse = value; angle = ((value - 500) * 180 + 1000) / 2000; }
};
Servo LeftWheel, RightWheel;
TickType_t xTaskGetTickCount() { return now; }
uint32_t millis() { return now; }
TickType_t pdMS_TO_TICKS(unsigned ms) { return ms; }
int xQueueReceive(int queue, DRPacket* packet, int) {
  assert(queue == queueSC);
  if (commands.empty()) return 0;
  *packet = commands.front(); commands.pop_front(); return pdPASS;
}
void xQueueOverwrite(int queue, uint8_t* face) { assert(queue == queueHC); lastFace = *face; }
struct Done {};
void vTaskDelay(TickType_t period) {
  assert(period == 1);
  if (scheduled) samples.push_back({now, LeftWheel.angle, RightWheel.angle});
  now += period;
  if (scheduled) scheduled();
  if (--remaining == 0) throw Done{};
}
void vTaskDelayUntil(TickType_t* wake, TickType_t period) {
  assert(period == 1);
  *wake += period; now = *wake;
  if (--remaining == 0) throw Done{};
}
'''
source += sketch[receiver_start:receiver_end]
source += sketch[start:end]
source += r'''
void run(std::initializer_list<uint8_t> bytes, unsigned ticks) {
  commands.clear(); for (auto b : bytes) commands.push_back(b); now = 0; remaining = ticks; lastFace = 255;
  try { SpeedController(nullptr); } catch (Done&) {}
}
void receiveBatch() {
  now = 0; remaining = 1;
  try { DataReceiver(nullptr); } catch (Done&) {}
}
void deliver(uint8_t command) {
  // Executa o receptor real em uma fatia do agendador simulado.
  auto savedNow = now; auto savedRemaining = remaining;
  SerialBT.bytes.push_back(command); remaining = 1;
  try { DataReceiver(nullptr); } catch (Done&) {}
  now = savedNow; remaining = savedRemaining;
}
void sequence(unsigned heldMs) {
  samples.clear(); commands.clear(); now = 0; remaining = heldMs + 1600;
  scheduled = [heldMs] {
    if (now % 200 == 0)
      deliver(now < heldMs ? 0xfc : now < heldMs + 1000 ? 0xf4 : 0);
  };
  deliver(0xfc);
  try { SpeedController(nullptr); } catch (Done&) {}
  scheduled = nullptr;
  auto at = [](unsigned ms) { return samples.at(ms); };
  assert(at(heldMs - 1).left == 0 && at(heldMs - 1).right == 180);
  // A curva e a liberação começam a afetar a saída em até 5 ms.
  assert(at(heldMs + 5).right < 180);
  assert(at(heldMs + 400).left == 0 && at(heldMs + 400).right == 102);
  assert(at(heldMs + 1005).left > 0 && at(heldMs + 1005).right < 102);
  assert(at(heldMs + 1240).left == 93 && at(heldMs + 1240).right == 90);
  // Aquilo que o servo pode observar a cada pulso de 20 ms.
  for (unsigned ms = 20; ms < samples.size(); ms += 20) {
    assert(std::abs(at(ms).left - at(ms - 20).left) <= 8);
    assert(std::abs(at(ms).right - at(ms - 20).right) <= 8);
  }
}
int main() {
  sequence(1000); sequence(5000); sequence(20000);
  // Um pacote antigo, consumido tarde, não pode rearmar nem renovar o movimento.
  commands = {0xfc}; commands.front().receivedMs = 0;
  now = 1000; remaining = 10;
  try { SpeedController(nullptr); } catch (Done&) {}
  assert(LeftWheel.pulse == 1533 && RightWheel.pulse == 1500);
  commands = {0xfc}; now = 0; remaining = 802;
  scheduled = [] {
    if (now == 700) { DRPacket stale(0xfc); stale.receivedMs = 0; commands.push_back(stale); }
  };
  try { SpeedController(nullptr); } catch (Done&) {}
  scheduled = nullptr;
  assert(LeftWheel.pulse == 1533 && RightWheel.pulse == 1500);
  for (uint8_t moving : {0xfc, 0x6c}) {
    commands = {moving};
    SerialBT.bytes.assign(10000, moving);
    SerialBT.bytes.push_back(0);
    SerialBT.bytes.push_back(0xff); // Inválido após a parada não a substitui.
    receiveBatch();
    assert(SerialBT.bytes.empty());
    assert(commands.size() == 1 && commands.front().val == 0);
    assert(now == 1); // Todo o lote foi consumido em uma execução, não 10 ms/byte.
  }
  commands = {0};
  SerialBT.bytes = {0xfc, 0, 0x6c};
  receiveBatch(); assert(commands.front().val == 0x6c); // Retomada mais recente vence.
  Serial.bytes = {0xfc, 0};
  receiveBatch(); assert(Serial.bytes.empty() && commands.front().val == 0);
  SerialBT.bytes = {0xff, 0x01};
  commands.clear(); receiveBatch(); assert(commands.empty());
  run({0xfc, 0x6c}, 6); // Só o comando mais recente da fila define o destino.
  assert(LeftWheel.pulse > 1533 && RightWheel.pulse < 1500);
  assert(commands.empty() && lastFace == Motion::BACKWARD);
  run({0xfc, 0}, 100); // Zero mais recente prevalece.
  assert(LeftWheel.pulse == 1533 && RightWheel.pulse == 1500);
  assert(lastFace == Motion::STOPPED);
  run({0xfc}, 500);
  assert(LeftWheel.angle == 0 && RightWheel.angle == 180);
  assert(lastFace == Motion::FORWARD);
  run({0xfc}, 801); // Proteção existente de 800 ms continua funcionando.
  assert(LeftWheel.pulse == 1533 && RightWheel.pulse == 1500);
  assert(lastFace == Motion::STOPPED);
  run({0xf4}, 500); assert(lastFace == Motion::RIGHT);
  run({0xbc}, 500); assert(lastFace == Motion::LEFT);
  std::cout << "OK: recepcao de rajadas de 10000 bytes, parada mais recente, tarefa real, periodo, timeout e face\n";
}
'''
with tempfile.TemporaryDirectory() as directory:
    cpp = Path(directory) / 'test.cpp'
    cpp.write_text(source)
    exe = Path(directory) / 'test'
    subprocess.run(['g++', '-std=c++11', '-Wall', '-Wextra', '-Werror',
                    '-Wno-unused-parameter', '-DROBOT_TRACE=1', '-fsanitize=undefined',
                    '-I', str(root / 'HeadController'), str(cpp), '-o', str(exe)], check=True)
    subprocess.run([str(exe)], check=True)
