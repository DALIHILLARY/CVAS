from machine import Pin
import utime


trigger = Pin(15, Pin.OUT)
echo = Pin(14, Pin.IN)
bazzer = Pin(4, Pin.OUT)
led = Pin(25, Pin.OUT)


#set threshhold distance
while True:
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
       threshold_distance = (timepassed * 0.0343) / 2
       control_distance = threshold_distance + 20
       
       break
    except NameError:
       print("failed to initialize")
    utime.sleep(1)

def ultra():
   echo.low()
   utime.sleep_us(2)
   trigger.high()
   utime.sleep_us(10)
   trigger.low()
   
   low_counter2 = 0
   while echo.value() == 0:
       led.high()
       signaloff2 = utime.ticks_us()
       if low_counter2 == 500:
           break
       low_counter2 += 1
       utime.sleep_us(1)
       
   while echo.value() == 1:
       signalon2 = utime.ticks_us()
       
   try:
       timepassed2 = signalon2 - signaloff2
       distance2 = (timepassed2 * 0.0343) / 2
       if distance2 > control_distance:
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
       if bazzCounter == 1:
           bazzer.low()
           bazzCounter = 0
   print(bazzCounter)





