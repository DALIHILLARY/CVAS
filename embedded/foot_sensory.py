from machine import Pin
import utime


trigger = Pin(15, Pin.OUT)
echo = Pin(14, Pin.IN)
bazzer = Pin(4, Pin.OUT)
led = Pin(25, Pin.OUT)

def ultra():
   echo.low()
   utime.sleep_us(2)
   trigger.high()
   utime.sleep_us(10)
   trigger.low()
   
   low_counter = 0
   while echo.value() == 0:
       led.high()
       signaloff = utime.ticks_us()
       if low_counter == 500:
           break
       low_counter += 1
       utime.sleep_us(1)
       
   while echo.value() == 1:
       signalon = utime.ticks_us()
       
   try:
       timepassed = signalon - signaloff
       distance = (timepassed * 0.0343) / 2
       if distance < 60:
           bazzer.high()
           led.low()
   except NameError:
       print("Sonic missed a step")
bazzCounter = 0
while True:
   ultra()
   utime.sleep(1)
   if bazzer.value() == 1:
       bazzCounter += 1
       if bazzCounter == 2:
           bazzer.low()
           bazzCounter = 0
   print(bazzCounter)



