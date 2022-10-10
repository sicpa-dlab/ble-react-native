// eslint-disable-next-line import/no-extraneous-dependencies
import { NativeEventEmitter, NativeModules } from "react-native"

const { BLEModule } = NativeModules

export class BLE {
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
}
