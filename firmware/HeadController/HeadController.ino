
/*
******************************************************************************************
  Head Controller
  Autor:       Luciano Neris
  Version:     V 0.1 
  Date:        24/02/2023  
  Processor:   ESP32 DEVKIT DOIT board (ESP32-WROOM-32 - Dual Core 4 MB flash 40 MHz crystal)

  Description: This program change parts of head (eye, mouth, etc.) by commands read from
                bluetooth module. The parts of head are LED Matrix 8x8 controlled by MAX7219
               
                Frame: 1 byte ->  [look]    [part]
                                   00000     000

                                  part ->  0:disable, 1:face, 2:eyes, 3:left eye, 4:right eye, 5:mouth
                                           look face -> 1: happy, 2:sad
                                           look eyes -> 1:neutral , 2:closed , 3:partially_closed , 4:partially_open , 5:slight_left , 6:left , 7:realleft , 8:slight_right , 9:right , 10: real_right
                                           look left eye -> 1:neutral , 2:closed , 3:partially_closed , 4:partially_open , 5:slight_left , 6:left , 7:realleft , 8:slight_right , 9:right , 10: real_right  
                                           look right eye -> 1:neutral , 2:closed , 3:partially_closed , 4:partially_open , 5:slight_left , 6:left , 7:realleft , 8:slight_right , 9:right , 10: real_right  
                                           look eyes -> 11: up (adicionado)
                                           look mouth -> 1: happy, 2:sad, 3: verry_happy, 4:partially_open, 5:neutral, 6:opened
                                           
Pinout
  ESP32   G23     -> LED Matrix DIN
  ESP32   G18     -> LED Matrix CLK
  ESP32   G05     -> LED Matrix CS        
  
  ESP32   G02     -> LED BUILTIN
  
  https://curtocircuito.com.br/modulo-matriz-de-led-8x8-max7219.html
  https://www.baudaeletronica.com.br/produto/placa-doit-esp32-bluetooth-e-wifi.html
  

******************************************************************************************  
*/

// Includes
// ----------------------------------------------------------------------------
#include "Arduino.h"
#include "BluetoothSerial.h"
#include <WiFi.h>
#include <WebServer.h>
#include "Head.h"
#include "Motion.h"
#include "ControlArbiter.h"
#include "WebControl.h"
#include <ESP32Servo.h>


// Constants
// ----------------------------------------------------------------------------
#ifndef ROBOT_TRACE
#define ROBOT_TRACE 0 // 1: diagnóstico serial não bloqueante, no máximo 10 linhas/s.
#endif

#define QDS_QUEUE_SIZE 64
#define QDR_QUEUE_SIZE 64

#define LED_BUILTIN 2

#define RAPIDO 0b11
#define MEDIO 0b10
#define LENTO 0b01
#define PARADO 0b00

#define FRENTE 1
#define TRAS 0
#define DIREITA 3
#define ESQUERDA 2

const int pinoServoRight = 19; 
const int pinoServoLeft = 21;

// Typedefs
// ----------------------------------------------------------------------------
typedef struct 
{
  uint8_t val;                // 
} DSPacket; //Data sender

typedef struct 
{
  uint8_t val;                // 
  uint32_t receivedMs;
  uint32_t sequence;
} DRPacket; //Data receiver


// Variables
// ----------------------------------------------------------------------------
Servo LeftWheel;
Servo RightWheel;

void DataSender( void *pvParameters );
void DataReceiver( void *pvParameters );
void SpeedController(void *pvParameters);
void HeadController(void *pvParameters);

void Setup( void *pvParameters );
void Demo( void *pvParameters );

TaskHandle_t TDataSenser = NULL;
TaskHandle_t TDataReceiver = NULL;
TaskHandle_t TSpeedController = NULL;
TaskHandle_t THeadController = NULL;

TaskHandle_t TUpdate = NULL;
TaskHandle_t TDemo = NULL;

static QueueHandle_t queueDS = NULL;
static QueueHandle_t queueSC = NULL;
static QueueHandle_t queueHC = NULL;
//static QueueHandle_t qDataRec = NULL;

//static volatile SemaphoreHandle_t mutex_v  = NULL;

BluetoothSerial SerialBT;
WebServer webServer(80);
HybridControl::Arbiter controlArbiter;

Head *head;

static void enqueueCommand(uint8_t value, uint32_t &sequence)
{
  DRPacket packet = {value, millis(), ++sequence};
  xQueueOverwrite(queueSC, &packet);
}

