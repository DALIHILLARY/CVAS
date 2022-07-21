from machine import Pin
import utime
trigger = Pin(3, Pin.OUT)
echo = Pin(2, Pin.IN)
bazzer = Pin(4, Pin.OUT)

initialised = True

if intialised:
    #set threshhold distance
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
    threshold_distance = (timepassed * 0.0343) / 2
    control_distance = threshold_distance+5
    
    initialised = False

def ultra():
    
   trigger.low()
   utime.sleep_us(2)
   trigger.high()
   utime.sleep_us(5)
   trigger.low()
   
   while echo.value() == 0:
       signaloff2 = utime.ticks_us()
   while echo.value() == 1:
       signalon2 = utime.ticks_us()
       
   timepassed2 = signalon2 - signaloff2
   distance2 = (timepassed2 * 0.0343) / 2
  
   if(distance2 > control_distance):
       bazzer.high()
       utime.sleep_us(1500)
       bazzer.low()
   print("The pit ahead is above 5cm deep")
   
while True:
   ultra()
   utime.sleep(1)