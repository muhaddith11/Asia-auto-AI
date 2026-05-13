// Car Controller Arduino Firmware
// HC-05 Bluetooth Module -> Arduino -> Relay Board -> Car Electronics
// Baud rate: 9600

#include <SoftwareSerial.h>

SoftwareSerial bluetooth(10, 11); // RX, TX

// Relay pins (LOW = ON for most relay boards)
#define RELAY_WIN_ALL_OPEN  2
#define RELAY_WIN_ALL_CLOSE 3
#define RELAY_WIN_FL_OPEN   4
#define RELAY_WIN_FL_CLOSE  5
#define RELAY_WIN_FR_OPEN   6
#define RELAY_WIN_FR_CLOSE  7
#define RELAY_SUNROOF_OPEN  8
#define RELAY_SUNROOF_CLOSE 9
#define RELAY_AC            12
#define RELAY_LIGHTS        13

#define RELAY_PULSE_MS 500  // How long to hold relay before releasing

void setup() {
  Serial.begin(9600);
  bluetooth.begin(9600);

  // All relays off initially
  for (int i = 2; i <= 13; i++) {
    pinMode(i, OUTPUT);
    digitalWrite(i, HIGH);
  }
  Serial.println("Car Controller Ready");
}

void pulseRelay(int pin, int duration = RELAY_PULSE_MS) {
  digitalWrite(pin, LOW);
  delay(duration);
  digitalWrite(pin, HIGH);
}

void loop() {
  if (bluetooth.available()) {
    String cmd = bluetooth.readStringUntil('\n');
    cmd.trim();
    Serial.print("CMD: ");
    Serial.println(cmd);

    if      (cmd == "WIN_ALL_OPEN")  pulseRelay(RELAY_WIN_ALL_OPEN);
    else if (cmd == "WIN_ALL_CLOSE") pulseRelay(RELAY_WIN_ALL_CLOSE);
    else if (cmd == "WIN_FL_OPEN")   pulseRelay(RELAY_WIN_FL_OPEN);
    else if (cmd == "WIN_FL_CLOSE")  pulseRelay(RELAY_WIN_FL_CLOSE);
    else if (cmd == "WIN_FR_OPEN")   pulseRelay(RELAY_WIN_FR_OPEN);
    else if (cmd == "WIN_FR_CLOSE")  pulseRelay(RELAY_WIN_FR_CLOSE);
    else if (cmd == "SUNROOF_OPEN")  pulseRelay(RELAY_SUNROOF_OPEN);
    else if (cmd == "SUNROOF_CLOSE") pulseRelay(RELAY_SUNROOF_CLOSE);
    else if (cmd == "AC_ON")         digitalWrite(RELAY_AC, LOW);
    else if (cmd == "AC_OFF")        digitalWrite(RELAY_AC, HIGH);
    else if (cmd == "LIGHTS_ON")     digitalWrite(RELAY_LIGHTS, LOW);
    else if (cmd == "LIGHTS_OFF")    digitalWrite(RELAY_LIGHTS, HIGH);
    else if (cmd == "HORN_BEEP")     pulseRelay(RELAY_LIGHTS, 200);

    // Music commands go to Android media session, no relay needed
    bluetooth.println("OK:" + cmd);
  }
}