// Setup
// This function will only be run once after STAMP-PICO is started or reset .
// ----------------------------------------------------------------------------
void setup() 
{
    setCpuFrequencyMhz(240);
        
    // initialize digital LED_BUILTIN on pin 13 as an output.
    pinMode(LED_BUILTIN, OUTPUT);
    delay(100);
    digitalWrite(LED_BUILTIN, LOW);    // turn the LED off by making the voltage LOW    

    // Initializing the serial port at 115200 baud
#if ROBOT_TRACE
    Serial.setTxBufferSize(512);
#endif
    Serial.begin(115200); 
    delay(100);
    Serial.println("Serial Ok - RobotControl elapsed-ramp-v3 (200 accel / 400 brake deg/s)");    

    SerialBT.setTimeout(0); // Leitura de comando nunca espera por um próximo byte.
    SerialBT.begin("RobotUA");           // Bluetooth device name
    delay(1000); 
    Serial.println("Bluetooth Ok");

    // Rede local independente de internet para Safari/iPhone e outros navegadores.
    WiFi.mode(WIFI_AP);
    WiFi.setSleep(false);
    if (WiFi.softAP("RobotUA-Controle", "controle123")) {
      Serial.print("Controle web: http://");
      Serial.println(WiFi.softAPIP());
    } else {
      Serial.println("ERRO: nao foi possivel iniciar o Wi-Fi do controle.");
    }
    
    // Initialize Head
    head = new Head();       
    Serial.println("Leds Matrix Ok");
      
    queueDS = xQueueCreate(QDS_QUEUE_SIZE, sizeof(DSPacket));  //initialize the queue
    queueSC = xQueueCreate(1, sizeof(DRPacket)); //queue speed controller
    queueHC = xQueueCreate(1, sizeof(uint8_t)); // Head controler queue
    //qDataRec = xQueueCreate(QDR_QUEUE_SIZE, sizeof(DRPacket)); //initialize the queue
    
    //SERVO
    ESP32PWM::allocateTimer(0);
    ESP32PWM::allocateTimer(1);
    ESP32PWM::allocateTimer(2);
    ESP32PWM::allocateTimer(3);


    // Configura a frequência padrão para servos (50Hz)
    LeftWheel.setPeriodHertz(50);    
    RightWheel.setPeriodHertz(50);    
    // Conecta o servo definindo os tempos de pulso (mínimo de 500us e máximo de 2500us)
    LeftWheel.attach(pinoServoLeft, 500, 2500); 
    RightWheel.attach(pinoServoRight, 500, 2500);
    LeftWheel.write(93);
    RightWheel.write(90); 
   
    /*
      FreeRTOS will implicitly create an idle task of priority for each core. LoopTask runs in Core 1 (loopTask will run setup() once, then run loop());  
      Core 0 handle most protocol processing (PRO_CPU) and is solely responsible for keeping time in ESP-IDF FreeRTOS. 
      Core 1 is generally more free of resources, then application code run mostly on Core 1 (APP_CPU).
      WDT runs on core 0 -> disbale or yield to avoit reset
      Low priority numbers denote low priority tasks. The idle task has priority zero (tskIDLE_PRIORITY).
      https://docs.espressif.com/projects/esp-idf/en/latest/esp32/api-guides/freertos-smp.html
    */
    
    xTaskCreatePinnedToCore(DataSender, "DataSender", 4096, NULL, 1,  &TDataSenser, 0);  
    delay(100); // time to task creation 
    
    xTaskCreatePinnedToCore(DataReceiver, "DataReceiver", 4096, NULL, 1,  &TDataReceiver, 0);  
    delay(100); // time to task creation 

    xTaskCreatePinnedToCore(SpeedController, "SpeedController", 4096, NULL, 1,&TSpeedController, 1 );
    delay(100);

    xTaskCreatePinnedToCore(HeadController, "HeadController", 4096, NULL, 1, &THeadController, 1);
    delay(100);
    
    //xTaskCreatePinnedToCore( Update, "Update", 4096, NULL, 1,  &TUpdate, 1 );  
    delay(100); // time to task creation 

    //xTaskCreatePinnedToCore( Demo, "Demo", 4096, NULL, 1,  &TDemo, 1 );  
    //delay(100); // time to task creation     

    digitalWrite(LED_BUILTIN, HIGH);   // turn the LED on (HIGH is the voltage level)    

    vTaskDelete(NULL); // terminate myself so only task above is running      
}

// Main
// ----------------------------------------------------------------------------
void loop()
{
  // Empty. Things are done in Tasks.
}

