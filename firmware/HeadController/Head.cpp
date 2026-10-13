// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

#include "Head.h"

Head::Head()
{       
    ledMats.init(CS);			// Initialize Leds Matrices
    ledMats.setIntensity(5);    // (0..15)
    ledMats.clearMatrix();   
}

void Head::SetEyes(int eye)
{
    SetLeftEye(eye);
    SetRightEye(eye);    
}

void Head::SetLeftEye(int eye)
{
  ledMats.displayOnSegment(LEFT_EYE, eyes[eye]);  
}

void Head::SetRightEye(int eye)
{
  ledMats.displayOnSegment(RIGHT_EYE, eyes[eye]);  
}

void Head::SetMouth(int mouth)
{
  ledMats.displayOnSegment(MOUTH_LEFT_SEG, mouths[(mouth * 2)+1]);   
  ledMats.displayOnSegment(MOUTH_RIGHT_SEG, mouths[mouth *2]);    
}

void Head::Update(byte cmd)
{
  byte part = cmd & 0b00000111;
  byte look  = (cmd & 0b11111000) >> 3;

  //Serial.println(part);
  //Serial.println(look);
  //Serial.println(" ");  

  switch (part) 
  {
    case 0:                   // disable
      ledMats.clearMatrix();   
      break; 
    case 1:                   // face
      UpdateFace(look);  
      break; 
    case 2:                   // eyes
      UpdateEyes(look);   
      break;   
    case 3:                   // left eye
      UpdateLeftEye(look);   
      break;  
    case 4:                   // right eye
      UpdateRightEye(look);   
      break;  
    case 5:                   // mouth
      UpdateMouth(look);   
      break;  
  }  
}

void Head::UpdateFace(byte look)
{
  // Caras completas adaptadas às matrizes 8x8 dos olhos e 16x8 da boca.
  // Mantém a orientação confirmada nas fotos: boca 3 à esquerda, 2 à direita.
  if (look == 3) { // Direita: primeira referência, olhos grandes e sorriso pequeno.
    ledMats.displayOnSegment(LEFT_EYE, {0x1e,0,0x3c,0x42,0x81,0x99,0x99,0x7e});
    ledMats.displayOnSegment(RIGHT_EYE, {0x78,0,0x3c,0x42,0x81,0x99,0x99,0x7e});
    ledMats.displayOnSegment(3, {0,0,0,0,0,0x08,0x07,0});
    ledMats.displayOnSegment(2, {0,0,0,0,0,0x10,0xe0,0});
    return;
  }
  if (look == 4) { // Esquerda: segunda referência, olhos com brilho e boca inclinada.
    ledMats.displayOnSegment(LEFT_EYE, {0x03,0x3e,0x66,0x63,0xff,0xe7,0x7e,0x3c});
    ledMats.displayOnSegment(RIGHT_EYE, {0xc0,0x7c,0x66,0x63,0xff,0xe7,0x7e,0x3c});
    ledMats.displayOnSegment(3, {0,0,0,0,0,0x01,0x06,0x08});
    ledMats.displayOnSegment(2, {0,0,0x10,0x60,0x80,0,0,0});
    return;
  }
  switch (look) 
  {
    case 1:                   // happy
      SetEyes(static_cast<int>(Eye::neutral));
      SetMouth(static_cast<int>(Mouth::happy));      
      break; 
    case 2:                   // sad
      SetEyes(static_cast<int>(Eye::partially_open));
      SetMouth(static_cast<int>(Mouth::sad));      
      break;     
  }  
}

void Head::UpdateEyes(byte look)
{
  //NOTICE that the switch starts in 1 instead of 0, so PLEASE add 1 to your calculations
  switch (look) 
  {
    case 1:                   
      SetEyes(static_cast<int>(Eye::neutral));
      break; 
    case 2:
      SetEyes(static_cast<int>(Eye::closed));
      break; 
    case 3:
      SetEyes(static_cast<int>(Eye::partially_closed));
      break; 
    case 4:
      SetEyes(static_cast<int>(Eye::partially_open));
      break; 
    case 5:
      SetEyes(static_cast<int>(Eye::slight_left));
      break; 
    case 6:
      SetEyes(static_cast<int>(Eye::left));
      break; 
    case 7:
      SetEyes(static_cast<int>(Eye::realleft));
      break; 
    case 8:
      SetEyes(static_cast<int>(Eye::slight_right));
      break; 
    case 9:
      SetEyes(static_cast<int>(Eye::right));
      break; 
    case 10:
      SetEyes(static_cast<int>(Eye::real_right));
      break;         
    case 11:
      SetEyes(static_cast<int>(Eye::up));
      break;
  }   
}

void Head::UpdateLeftEye(byte look)
{
  switch (look) 
  {
    case 1:
      SetLeftEye(static_cast<int>(Eye::neutral));
      break; 
    case 2:
      SetLeftEye(static_cast<int>(Eye::closed));
      break; 
    case 3:
      SetLeftEye(static_cast<int>(Eye::partially_closed));
      break; 
    case 4:
      SetLeftEye(static_cast<int>(Eye::partially_open));
      break; 
    case 5:
      SetLeftEye(static_cast<int>(Eye::slight_left));
      break; 
    case 6:
      SetLeftEye(static_cast<int>(Eye::left));
      break; 
    case 7:
      SetLeftEye(static_cast<int>(Eye::realleft));
      break; 
    case 8:
      SetLeftEye(static_cast<int>(Eye::slight_right));
      break; 
    case 9:
      SetLeftEye(static_cast<int>(Eye::right));
      break; 
    case 10:
      SetLeftEye(static_cast<int>(Eye::real_right));
      break;     
    case 11:
      SetLeftEye(static_cast<int>(Eye::up));
      break;
  }      
}

void Head::UpdateRightEye(byte look)
{
  switch (look) 
  {
    case 1:
      SetRightEye(static_cast<int>(Eye::neutral));
      break; 
    case 2:
      SetRightEye(static_cast<int>(Eye::closed));
      break; 
    case 3:
      SetRightEye(static_cast<int>(Eye::partially_closed));
      break; 
    case 4:
      SetRightEye(static_cast<int>(Eye::partially_open));
      break; 
    case 5:
      SetRightEye(static_cast<int>(Eye::slight_left));
      break; 
    case 6:
      SetRightEye(static_cast<int>(Eye::left));
      break; 
    case 7:
      SetRightEye(static_cast<int>(Eye::realleft));
      break; 
    case 8:
      SetRightEye(static_cast<int>(Eye::slight_right));
      break; 
    case 9:
      SetRightEye(static_cast<int>(Eye::right));
      break; 
    case 10:
      SetRightEye(static_cast<int>(Eye::real_right));
      break;     
    case 11:
      SetRightEye(static_cast<int>(Eye::up));
      break;
  }
}

void Head::UpdateMouth(byte look)
{
  switch (look) 
  {
    case 1:
      SetMouth(static_cast<int>(Mouth::happy));
      break; 
    case 2:
      SetMouth(static_cast<int>(Mouth::sad));
      break; 
    case 3:
      SetMouth(static_cast<int>(Mouth::very_happy));
      break; 
    case 4:
      SetMouth(static_cast<int>(Mouth::partially_open));
      break; 
    case 5:
      SetMouth(static_cast<int>(Mouth::neutral));
      break; 
    case 6:
      SetMouth(static_cast<int>(Mouth::opened));
      break;                  
  }
}        
