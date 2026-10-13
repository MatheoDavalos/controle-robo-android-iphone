#pragma once

#include <stdint.h>

namespace HybridControl {

// Evite o nome SERIAL: Arduino.h o define como macro no core ESP32 3.x.
enum class Source : uint8_t { NONE, BLUETOOTH, WEB, USB_SERIAL };

// A fonte que envia o primeiro comando de movimento recebe uma concessao curta.
// Bytes zero nao tomam o controle quando ele esta livre, evitando que um Android
// conectado e parado bloqueie o navegador.
class Arbiter {
 public:
  static constexpr uint32_t LEASE_MS = 600;

  bool accept(Source source, uint8_t command, uint32_t nowMs) {
    if ((command & 3) != 0) return false;
    expire(nowMs);
    if (owner_ == Source::NONE) {
      if (command == 0) return true;
      owner_ = source;
    }
    if (owner_ != source) return false;
    lastOwnerCommandMs_ = nowMs;
    if (command == 0) owner_ = Source::NONE;
    return true;
  }

  Source owner(uint32_t nowMs) {
    expire(nowMs);
    return owner_;
  }

  const char* ownerName(uint32_t nowMs) {
    switch (owner(nowMs)) {
      case Source::BLUETOOTH: return "bluetooth";
      case Source::WEB: return "web";
      case Source::USB_SERIAL: return "serial";
      default: return "livre";
    }
  }

 private:
  void expire(uint32_t nowMs) {
    if (owner_ != Source::NONE &&
        static_cast<uint32_t>(nowMs - lastOwnerCommandMs_) >= LEASE_MS)
      owner_ = Source::NONE;
  }

  Source owner_ = Source::NONE;
  uint32_t lastOwnerCommandMs_ = 0;
};

}  // namespace HybridControl