// DataSender
// This task get data from queueDS queue and sends it to UART and Bluetooth channels 
// ----------------------------------------------------------------------------
// Envia informação para o celular
void DataSender( void* pvParameters )
{
  String str;
  char *pcTaskName;
  DSPacket dsPacket;
  TickType_t xLastWakeTime;
  
  pcTaskName = ( char * ) pvParameters;

  for(;;)
  {
    // Wait 10 ms if queue is empty (WDT won't be reset)
    if(xQueueReceive(queueDS, &(dsPacket), (TickType_t) pdMS_TO_TICKS(10)) == pdPASS) 
    {
      str = String(dsPacket.val) + '\r' + '\n';
      //Serial.print(str); 
      SerialBT.write((const uint8_t *)str.c_str(), str.length());     
    } 
  }
}

// DataReceiver
// This task get data from communication channels (UART and bluetooth) and 
// send them to Setup Task
// ----------------------------------------------------------------------------
void DataReceiver( void* pvParameters )
{
  uint32_t sequence = 0;
#ifndef ROBOT_HOST_TEST
  webServer.on("/", HTTP_GET, []() {
    webServer.send_P(200, "text/html; charset=utf-8", CONTROL_PAGE);
  });
  webServer.on("/api/status", HTTP_GET, []() {
    String json = String("{\"owner\":\"") + controlArbiter.ownerName(millis()) +
      "\",\"ip\":\"" + WiFi.softAPIP().toString() + "\"}";
    webServer.send(200, "application/json", json);
  });
  webServer.on("/api/command", HTTP_ANY, [&sequence]() {
    if (!webServer.hasArg("v")) {
      webServer.send(400, "application/json", "{\"error\":\"comando ausente\"}");
      return;
    }
    long parsed = webServer.arg("v").toInt();
    if (parsed < 0 || parsed > 255 || (parsed & 3) != 0) {
      webServer.send(400, "application/json", "{\"error\":\"comando invalido\"}");
      return;
    }
    uint32_t now = millis();
    if (!controlArbiter.accept(HybridControl::Source::WEB, static_cast<uint8_t>(parsed), now)) {
      String json = String("{\"accepted\":false,\"owner\":\"") +
        controlArbiter.ownerName(now) + "\"}";
      webServer.send(409, "application/json", json);
      return;
    }
    enqueueCommand(static_cast<uint8_t>(parsed), sequence);
    webServer.send(200, "application/json", "{\"accepted\":true,\"owner\":\"web\"}");
  });
  webServer.onNotFound([]() { webServer.send(404, "text/plain", "Nao encontrado"); });
  webServer.begin();
#endif
  TickType_t wake = xTaskGetTickCount();
  const TickType_t period = pdMS_TO_TICKS(1) ? pdMS_TO_TICKS(1) : 1;
  for (;;) {
#ifndef ROBOT_HOST_TEST
    webServer.handleClient();
#endif
    DRPacket latest;
    bool received = false;
    // Consome o lote já disponível, sem esperar novos bytes nem processar
    // comandos antigos um por vez. Limita o trabalho ao tamanho deste lote.
    int pending = SerialBT.available();
    for (int i = 0; i < pending; ++i) {
      int value = SerialBT.read();
      if (value < 0) break;
      if ((value & 3) != 0) continue;
#ifndef ROBOT_HOST_TEST
      if (!controlArbiter.accept(HybridControl::Source::BLUETOOTH,
          static_cast<uint8_t>(value), millis())) continue;
#endif
      latest.val = static_cast<uint8_t>(value);
      latest.receivedMs = millis();
      latest.sequence = ++sequence;
      received = true;
    }
    pending = Serial.available();
    for (int i = 0; i < pending; ++i) {
      int value = Serial.read();
      if (value < 0) break;
      if ((value & 3) != 0) continue;
#ifndef ROBOT_HOST_TEST
      if (!controlArbiter.accept(HybridControl::Source::USB_SERIAL,
          static_cast<uint8_t>(value), millis())) continue;
#endif
      latest.val = static_cast<uint8_t>(value);
      latest.receivedMs = millis();
      latest.sequence = ++sequence;
      received = true;
    }
    // A fila guarda somente o estado mais recente, inclusive o byte de parada.
    if (received) xQueueOverwrite(queueSC, &latest);
    vTaskDelayUntil(&wake, period);
  }
}

