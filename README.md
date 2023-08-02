# Bluetooth Low Energy React Native

A React Native library for connecting two phones via Bluetooth Low Energy and exchanging messages. Based on Nordic's
library for Android and CoreBluetooth for iOS.

## IMPORTANT

Even though this library does not restrict the format of the messages that can be sent, it cannot be used as is for any
application since it contains some specifics related to SICPA's internal products. See [Nuances](#Nuances) for more info.

## Publishing new version

Manually bump package version in `package.json` file.
The package will be published automatically from `main` branch by GitHub Actions.

## Features

- Central (client) mode
  - Find a peripheral with a specified filter
  - Connect to a peripheral (one at a time)
  - Send messages to the peripheral (supports long write)
  - Receive messages from the peripheral
- Peripheral (server) mode
  - Advertise a service with a writable characteristic with a specified tag
  - Send messages to a connected central (supports long write)
  - Receive messages from the central
- Get notified when the following events happen:
  - Message received
  - Started message receive
  - Connecting to server
  - Connected to server
  - Disconnecting from server
  - Disconnected from server
  - Client connected
  - Client disconnected
  - Sending message
  - Message sent

## Usage

For Android:

- The library does not manage permissions, you need to request them yourself.
- The library does not manage bluetooth state, you need to ensure that it's enabled.

For iOS:

- The library will ask for Bluetooth permission one time, at the time of first interaction. If the user denies the permission, you need to request it yourself.
- The library does not manage bluetooth state, you need to ensure that it's enabled.
- Make sure you provide [NSBluetoothAlwaysUsageDescription](https://developer.apple.com/documentation/bundleresources/information_property_list/nsbluetoothalwaysusagedescription) in your `Info.plist` for iOS 13 and later, otherwise [NSBluetoothPeripheralUsageDescription](https://developer.apple.com/documentation/bundleresources/information_property_list/nsbluetoothperipheralusagedescription).

Example establishing a connection between two devices and sending some messages:

```typescript
// phone 1
await BLE.start() // for iOS see Nuances section
await BLE.advertise("1234a")
BLE.addConnectedToServerListener(async () => {
  await BLE.sendMessage("Test message from the server")
})
BLE.addBleMessageListener((message) => {
  console.log(`Message received from the client ${message}`)
})
BLE.addClientDisconnectedListener(() => {
  BLE.removeAllListeners()
  BLE.stop()
})
```

```typescript
// phone 2
import { BLE } from "./ble"

await BLE.start() // for iOS see Nuances section
const peripheralId = await BLE.scan("1234a")
if (peripheral) {
  BLE.addBleMessageListener((message) => {
    console.log(`Message received from the server ${message}`)
  })
  BLE.addDisconnectedFromServerListener(() => {
    BLE.removeAllListeners()
    BLE.stop()
  })
  await BLE.connect(peripheralId)
  await BLE.sendMessage("Test message from the client")
}
```

## Demo

### Running

#### 1. Launching Metro bundler

1. Go to `demo`
2. Execute `yarn install`
3. Execute `yarn start`

#### 2. Launching on Android

1. Go to `demo`
2. Execute `yarn android`

#### 3. Launching on iOS

**NOTE FOR M1 USERS: For all `pod` and ios-related `yarn` commands use `arch -x86_64` before the command. For example `arch -x86_64 pod install` instead of `pod install`.**

1. Set a provisioning profile. The easiest way to do this is through Xcode.
2. Go to `demo/ios`
3. Execute `pod install`
4. Go to `demo`
5. Execute `yarn ios --device` or launch the app through Xcode

### Demo usage

Demo allows you to connect two devices and exchange message. There are input field between the buttons, use them
to enter info to be used by the lib or read the info needed on the other device (for example, pressing the Advertise
button will output the advertisement tag which you'll need to enter into the same field on the other phone).

For events and to verify that the messages are actually delivered, check the logs.

## Nuances

### BLE.start() on ios

Bluetooth adapter is initialized lazily on iOS, so the first time you access it, it takes some time (milliseconds scale)
to initialize. Executing some bluetooth action (e.g. advertise) immediately after calling `start()` might result in an
`incorrect bluetooth state` error. This can be fixed by making the `start()` function async and only resolving the promise
when the adapter initializes.

### Hardcoded Service and Characteristic UUIDs

This library was intended to be used to cover one very specific use-case, so the BLE's Service and Characteristic UUIDs
are hardcoded into the library. This can be easily changed later if the need for broader usage of the library arises.

### One-to-one connection

In theory, BLE protocol and some devices allow to connect to multiple devices at the same time. This library does not
this and can only establish connections between two devices.

### "Ready" message

This depends on the peculiarity of the other library that we depend on: Nordic's BLE library. The Peripheral needs
to discover Central's Services before being able to interact with it. This means that Android BLE server is ready to send
and receive messages slightly after iOS BLE client can, which might result in lost messages.
To fix that, Android BLE server sends "ready" message as soon as it's ready. Clients will resolve `connect` Promise only
after receiving "ready" messages. iOS BLE server does not need this to work, but sends the "ready" message for compatibility.

The problem described above seems to only happen between Android BLE server and iOS BLE client. It does not happen for any
other cases (Android<->Android, Android client <-> iOS server, iOS<->iOS)

### Client connected/disconnected events on iOS

iOS BLE server native API does not notify us when a client has connected or disconnected. Due to the specifics of out use-case
we can assume that every client must subscribe to our characteristic notifications, which iOS will let us know about. Therefore,
we can assume that the client has connected to our server if it has subscribed to the characteristic notifications and disconnected
if the client has unsubscribed.

### MTU

MTU is a challenging topic in the world of smartphone BLE: different manufacturers implement this functionality differently
making it unreliable. Because of this we hardcode the MTU, it's 185 - iOS's default MTU.

### Long write

To support long write, manual message splitter and message merger were implemented. Messages are split into MTU sized chunks
automatically and sent separately. The terminal character is `\0`. The receiving end then can buffer all the messages until
it receives the terminal character.

### Hardcoded manufacturer id

To filter the peers while scanning, we put an arbitrary string, bleId, into the advert. We found that the most reliable way
to include this data into adverts is to put into Manufacturer Data. The first two bytes of Manufacturer Data, as per BLE protocol,
is Manufacturer ID. Manufacturer ID needs to be registered to be recognized by third parties. We don't need that
for now so we use the general Manufacturer ID, reserved for testing - 0xFFFF.
