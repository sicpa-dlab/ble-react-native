import * as React from "react"
import { useEffect } from "react"
import { StyleSheet, View, Text, Button, NativeEventEmitter, NativeModules } from "react-native"

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

export default function App() {

  return (
    <View style={styles.container}>
      <Text>Please see logs for demo run results.</Text>
    </View>
  )
}
