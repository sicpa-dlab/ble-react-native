// eslint-disable-next-line import/no-extraneous-dependencies
import {EmitterSubscription, NativeEventEmitter, NativeModules} from "react-native"

const {BLEModule} = NativeModules
const {PAYLOAD_STRING_KEY, BLE_EVENT_NAME} = BLEModule.getConstants()

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

  private static messageReceivedListener?: (payload: string) => void
  private static listeners = new Map<BleEvent, () => void>()
  private static nativeEmitterSubscription?: EmitterSubscription

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

  public static addBleMessageListener(listener: (payload: string) => void): void {
    BLE.messageReceivedListener = listener
  }

  public static removeBleMessageListeners(): void {
    BLE.messageReceivedListener = undefined
  }

  public static addStartedMessageReceiveListener(listener: () => void): void {
    BLE.listeners.set(BleEvent.StartedMessageReceive, listener)
  }

  public static removeStartedMessageReceiveListeners(): void {
    BLE.listeners.delete(BleEvent.StartedMessageReceive)
  }

  public static addConnectingToServerListener(listener: () => void): void {
    BLE.listeners.set(BleEvent.ConnectingToServer, listener)
  }

  public static removeConnectingToServerListeners(): void {
    BLE.listeners.delete(BleEvent.ConnectingToServer)
  }

  public static addConnectedToServerListener(listener: () => void): void {
    BLE.listeners.set(BleEvent.ConnectedToServer, listener)
  }

  public static removeConnectedToServerListeners(): void {
    BLE.listeners.delete(BleEvent.ConnectedToServer)
  }

  public static addDisconnectingFromServerListener(listener: () => void): void {
    BLE.listeners.set(BleEvent.DisconnectingFromServer, listener)
  }

  public static removeDisconnectingFromServerListeners(): void {
    BLE.listeners.delete(BleEvent.DisconnectingFromServer)
  }

  public static addDisconnectedFromServerListener(listener: () => void): void {
    BLE.listeners.set(BleEvent.DisconnectedFromServer, listener)
  }

  public static removeDisconnectedFromServerListeners(): void {
    BLE.listeners.delete(BleEvent.DisconnectedFromServer)
  }

  public static addClientConnectedListener(listener: () => void): void {
    BLE.listeners.set(BleEvent.ClientConnected, listener)
  }

  public static removeClientConnectedListeners(): void {
    BLE.listeners.delete(BleEvent.ClientConnected)
  }

  public static addClientDisconnectedListener(listener: () => void): void {
    BLE.listeners.set(BleEvent.ClientDisconnected, listener)
  }

  public static removeClientDisconnectedListeners(): void {
    BLE.listeners.delete(BleEvent.ClientDisconnected)
  }

  public static addSendingMessageListener(listener: () => void): void {
    BLE.listeners.set(BleEvent.SendingMessage, listener)
  }

  public static removeSendingMessageListeners(): void {
    BLE.listeners.delete(BleEvent.SendingMessage)
  }

  public static addMessageSentListener(listener: () => void): void {
    BLE.listeners.set(BleEvent.MessageSent, listener)
  }

  public static removeMessageSentListeners(): void {
    BLE.listeners.delete(BleEvent.MessageSent)
  }

  static start(): EmitterSubscription {
    return new NativeEventEmitter(BLEModule).addListener(BLE_EVENT_NAME, (event) => {
      console.debug("Received an event")
      const type = event.type
      switch (type) {
        case BleEvent.MessageReceived:
          BLE.messageReceivedListener?.(event.payload)
          break
        default:
          BLE.listeners.get(type)?.()
          break
      }
    })
  }

  static stop(): void{
    BLE.nativeEmitterSubscription?.remove()
  }

}
