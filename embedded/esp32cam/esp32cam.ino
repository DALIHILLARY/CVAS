

#include <WebSocketsServer.h>
#include <WiFi.h>
#include "camera_wrap.h"


const char* ssid = "BSE22-6 YR PROJECT";   
const char* password = "123project";

//holds the current upload
int cameraInitState = -1;
uint8_t* jpgBuff = new uint8_t[68123];
size_t   jpgLength = 0;
uint8_t camNo=0;
bool clientConnected = false;

int control_distance; //threshold distance

//sonic pins
const int trigger = 12;
const int echo  = 13;

//bazzer pin
const int bazzer= 15;


WebSocketsServer webSocket = WebSocketsServer(86);
String html_home;

void webSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {

  switch(type) {
      case WStype_DISCONNECTED:
          Serial.printf("[%u] Disconnected!\n", num);
          camNo = num;
          clientConnected = false;
          break;
      case WStype_CONNECTED:
          Serial.printf("[%u] Connected!\n", num);
          clientConnected = true;
          break;
      case WStype_TEXT:
      case WStype_BIN:
      case WStype_ERROR:
      case WStype_FRAGMENT_TEXT_START:
      case WStype_FRAGMENT_BIN_START:
      case WStype_FRAGMENT:
      case WStype_FRAGMENT_FIN:
          Serial.println(type);
          break;
  }
}

std::vector<String> splitString(String data, String delimiter){
    std::vector<String> ret;
    // initialize first part (string, delimiter)
    char* ptr = strtok((char*)data.c_str(), delimiter.c_str());

    while(ptr != NULL) {
        ret.push_back(String(ptr));
        // create next part
        ptr = strtok(NULL, delimiter.c_str());
    }
    return ret;
}

void setup(void) {

  Serial.begin(9600);
  #ifdef DEBUG
  Serial.setDebugOutput(true);
  #endif

  //set up the bazzer and snoic pins
  pinMode(trigger, OUTPUT);
  pinMode(echo, INPUT);
  pinMode(bazzer, OUTPUT);

  cameraInitState = initCamera();

  Serial.printf("camera init state %d\n", cameraInitState);

  if(cameraInitState != 0){
    return;
  }

  //ACCESS POINT SETUP
  WiFi.softAP(ssid, password);
  IPAddress IP = WiFi.softAPIP();
  Serial.print("AP IP address: ");
  Serial.println(IP);

  webSocket.begin();
  webSocket.onEvent(webSocketEvent);

  for(;;) {
      //SET TRESHOLD DISTANCE
      digitalWrite(trigger, LOW);
      delayMicroseconds(2);
      digitalWrite(bazzer, LOW);
      delayMicroseconds(2);
      digitalWrite(trigger, HIGH);
      delayMicroseconds(10);
      digitalWrite(trigger, LOW);
      
      int duration = pulseIn(echo, HIGH);
      Serial.print(duration);
      Serial.print("\n");
      if(duration > 0) {
          control_distance = duration * 0.0343 / 2;
          control_distance += 20; //add 20 cm for safe depth
          break;
      }
    
  }
  
}

int bazzerCounter = 0;
int sonicCounter = 0;

void loop(void) {
  webSocket.loop();
  if(clientConnected == true){
    grabImage(jpgLength, jpgBuff);
    webSocket.sendBIN(camNo, jpgBuff, jpgLength);
  }
  


  if(digitalRead(bazzer) == HIGH) {
      bazzerCounter += 1;
      if( bazzerCounter == 2){
        Serial.println("Bazzer Off");
        digitalWrite(bazzer, LOW);
          delayMicroseconds(2);
        bazzerCounter = 0;
      }
  }else{
      for(;;) {
          //sonic setup
          digitalWrite(trigger, LOW);
          delayMicroseconds(2);
          digitalWrite(trigger, HIGH);
          delayMicroseconds(10);
          digitalWrite(trigger, LOW);
          
          int duration = pulseIn(echo, HIGH);

          if(duration > 0){
            int distance = duration * 0.0343 / 2;
            if(distance > control_distance ) {
              digitalWrite(bazzer, HIGH);
              delayMicroseconds(2);
            }
            break;
          }
      }

  }
  
}
