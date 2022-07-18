from machine import Pin
import utime
trigger = Pin(3, Pin.OUT)
echo = Pin(2, Pin.IN)
bazzer = Pin(4, Pin.OUT)
def ultra():
   trigger.low()
   utime.sleep_us(2)
   trigger.high()
   utime.sleep_us(5)
   trigger.low()
   while echo.value() == 0:
       signaloff = utime.ticks_us()
   while echo.value() == 1:
       signalon = utime.ticks_us()
   timepassed = signalon - signaloff
   distance = (timepassed * 0.0343) / 2
   if(distance < 15):
       bazzer.high()
       utime.sleep_us(1500)
       bazzer.low()
   print("The distance from object is ",distance,"cm")
while True:
   ultra()
   utime.sleep(1)