// Recepção externa: byte das rodas [RE dir][RE vel:2][RD dir][RD vel:2][00].
// Os comandos de face do cabeçalho são internos, enviados a Head::Update.
// Rampa e face compartilham o mesmo estado; nenhum delay bloqueia a recepção.
void SpeedController(void* pvParameters)
{
  Motion::Controller motion(millis());
  DRPacket packet;
  const TickType_t period = pdMS_TO_TICKS(Motion::PERIOD_MS) ? pdMS_TO_TICKS(Motion::PERIOD_MS) : 1;
  uint32_t lastCommandMs = millis();
  uint8_t lastFace = 255;
#if ROBOT_TRACE
  uint32_t loggedMs = 0, appliedMs = 0;
  DRPacket applied = {};
#endif
  for (;;) {
    // Mesmo consumo da fila usado pelo colega: fica com o último comando.
    bool newCmd = false;
    while (xQueueReceive(queueSC, &packet, 0) == pdPASS) newCmd = true;
    if (newCmd && static_cast<uint32_t>(millis() - packet.receivedMs) < Motion::TIMEOUT_MS
        && motion.accept(packet.val)) {
      lastCommandMs = packet.receivedMs;
#if ROBOT_TRACE
      applied = packet;
      appliedMs = millis();
#endif
    }
    if (static_cast<uint32_t>(millis() - lastCommandMs) >= Motion::TIMEOUT_MS)
      motion.timeout();
    motion.tick(millis());
    LeftWheel.writeMicroseconds(motion.leftPulseUs());
    RightWheel.writeMicroseconds(motion.rightPulseUs());
    uint8_t face = motion.face();
    if (face != lastFace) {
      xQueueOverwrite(queueHC, &face);
      lastFace = face;
    }
#if ROBOT_TRACE
    uint32_t nowMs = millis();
    if (static_cast<uint32_t>(nowMs - loggedMs) >= 100) {
      loggedMs = nowMs;
      char line[128];
      int length = snprintf(line, sizeof(line), "CTRL t=%lu seq=%lu rx=%lu apply=%lu cmd=%02X L=%u R=%u usL=%u usR=%u\n",
          (unsigned long)nowMs, (unsigned long)applied.sequence,
          (unsigned long)applied.receivedMs, (unsigned long)appliedMs,
          (unsigned)applied.val, (unsigned)motion.curL, (unsigned)motion.curR,
          (unsigned)motion.leftPulseUs(), (unsigned)motion.rightPulseUs());
      if (length > 0 && length < (int)sizeof(line) && Serial.availableForWrite() >= length)
        Serial.write((const uint8_t*)line, length); // Se não couber, descarta o log.
    }
#endif
    vTaskDelay(period); // Sem ciclos de recuperação acumulados após um atraso.
  }
}

void HeadController(void* pvParameters)
{
  uint8_t face;
  for (;;) {
    if (xQueueReceive(queueHC, &face, portMAX_DELAY) != pdPASS) continue;
    Eye eyes = Eye::neutral;
    Mouth mouth = Mouth::neutral;
    switch (face) {
      case Motion::FORWARD: mouth = Mouth::happy; break;
      case Motion::BACKWARD: eyes = Eye::up; mouth = Mouth::opened; break;
      case Motion::LEFT: head->Update((4 << 3) | 1); continue; // referência 2
      case Motion::RIGHT: head->Update((3 << 3) | 1); continue; // referência 1
      default: break;
    }
    head->Update((static_cast<byte>(eyes) + 1) << 3 | 2);
    head->Update((static_cast<byte>(mouth) + 1) << 3 | 5);
  }
}

// Demo Task
// This task change head parts periodically
// ----------------------------------------------------------------------------
void Demo( void* pvParameters )
{
  byte op, cmd1, cmd2;
  char *pcTaskName;
  TickType_t xLastWakeTime;
  
  
  pcTaskName = ( char * ) pvParameters;
  xLastWakeTime = xTaskGetTickCount();

  op = 0;
  for(;;)
  {                                
    switch(op++)
    {
      case 0:
        cmd1 = B00001010;  //(1 << 3) | 2;      
        cmd2 = B00001101; // happy           
        break;
      case 1:
        cmd1 = B00110010; //(6 << 3) | 2; 
        cmd2 = B00010101; // sad  
        break;
      case 2:
        cmd1 = B01001010; //(9 << 3) | 2;
        cmd2 = B00011101; // very happy          
        break;
      case 3:
        cmd1 = B00100010; //(4 << 3) | 2;
        cmd2 = B00101101; // neutral          
        break;        
    }          
    
    if (op > 3)
      op = 0;
  
    // eyes
    head->Update(cmd1);    

    // mouth
    //cmd = B00100101; // (5 << 3) | 5;
    head->Update(cmd2);
  
    vTaskDelayUntil(&xLastWakeTime, pdMS_TO_TICKS( 100 ));  
  }
}
