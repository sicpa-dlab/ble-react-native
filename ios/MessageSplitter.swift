//
//  MessageSplitter.swift
//  Ble
//
//  Created by AndrewNadraliev on 28.11.2022.
//  Copyright © 2022 Facebook. All rights reserved.
//

import Foundation

func splitMessage(data: Data, maxChunkLength: Int) -> [Data] {
    if data.isEmpty {
        return []
    }
    
    var result: [Data] = []
    
    var index = 0
    
    repeat {
        let bottomBar = index * maxChunkLength
        let upperBar = min(data.count, (index + 1) * maxChunkLength)
        
        result.append(data.subdata(in: bottomBar..<upperBar))
        
        index += 1
    } while index * maxChunkLength < data.count
    
    let lastChunk = result.last
    
    if var lastChunk = lastChunk, lastChunk.count < maxChunkLength {
        // there is enough space for terminal operator
        lastChunk.append(contentsOf: [0])
        result[result.count - 1] = lastChunk
    } else {
        // not enough space in the last chunk, create a new one
        result.append(Data(repeating: 0, count: 1))
    }
    
    return result
}
