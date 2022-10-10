import {Agent, InboundTransport, OutboundPackage, OutboundTransport} from "@sicpa-dlab/aries-framework-core";
import {BLE} from "./ble";

export class OutboundBleTransport implements OutboundTransport {
  supportedSchemes = ["androidnearby"]

  public async sendMessage(outboundPackage: OutboundPackage): Promise<void> {
    await BLE.sendMessage(JSON.stringify(outboundPackage.payload))
  }

  start(agent: Agent): Promise<void> {
    return Promise.resolve(undefined);
  }

  stop(): Promise<void> {
    return Promise.resolve(undefined);
  }

}

export class InboundBleTransport implements InboundTransport {

  public async start(agent: Agent): Promise<void> {
    BLE.addBleMessageListener((payload) => {
      agent.receiveMessage(JSON.parse(payload))
    })
  }

  public async stop(): Promise<void> {
    BLE.removeBleMessageListeners()
  }

}