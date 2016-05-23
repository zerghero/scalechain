package io.scalechain.blockchain.cli

import java.util

import io.scalechain.blockchain.chain.Blockchain
import io.scalechain.blockchain.net.PeerCommunicator
import io.scalechain.blockchain.proto.{CoinbaseData, Hash, BlockHash, Block}
import io.scalechain.blockchain.script.HashCalculator
import io.scalechain.util.Utils
import io.scalechain.wallet.Wallet

import scala.util.Random

/**
  * Created by kangmo on 3/15/16.
  */
object CoinMiner {
  var theCoinMiner : CoinMiner = null

  def create(minerAccount : String, wallet : Wallet, chain : Blockchain, peerCommunicator: PeerCommunicator) = {
    theCoinMiner = new CoinMiner(minerAccount, wallet, chain, peerCommunicator)
    theCoinMiner.start()
    theCoinMiner
  }

  def get = {
    assert(theCoinMiner != null)
    theCoinMiner
  }

  def isLessThan(hash1 : Hash, hash2 : Hash): Boolean = {
    val value1 = Utils.bytesToBigInteger(hash1.value)
    val value2 = Utils.bytesToBigInteger(hash2.value)

    if ( value1.compareTo( value2 ) < 0 ) {
      true
    } else {
      false
    }
  }
}


class CoinMiner(minerAccount : String, wallet : Wallet, chain : Blockchain, peerCommunicator: PeerCommunicator) {
  // For every 10 seconds, create a new block template for mining a block.
  // This means that transactions received within the time window may not be put into the mined block.
  val MINING_TRIAL_WINDOW_MILLIS = 10000
  val PREMINE_BLOCKS = 5;
  def start() : Unit = {

    val thread = new Thread {
      override def run {
        println("Miner started.")
        // TODO : Need to eliminate this code.
        // Sleep for one minute to wait for each peer to start.
//        Thread.sleep(20000)

        // Step 1 : Set the minder's coin address to receive block mining reward.
        val minerAddress = wallet.getReceivingAddress(minerAccount)

        var nonce : Int = 1

        while(true) { // This thread loops forever.
          nonce += 1
          // Randomly sleep from 100 to 200 milli seconds. On average, sleep 60 seconds.
          // Because current difficulty(max hash : 00F0.. ) is to find a block at the probability 1/256,
          // We will get a block in (100ms * 256 = 25 seconds) ~ (200 ms * 256 = 52 seconds)
//          Thread.sleep(200 + Random.nextInt(200))
          Thread.sleep(10 + Random.nextInt(10))

          //          Thread.sleep(10 + Random.nextInt(10))

          val COINBASE_MESSAGE = CoinbaseData(s"height:${chain.getBestBlockHeight() + 1}, ScaleChain by Kwanho, Chanwoo, Kangmo.")
          // Step 2 : Create the block template
          val blockTemplate = chain.getBlockTemplate(COINBASE_MESSAGE, minerAddress)
          val bestBlockHash = chain.getBestBlockHash()
          if (bestBlockHash.isDefined) {
            // Step 3 : Get block header
            val blockHeader = blockTemplate.getBlockHeader(BlockHash(bestBlockHash.get.value))
            val startTime = System.currentTimeMillis()
            var blockFound = false;

            // Step 3 : Loop until we find a block header hash less than the threshold.
//            do {
              // TODO : BUGBUG : Need to use chain.getDifficulty instead of using a fixed difficulty
              val blockHashThreshold = Hash("00F0000000000000000000000000000000000000000000000000000000000000")

              val newBlockHeader = blockHeader.copy(nonce = nonce)
              val newBlockHash = Hash(HashCalculator.blockHeaderHash(newBlockHeader))

              if (CoinMiner.isLessThan(newBlockHash, blockHashThreshold)) {
                // Check the best block hash once more.
                if ( bestBlockHash.get.value == chain.getBestBlockHash().get.value ) {
                  // Step 5 : When a block is found, create the block and put it on the blockchain.
                  // Also propate the block to the peer to peer network.
                  val block = blockTemplate.createBlock(newBlockHeader, nonce)
                  peerCommunicator.propagateBlock(block)
                  chain.putBlock(BlockHash(newBlockHash.value), block)
                  blockFound = true
                  println(s"Block Mined.\n hash : ${newBlockHash}, block : ${block}\n\n")
/*
                  if ( chain.getBestBlockHeight() <= PREMINE_BLOCKS) {
                    Thread.sleep(Random.nextInt(120000))
                  }
*/
                }
              }
 //           } while (System.currentTimeMillis() - startTime < MINING_TRIAL_WINDOW_MILLIS && !blockFound)
          } else {
            println("The best block hash is not defined yet.")
          }
        }
      }
    }
    thread.start
  }
}