/*
******************************************************************************************
  Head   Autor:       Luciano Neris
  Version:     V 0.1 
  Processor:   
  Description: This class is intend to 
                                            
  Date:        25/12/2023
******************************************************************************************
*/

#pragma once

// Includes
// ----------------------------------------------------------------------------
#include "Arduino.h"
#include "LedController.hpp"

// Consts
// ----------------------------------------------------------------------------


// Defines
// ----------------------------------------------------------------------------

#define HARDWARE_TYPE MD_MAX72XX::FC16_HW   // MAX7219

#define CS 05   							              // SPI CS pins 

#define HEAD_PARTS 2  						          // Rows of LED matrices (eyes + mouth)
#define EYES_PART 0                         // First Row
#define MOUTH_PART 1                        // Second Row

#define SEGS_EYES 2							            // The number of segments per part
#define LEFT_EYE 0
#define RIGHT_EYE 1

#define SEGS_MOUTH 2						             // The number of segment per part
#define MOUTH_LEFT_SEG 2
#define MOUTH_RIGHT_SEG 3

enum class Eye
{
	neutral,
	closed,
	partially_closed,
	partially_open,
	slight_left,
	left,
	realleft,
	slight_right,
	right,
	real_right,
    up
};

enum class Mouth
{
	happy,
	sad,
	very_happy,
	partially_open,
	neutral,
	opened
};

// Classes
// ----------------------------------------------------------------------------
class Head
{
  public:
    Head();  

    void Update(byte cmd);
       
  private:
  	LedController<SEGS_EYES + SEGS_MOUTH, 1> ledMats = LedController<SEGS_EYES + SEGS_MOUTH, 1>();   

    void SetEyes(int eye);
    
    void SetLeftEye(int eye);
    
    void SetRightEye(int eye);

    void SetMouth(int mouth);  

    void UpdateFace(byte look);
  
    void UpdateEyes(byte look);

    void UpdateLeftEye(byte look);

    void UpdateRightEye(byte look);
 
    void UpdateMouth(byte look);

    // Bitmaps em linhas, de cima para baixo. Nas fotos, a boca usa
    // bit 7 à esquerda; os olhos usam bit 7 à direita (módulos espelhados).
    ByteBlock eyes[11] = {
      {0x3c,0x42,0x81,0x99,0x99,0x81,0x42,0x3c}, // neutral
      {0,0,0,0xff,0xff,0,0,0},                   // closed
      {0,0,0x7e,0x99,0x99,0x7e,0,0},             // partially_closed
      {0,0x3c,0x42,0x99,0x99,0x42,0x3c,0},       // partially_open
      {0x3c,0x42,0x81,0xb1,0xb1,0x81,0x42,0x3c}, // slight_left
      {0x3c,0x42,0x81,0xe1,0xe1,0x81,0x42,0x3c}, // left do robô
      {0x3c,0x42,0x81,0xe1,0xe1,0x81,0x42,0x3c}, // realleft
      {0x3c,0x42,0x81,0x8d,0x8d,0x81,0x42,0x3c}, // slight_right
      {0x3c,0x42,0x81,0x87,0x87,0x81,0x42,0x3c}, // right do robô
      {0x3c,0x42,0x81,0x87,0x87,0x81,0x42,0x3c}, // real_right
      {0x3c,0x5a,0x99,0x81,0x81,0x81,0x42,0x3c}  // up
    };

    // Cada par: metade esquerda da foto (segmento 3), depois direita
    // (segmento 2). Desenhos contínuos de 16 x 8, sem rotações por expressão.
    ByteBlock mouths[12] = {
      {0,0x80,0xc0,0x60,0x30,0x1f,0x0f,0}, // happy esquerda
      {0,0x01,0x03,0x06,0x0c,0xf8,0xf0,0}, // happy direita
      {0,0x0f,0x1f,0x30,0x60,0xc0,0x80,0}, // sad
      {0,0xf0,0xf8,0x0c,0x06,0x03,0x01,0},
      {0,0x7f,0x40,0x40,0x20,0x10,0x0f,0}, // very_happy
      {0,0xfe,0x02,0x02,0x04,0x08,0xf0,0},
      {0,0,0x0f,0x10,0x10,0x0f,0,0},       // partially_open
      {0,0,0xf0,0x08,0x08,0xf0,0,0},
      {0,0,0,0x3f,0x3f,0,0,0},            // neutral horizontal
      {0,0,0,0xfc,0xfc,0,0,0},
      {0x03,0x0c,0x08,0x10,0x10,0x08,0x0c,0x03}, // opened / O
      {0xc0,0x30,0x10,0x08,0x08,0x10,0x30,0xc0}
    };
};
