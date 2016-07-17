package io.scalechain.blockchain.net

import java.io.File
import java.util.concurrent.TimeUnit

import io.scalechain.blockchain.chain.NewOutput
import io.scalechain.blockchain.script.HashSupported
import io.scalechain.blockchain.storage.Storage
import io.scalechain.blockchain.transaction.{CoinAmount, TransactionTestDataTrait}
import io.scalechain.wallet.{WalletBasedBlockSampleData, WalletTestTrait}
import org.scalatest.{Suite, Matchers, BeforeAndAfterEach, FlatSpec}
import HashSupported._

class IncompleteBlockCacheSpec extends FlatSpec with WalletTestTrait with BeforeAndAfterEach with TransactionTestDataTrait with Matchers {

  this: Suite =>

  Storage.initialize()

  var data : WalletBasedBlockSampleData = null
  val testPath = new File("./target/unittests-IncompleteBlockCacheSpec-storage/")

  var signer : BlockSigner = null
  var cache : IncompleteBlockCache = null

  val CACHE_KEEP_MILLISECONDS = 10

  override def beforeEach() {
    super.beforeEach()

    data = new WalletBasedBlockSampleData(wallet)
    cache = new IncompleteBlockCache(CACHE_KEEP_MILLISECONDS, TimeUnit.MILLISECONDS)
  }

  override def afterEach() {
    super.afterEach()

    data = null
    cache = null
  }

  "getBlock" should "return None if no block/transaction was added" in {
    val blockHash = data.Block.BLK02.header.hash
    cache.getBlock(blockHash) shouldBe None
  }

  "getBlock" should "return an IncompleteBlock if a signing transaction was added" in {
    val blockHash = data.Block.BLK02.header.hash

    cache.addSigningTransaction(blockHash, data.Tx.TX03.transaction)

    cache.getBlock(blockHash) shouldBe Some(IncompleteBlock(None, Set(data.Tx.TX03.transaction)))
  }

  "getBlock" should "return None if a signing transaction was added but expired" in {
    val blockHash = data.Block.BLK02.header.hash

    cache.addSigningTransaction(blockHash, data.Tx.TX03.transaction)

    Thread.sleep(CACHE_KEEP_MILLISECONDS + 10)

    cache.getBlock(blockHash) shouldBe None
  }

  "getBlock" should "return an IncompleteBlock if a block was added" in {
    val blockHash = data.Block.BLK02.header.hash

    cache.addBlock(blockHash, data.Block.BLK02)

    cache.getBlock(blockHash) shouldBe Some( IncompleteBlock(Some(data.Block.BLK02), Set()) )
  }

  "getBlock" should "return None if a block was added but expired" in {
    val blockHash = data.Block.BLK02.header.hash

    cache.addBlock(blockHash, data.Block.BLK02)

    Thread.sleep(CACHE_KEEP_MILLISECONDS + 10)

    cache.getBlock(blockHash) shouldBe None
  }


  "addSigningTransaction" should "return an IncompleteBlock without any block" in {
    val blockHash = data.Block.BLK02.header.hash

    cache.addSigningTransaction(blockHash, data.Tx.TX03.transaction) shouldBe IncompleteBlock(None, Set(data.Tx.TX03.transaction))
  }

  "addBlock" should "return an IncompleteBlock without any signing transaction" in {
    val blockHash = data.Block.BLK02.header.hash
    cache.addBlock(blockHash, data.Block.BLK02) shouldBe IncompleteBlock(Some(data.Block.BLK02), Set())
  }


  "addBlock" should "return an IncompleteBlock with signing transactions" in {
    val blockHash = data.Block.BLK02.header.hash

    cache.addSigningTransaction(blockHash, data.Tx.TX03.transaction) shouldBe IncompleteBlock(None, Set(data.Tx.TX03.transaction))
    cache.addSigningTransaction(blockHash, data.Tx.TX04.transaction) shouldBe IncompleteBlock(None, Set(data.Tx.TX03.transaction, data.Tx.TX04.transaction))
    cache.addBlock(blockHash, data.Block.BLK02) shouldBe IncompleteBlock(Some(data.Block.BLK02), Set(data.Tx.TX03.transaction, data.Tx.TX04.transaction))
  }

  "addSigningTransaction" should "return an IncompleteBlock with block" in {
    val blockHash = data.Block.BLK02.header.hash
    cache.addBlock(blockHash, data.Block.BLK02) shouldBe IncompleteBlock(Some(data.Block.BLK02), Set())

    cache.addSigningTransaction(blockHash, data.Tx.TX03.transaction) shouldBe IncompleteBlock(Some(data.Block.BLK02), Set(data.Tx.TX03.transaction))
    cache.addSigningTransaction(blockHash, data.Tx.TX04.transaction) shouldBe IncompleteBlock(Some(data.Block.BLK02), Set(data.Tx.TX03.transaction, data.Tx.TX04.transaction))
  }

  "addBlock" should "return an IncompleteBlock without any signing transaction if expired" in {
    val blockHash = data.Block.BLK02.header.hash

    cache.addSigningTransaction(blockHash, data.Tx.TX03.transaction) shouldBe IncompleteBlock(None, Set(data.Tx.TX03.transaction))
    cache.addSigningTransaction(blockHash, data.Tx.TX04.transaction) shouldBe IncompleteBlock(None, Set(data.Tx.TX03.transaction, data.Tx.TX04.transaction))

    Thread.sleep(CACHE_KEEP_MILLISECONDS + 10)

    cache.addBlock(blockHash, data.Block.BLK02) shouldBe IncompleteBlock(Some(data.Block.BLK02), Set())
  }


  "addSigningTransaction" should "return an IncompleteBlock with the newly added transaction only if expired" in {
    val blockHash = data.Block.BLK02.header.hash

    cache.addBlock(blockHash, data.Block.BLK02) shouldBe IncompleteBlock(Some(data.Block.BLK02), Set())

    cache.addSigningTransaction(blockHash, data.Tx.TX03.transaction) shouldBe IncompleteBlock(Some(data.Block.BLK02), Set(data.Tx.TX03.transaction))

    Thread.sleep(CACHE_KEEP_MILLISECONDS + 10)

    cache.addSigningTransaction(blockHash, data.Tx.TX04.transaction) shouldBe IncompleteBlock(None, Set(data.Tx.TX04.transaction))
  }

}