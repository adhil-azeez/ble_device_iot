/*
    Video: https://www.youtube.com/watch?v=oCMOYS71NIU
    Based on Neil Kolban example for IDF: https://github.com/nkolban/esp32-snippets/blob/master/cpp_utils/tests/BLE%20Tests/SampleNotify.cpp
    Ported to Arduino ESP32 by Evandro Copercini
    updated by chegewara

   Create a BLE server that, once we receive a connection, will send periodic notifications.
   The service advertises itself as: 4fafc201-1fb5-459e-8fcc-c5c9c331914b
   And has a characteristic of: beb5483e-36e1-4688-b7f5-ea07361b26a8

   The design of creating the BLE server is:
   1. Create a BLE Server
   2. Create a BLE Service
   3. Create a BLE Characteristic on the Service
   4. Create a BLE Descriptor on the characteristic
   5. Start the service.
   6. Start advertising.

   A connect hander associated with the server starts a background task that performs notification
   every couple of seconds.
*/
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include "BLEDescriptor.h"

#include <Wire.h>
#include "MAX30100_PulseOximeter.h"

#include <OneWire.h>
#include <DallasTemperature.h>


#define REPORTING_PERIOD_MS     1000
#define REPORTING_TMP_PERIOD_MS     1500
#define BLE_STACK_DELAY     1000

// PulseOximeter is the higher level interface to the sensor
// it offers:
//  * beat detection reporting
//  * heart rate calculation
//  * SpO2 (oxidation level) calculation
PulseOximeter pox;

uint32_t tsLastReport = 0;
uint32_t tsLastTmpReport = 0;
uint32_t tsLastBLEStack = 0;


BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
BLECharacteristic* pCharTemp = NULL;
BLECharacteristic* pCharSpo = NULL;

bool deviceConnected = false;
bool oldDeviceConnected = false;


#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHAR_HEART_BEAT_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"
#define CHAR_TEMP_UUID        "5c6ea9f3-b9a5-4ceb-a2d7-ffcbbeb29abb"
#define CHAR_SPO_UUID         "f0b6957d-1d77-4ec8-9755-6c08090fe3a6"


// GPIO where the DS18B20 is connected to
const int oneWireBus = 4;     

// Setup a oneWire instance to communicate with any OneWire devices
OneWire oneWire(oneWireBus);

// Pass our oneWire reference to Dallas Temperature sensor 
DallasTemperature sensors(&oneWire);


class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    }
};


// Callback (registered below) fired when a pulse is detected
void onBeatDetected()
{
    Serial.println("Beat!");
    
//   loopPulse();
}

void initialisePulse(){
  // Initialize the PulseOximeter instance
    // Failures are generally due to an improper I2C wiring, missing power supply
    // or wrong target chip
    if (!pox.begin()) {
        Serial.println("FAILED");
        for(;;);
    } else {
        Serial.println("SUCCESS");
    }

    // The default current for the IR LED is 50mA and it could be changed
    //   by uncommenting the following line. Check MAX30100_Registers.h for all the
    //   available options.
    // pox.setIRLedCurrent(MAX30100_LED_CURR_7_6MA);

    // Register a callback for the beat detection
    pox.setOnBeatDetectedCallback(onBeatDetected);
}


void setup() {
  Serial.begin(115200);

  // Create the BLE Device
  BLEDevice::init("ESP32");

  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create the BLE Service
  BLEService *pService = pServer->createService(SERVICE_UUID);

  // Create a BLE Characteristic
  pCharacteristic = pService->createCharacteristic(
                      CHAR_HEART_BEAT_UUID,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY |
                      BLECharacteristic::PROPERTY_INDICATE
                    );
  pCharTemp = pService->createCharacteristic(
                      CHAR_TEMP_UUID,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY |
                      BLECharacteristic::PROPERTY_INDICATE
                    );
  pCharSpo = pService->createCharacteristic(
                      CHAR_SPO_UUID,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY |
                      BLECharacteristic::PROPERTY_INDICATE
                    );

  // https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
  // Create a BLE Descriptor
  pCharacteristic->addDescriptor(new BLE2902());
  pCharTemp->addDescriptor(new BLE2902());
  pCharSpo->addDescriptor(new BLE2902());


  // Start the service
  pService->start();

  // Start advertising
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  float d = 0.0;
  pCharacteristic->setValue(d);
  pCharTemp->setValue(d);
  pCharSpo->setValue(d);
  
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(false);
  pAdvertising->setMinPreferred(0x0);  // set value to 0x00 to not advertise this parameter
  BLEDevice::startAdvertising();
  Serial.println("Waiting a client connection to notify...");
  // Start the DS18B20 sensor
  sensors.begin();
  
  initialisePulse();
}

void loopPulse(){




    // Asynchronously dump heart rate and oxidation levels to the serial
    // For both, a value of 0 means "invalid"
    if (millis() - tsLastReport > REPORTING_PERIOD_MS) {
      Serial.println("looping pulse");

       float heartbeat = pox.getHeartRate() ;
       float spo = pox.getSpO2();
        
        
        Serial.print("Heart rate:");
         Serial.print(heartbeat);
        Serial.print("bpm / SpO2:");
        Serial.print(spo);
        Serial.println("%");
        tsLastReport = millis();

Serial.print("Device status");
Serial.println(deviceConnected);
         if(deviceConnected && (millis() - tsLastBLEStack > BLE_STACK_DELAY) ){
           pCharacteristic->setValue(heartbeat);
          pCharacteristic->notify();

          pCharSpo->setValue(spo);
          pCharSpo->notify();
          tsLastBLEStack = millis();
    
         }

       
    }
}

void loopTemp(){

   if (millis() - tsLastTmpReport > REPORTING_TMP_PERIOD_MS){
    sensors.setWaitForConversion(false);
      sensors.requestTemperatures(); 
    
      float temperatureC = sensors.getTempCByIndex(0);

      float temperatureF = sensors.getTempFByIndex(0);
   
      Serial.print(temperatureC);
      Serial.println("ºC");
    if (deviceConnected) {
if((millis() - tsLastBLEStack > BLE_STACK_DELAY)){
   
              pCharTemp->setValue(temperatureC);
            pCharTemp->notify();
           
            tsLastBLEStack = millis();
           
}
   }
tsLastTmpReport = millis();
   
   }
   
   
}

void loop() {
  uint32_t initMillis = millis();
  
          // Make sure to call update as fast as possible
    pox.update();
    

    
        loopPulse();
    // notify changed value
//    if (deviceConnected) {
pox.update();
//
       loopTemp();
        

pox.update();
//        delay(3); // bluetooth stack will go into congestion, if too many packets are sent, in 6 hours test i was able to go as low as 3ms
//    }
    // disconnecting
    if (!deviceConnected && oldDeviceConnected) {
        delay(500); // give the bluetooth stack the chance to get things ready
        pServer->startAdvertising(); // restart advertising
        Serial.println("start advertising");
        oldDeviceConnected = deviceConnected;
        
    }
    // connecting
    if (deviceConnected && !oldDeviceConnected) {
        Serial.println("Device connecting");
        // do stuff here on connecting
        oldDeviceConnected = deviceConnected;
          initialisePulse();
        
    }

}
