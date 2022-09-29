
//sonic pins
const int trigger = 10;
const int echo  = 11;

//bazzer pin
const int bazzer= 12;


void setup(void) {
  
  Serial.begin(9600);
  #ifdef DEBUG
  Serial.setDebugOutput(true);
  #endif
  
  //set up the bazzer and sonic pins
  pinMode(trigger, OUTPUT);
  pinMode(echo, INPUT);
  pinMode(bazzer, OUTPUT);

}

int bazzerCounter = 0;
int sonicCounter = 0;

void loop(void) {
  
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
            if(distance < 100 ) {
              digitalWrite(bazzer, HIGH);
              delayMicroseconds(2);
            }
            break;
          }
      }

  }
}
