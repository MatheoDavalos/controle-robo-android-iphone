#pragma once
#include <array>
#include <cassert>
#include <initializer_list>
#include "Arduino.h"
using ByteBlock = std::array<byte, 8>;
extern std::array<ByteBlock, 4> displayed;
template<unsigned columns, unsigned rows> class LedController {
 public:
  void init(unsigned) {}
  void setIntensity(unsigned) {}
  void clearMatrix() { displayed = {}; }
  void displayOnSegment(unsigned index, ByteBlock data) {
    assert(index < columns * rows);
    displayed[index] = data;
  }
};
