import { BLE } from "@sicpa-dlab/ble-react-native"
import * as React from "react"
import { useCallback, useEffect, useState } from "react"
import {
  StyleSheet,
  View,
  Text,
  Button,
  PermissionsAndroid,
  TextInput,
} from "react-native"

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
})

const requestPermissions = async () => {
  try {
    const granted = await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT, {
      title: "Cool Photo App Camera Permission",
      message: "Cool Photo App needs access to your camera " + "so you can take awesome pictures.",
      buttonNeutral: "Ask Me Later",
      buttonNegative: "Cancel",
      buttonPositive: "OK",
    })

    await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN, {
      title: "Cool Photo App Camera Permission",
      message: "Cool Photo App needs access to your camera " + "so you can take awesome pictures.",
      buttonNeutral: "Ask Me Later",
      buttonNegative: "Cancel",
      buttonPositive: "OK",
    })

    await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.BLUETOOTH_ADVERTISE, {
      title: "Cool Photo App Camera Permission",
      message: "Cool Photo App needs access to your camera " + "so you can take awesome pictures.",
      buttonNeutral: "Ask Me Later",
      buttonNegative: "Cancel",
      buttonPositive: "OK",
    })

    await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION, {
      title: "Cool Photo App Camera Permission",
      message: "Cool Photo App needs access to your camera " + "so you can take awesome pictures.",
      buttonNeutral: "Ask Me Later",
      buttonNegative: "Cancel",
      buttonPositive: "OK",
    })

    await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION, {
      title: "Cool Photo App Camera Permission",
      message: "Cool Photo App needs access to your camera " + "so you can take awesome pictures.",
      buttonNeutral: "Ask Me Later",
      buttonNegative: "Cancel",
      buttonPositive: "OK",
    })
    if (granted === PermissionsAndroid.RESULTS.GRANTED) {
      console.log("You can use the camera")
    } else {
      console.log("Camera permission denied")
    }
  } catch (err) {
    console.warn(err)
  }
}

export default function App() {
  const [bleId, setBleId] = useState<string>("")
  const [address, setAddress] = useState<string>("")
  const [message, setMessage] = useState<string>("")

  useEffect(() => {
    BLE.addBleMessageListener((payload) => {
      console.warn(`MessageReceived event received: ${payload}`)
    })
    BLE.addStartedMessageReceiveListener(() => {
      console.warn("StartedMessageReceive event received")
    })
    BLE.addConnectingToServerListener(() => {
      console.warn("ConnectingToServer event received")
    })
    BLE.addConnectedToServerListener(() => {
      console.warn("ConnectedToServer event received")
    })
    BLE.addDisconnectingFromServerListener(() => {
      console.warn("DisconnectingFromServer event received")
    })
    BLE.addDisconnectedFromServerListener(() => {
      console.warn("DisconnectedFromServer event received")
    })
    BLE.addClientConnectedListener(() => {
      console.warn("ClientConnected event received")
    })
    BLE.addClientDisconnectedListener(() => {
      console.warn("ClientDisconnected event received")
    })
    BLE.addSendingMessageListener(() => {
      console.warn("SendingMessage event received")
    })
    BLE.addMessageSentListener(() => {
      console.warn("MessageSent event received")
    })

    console.warn("Added listeners")

    return () => {
      console.warn("Removed listeners")
      BLE.removeBleMessageListeners()
      BLE.removeStartedMessageReceiveListeners()
      BLE.removeConnectingToServerListeners()
      BLE.removeConnectedToServerListeners()
      BLE.removeDisconnectingFromServerListeners()
      BLE.removeDisconnectedFromServerListeners()
      BLE.removeClientConnectedListeners()
      BLE.removeClientDisconnectedListeners()
      BLE.removeSendingMessageListeners()
      BLE.removeMessageSentListeners()
    }
  }, [])

  const advertise = useCallback(async () => {
    await requestPermissions()
    console.log("Got permissions")
    const bleId = await BLE.generateBleId()
    console.log(`Got ble id: ${bleId}`)
    setBleId(bleId)
    await BLE.advertise(bleId)
    console.log("Started advertise")
  }, [setBleId])

  const scan = useCallback(async () => {
    await requestPermissions()
    console.log(`BLEid is ${bleId}`)
    const result = await BLE.scan(bleId, true)
    console.log(`Found ble devices: ${result}`)
    setAddress(result)
  }, [bleId, setAddress])

  const connect = useCallback(async () => {
    await requestPermissions()
    await BLE.connect(address)
  }, [address])

  const sendMessage = useCallback(async () => {
    await requestPermissions()
    await BLE.sendMessage(message)
  }, [message])

  const disconnect = useCallback(async () => {
    await BLE.finish()
  }, [])

  return (
    <View style={styles.container}>
      <Button title={"Advertise"} onPress={advertise} />
      <TextInput value={bleId} onChangeText={setBleId} />
      <Button title={"Scan"} onPress={scan} />
      <TextInput value={address} onChangeText={setAddress} />
      <Button title={"Connect"} onPress={connect} />
      <TextInput value={message} onChangeText={setMessage} />
      <Button title={"Send"} onPress={sendMessage} />
      <Text>Please see logs for demo run results.</Text>
      <Button title={"Disconnect"} onPress={disconnect} />
    </View>
  )
}
