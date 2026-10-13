#include "../HeadController/Head.h"
#include <iostream>
std::array<ByteBlock, 4> displayed;
bool empty(ByteBlock block) { for (auto v : block) if (v) return false; return true; }
int main() {
  Head head;
  for (byte look = 1; look <= 6; ++look) {
    head.Update(0);
    head.Update((look << 3) | 5);
    assert(!empty(displayed[2]) && !empty(displayed[3]));
  }
  head.Update((5 << 3) | 5); // Neutro: traço, e não boca aberta.
  assert(displayed[3][0] == 0 && displayed[3][3] == 0x3f);
  assert(displayed[2][3] == 0xfc && displayed[2][4] == 0xfc);
  head.Update((6 << 3) | 5);
  assert(displayed[3][0] == 0x03 && displayed[2][0] == 0xc0);
  assert(displayed[3][3] == 0x10 && displayed[2][3] == 0x08);
  // Simetria da boca montada e continuidade nas duas metades.
  for (byte look = 1; look <= 6; ++look) {
    head.Update((look << 3) | 5);
    for (int y = 0; y < 8; ++y) {
      for (int x = 0; x < 8; ++x)
        assert(((displayed[3][y] >> x) & 1) == ((displayed[2][y] >> (7-x)) & 1));
    }
  }
  head.Update((1 << 3) | 5);
  assert(displayed[3][1] == 0x80 && displayed[3][6] == 0x0f);
  assert(displayed[2][1] == 0x01 && displayed[2][6] == 0xf0);
  for (byte part = 2; part <= 4; ++part) {
    head.Update(0); head.Update((11 << 3) | part);
    if (part != 4) assert(displayed[0][1] == 0x5a && displayed[0][3] == 0x81);
    if (part != 3) assert(displayed[1][1] == 0x5a && displayed[1][3] == 0x81);
  }
  // As caras completas substituem todos os segmentos, sem resíduos da anterior.
  head.Update((3 << 3) | 1);
  const auto rightFace = displayed;
  for (auto block : displayed) assert(!empty(block));
  head.Update((4 << 3) | 1);
  const auto leftFace = displayed;
  for (unsigned segment = 0; segment < 4; ++segment) {
    assert(!empty(displayed[segment]));
    assert(leftFace[segment] != rightFace[segment]);
  }
  head.Update((3 << 3) | 1);
  assert(displayed == rightFace);
  head.Update((1 << 3) | 1); // Volta ao sorriso normal de frente.
  assert(displayed[3][1] == 0x80 && displayed[0][3] == 0x99);
  const auto before = displayed;
  head.Update((31 << 3) | 2);
  assert(displayed == before);
  std::cout << "OK: seis bocas, olhos para cima e comandos invalidos\n";
}
