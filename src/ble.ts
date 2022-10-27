// eslint-disable-next-line import/no-extraneous-dependencies
import { EmitterSubscription, NativeEventEmitter, NativeModules } from "react-native"

const { BLEModule } = NativeModules
const { PAYLOAD_STRING_KEY } = BLEModule.getConstants()

export enum BleEvent {
  MessageReceived = "ble-message-received",
  StartedMessageReceive = "ble-started-message-receive",
  ConnectingToServer = "ble-connecting-to-server",
  ConnectedToServer = "ble-connected-to-server",
  DisconnectingFromServer = "ble-disconnecting-from-server",
  DisconnectedFromServer = "ble-disconnected-from-server",
  ClientConnected = "ble-device-connected",
  ClientDisconnected = "ble-device-disconnected",
  SendingMessage = "ble-sending-message",
  MessageSent = "ble-message-sent",
}

export class BLE {
  private static nativeEventEmitter: NativeEventEmitter

  private static getNativeEventEmitter(): NativeEventEmitter {
    return (
      this.nativeEventEmitter ??
      (() => {
        this.nativeEventEmitter = new NativeEventEmitter(BLEModule)
        return this.nativeEventEmitter
      })()
    )
  }

  public static async generateBleId(): Promise<string> {
    return await BLEModule.generateBleId()
  }

  public static async advertise(bleId: string) {
    await BLEModule.advertise(bleId)
  }

  public static async stopAdvertise() {
    await BLEModule.stopAdvertise()
  }

  public static async scan(filterBleId: string, stopIfFound: boolean): Promise<string> {
    return await BLEModule.scan(filterBleId, stopIfFound)
  }

  public static async stopScan() {
    await BLEModule.stopScan()
  }

  public static async connect(address: string) {
    await BLEModule.connectToPeripheral(address)
  }

  public static async sendMessage(message: string) {
    await BLEModule.sendMessage(message)
  }

  public static async disconnect() {
    await BLEModule.disconnect()
  }

  public static async finish() {
    await BLEModule.finish()
  }

  public static addBleMessageListener(listener: (payload: string) => void): EmitterSubscription {
    return this.getNativeEventEmitter().addListener(BleEvent.MessageReceived, (event) => {
      listener(event[PAYLOAD_STRING_KEY])
    })
  }

  public static removeBleMessageListeners(): void {
    this.getNativeEventEmitter().removeAllListeners(BleEvent.MessageReceived)
  }

  public static addStartedMessageReceiveListener(listener: () => void): EmitterSubscription {
    return this.getNativeEventEmitter().addListener(BleEvent.StartedMessageReceive, (event) => {
      listener()
    })
  }

  public static removeStartedMessageReceiveListeners(): void {
    this.getNativeEventEmitter().removeAllListeners(BleEvent.StartedMessageReceive)
  }

  public static addConnectingToServerListener(listener: () => void): EmitterSubscription {
    return this.getNativeEventEmitter().addListener(BleEvent.ConnectingToServer, (event) => {
      listener()
    })
  }

  public static removeConnectingToServerListeners(): void {
    this.getNativeEventEmitter().removeAllListeners(BleEvent.ConnectingToServer)
  }

  public static addConnectedToServerListener(listener: () => void): EmitterSubscription {
    return this.getNativeEventEmitter().addListener(BleEvent.ConnectedToServer, (event) => {
      listener()
    })
  }

  public static removeConnectedToServerListeners(): void {
    this.getNativeEventEmitter().removeAllListeners(BleEvent.ConnectedToServer)
  }

  public static addDisconnectingFromServerListener(listener: () => void): EmitterSubscription {
    return this.getNativeEventEmitter().addListener(BleEvent.DisconnectingFromServer, (event) => {
      listener()
    })
  }

  public static removeDisconnectingFromServerListeners(): void {
    this.getNativeEventEmitter().removeAllListeners(BleEvent.DisconnectingFromServer)
  }

  public static addDisconnectedFromServerListener(listener: () => void): EmitterSubscription {
    return this.getNativeEventEmitter().addListener(BleEvent.DisconnectedFromServer, (event) => {
      listener()
    })
  }

  public static removeDisconnectedFromServerListeners(): void {
    this.getNativeEventEmitter().removeAllListeners(BleEvent.DisconnectedFromServer)
  }

  public static addClientConnectedListener(listener: () => void): EmitterSubscription {
    return this.getNativeEventEmitter().addListener(BleEvent.ClientConnected, (event) => {
      listener()
    })
  }

  public static removeClientConnectedListeners(): void {
    this.getNativeEventEmitter().removeAllListeners(BleEvent.ClientConnected)
  }

  public static addClientDisconnectedListener(listener: () => void): EmitterSubscription {
    return this.getNativeEventEmitter().addListener(BleEvent.ClientDisconnected, (event) => {
      listener()
    })
  }

  public static removeClientDisconnectedListeners(): void {
    this.getNativeEventEmitter().removeAllListeners(BleEvent.ClientDisconnected)
  }

  public static addSendingMessageListener(listener: () => void): EmitterSubscription {
    return this.getNativeEventEmitter().addListener(BleEvent.SendingMessage, (event) => {
      listener()
    })
  }

  public static removeSendingMessageListeners(): void {
    this.getNativeEventEmitter().removeAllListeners(BleEvent.SendingMessage)
  }

  public static addMessageSentListener(listener: () => void): EmitterSubscription {
    return this.getNativeEventEmitter().addListener(BleEvent.MessageSent, (event) => {
      listener()
    })
  }

  public static removeMessageSentListeners(): void {
    this.getNativeEventEmitter().removeAllListeners(BleEvent.MessageSent)
  }

  public static removeAllListeners(): void {
    Object.values(BleEvent).forEach((type) => {
      this.getNativeEventEmitter().removeAllListeners(type)
    })
  }
}
