//
//  Utils.swift
//  Ble
//
//  Created by AndrewNadraliev on 30.10.2022.
//  Copyright © 2022 Facebook. All rights reserved.
//

import Foundation

func log(tag: String, message: String, error: Error? = nil) {
    if let error = error {
        NSLog("%s: %s \n %s", tag, message, error.localizedDescription)
    } else {
        NSLog("%@: %@", tag, message)
    }
}

@available(iOS 13.0, *)
public extension AsyncStream {
  /// Factory function that creates an AsyncStream and returns a tuple standing for its inputs and outputs.
  /// It easy the usage of an AsyncStream in a imperative code context.
  /// - Parameter bufferingPolicy: A `Continuation.BufferingPolicy` value to
  ///       set the stream's buffering behavior. By default, the stream buffers an
  ///       unlimited number of elements. You can also set the policy to buffer a
  ///       specified number of oldest or newest elements.
  /// - Returns: the tuple (input, output). The input can be yielded with values, the output can be iterated over
  static func pipe(
    bufferingPolicy: AsyncStream<Element>.Continuation.BufferingPolicy = .unbounded
  ) -> (AsyncStream<Element>.Continuation, AsyncStream<Element>) {
    var continuation: AsyncStream<Element>.Continuation!
    let stream = AsyncStream(bufferingPolicy: bufferingPolicy) { continuation = $0 }
    return (continuation, stream)
  }
}
