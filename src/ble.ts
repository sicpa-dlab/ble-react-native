// eslint-disable-next-line import/no-extraneous-dependencies
import { EmitterSubscription, NativeEventEmitter, NativeModules } from "react-native"

const { BLEModule } = NativeModules
const { PAYLOAD_STRING_KEY } = BLEModule.getConstants()

export enum BleEvent {
  MessageReceived = "message-received",
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

  public static addBleMessageListener(listener: (payload: string) => void): EmitterSubscription {
    return this.getNativeEventEmitter().addListener(BleEvent.MessageReceived, (event) => {
      listener(event[PAYLOAD_STRING_KEY])
    })
  }

  public static removeBleMessageListeners(): void {
    this.getNativeEventEmitter().removeAllListeners(BleEvent.MessageReceived)
  }
}
