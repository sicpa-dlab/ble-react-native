import * as React from "react"
import {useCallback, useEffect, useState} from "react"
import {
  StyleSheet,
  View,
  Text,
  Button,
  NativeEventEmitter,
  NativeModules,
  PermissionsAndroid,
  TextInput
} from "react-native"
import {BLE} from "@sicpa-dlab/ble-react-native";

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
    const granted = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
      {
        title: "Cool Photo App Camera Permission",
        message:
          "Cool Photo App needs access to your camera " +
          "so you can take awesome pictures.",
        buttonNeutral: "Ask Me Later",
        buttonNegative: "Cancel",
        buttonPositive: "OK"
      }
    )

    await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
      {
        title: "Cool Photo App Camera Permission",
        message:
          "Cool Photo App needs access to your camera " +
          "so you can take awesome pictures.",
        buttonNeutral: "Ask Me Later",
        buttonNegative: "Cancel",
        buttonPositive: "OK"
      }
    )

    await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_ADVERTISE,
      {
        title: "Cool Photo App Camera Permission",
        message:
          "Cool Photo App needs access to your camera " +
          "so you can take awesome pictures.",
        buttonNeutral: "Ask Me Later",
        buttonNegative: "Cancel",
        buttonPositive: "OK"
      }
    )
    if (granted === PermissionsAndroid.RESULTS.GRANTED) {
      console.log("You can use the camera");
    } else {
      console.log("Camera permission denied");
    }
  } catch (err) {
    console.warn(err);
  }
};

export default function App() {

  const [bleId, setBleId] = useState<string>("")

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
  }, [bleId])

  return (
    <View style={styles.container}>
      <Button title={"Advertise"} onPress={advertise} />
      <TextInput value={bleId} onChangeText={setBleId} />
      <Button title={"Scan"} onPress={scan} />
      <Text>Please see logs for demo run results.</Text>
    </View>
  )
}
