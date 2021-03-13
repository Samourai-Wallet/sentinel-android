package com.samourai.sentinel.util

import android.util.Patterns
import com.samourai.sentinel.core.SentinelState.Companion.getNetworkParam
import com.samourai.sentinel.core.segwit.bech32.Bech32
import com.samourai.sentinel.core.segwit.bech32.Bech32Segwit
import com.samourai.sentinel.data.AddressTypes
import org.bitcoinj.core.Address
import org.bitcoinj.core.AddressFormatException
import org.bitcoinj.core.Base58
import org.bitcoinj.core.WrongNetworkException
import org.bitcoinj.uri.BitcoinURI
import org.bitcoinj.uri.BitcoinURIParseException
import java.nio.ByteBuffer
import java.util.regex.Pattern

//import android.util.Log;
class FormatsUtil private constructor() {

    companion object {
        const val MAGIC_XPUB = 0x0488B21E
        const val MAGIC_TPUB = 0x043587CF
        const val MAGIC_YPUB = 0x049D7CB2
        const val MAGIC_UPUB = 0x044A5262
        const val MAGIC_ZPUB = 0x04B24746
        const val MAGIC_VPUB = 0x045F1CF6
        const val XPUB = "^[xtyu]pub[1-9A-Za-z][^OIl]+$"
        const val HEX = "^[0-9A-Fa-f]+$"

        @JvmStatic
        var instance: FormatsUtil? = null
            get() {
                if (field == null) {
                    field = FormatsUtil()
                }
                return field
            }

        private val emailPattern = Patterns.EMAIL_ADDRESS
        private val phonePattern = Pattern.compile("(\\+[1-9]{1}[0-9]{1,2}+|00[1-9]{1}[0-9]{1,2}+)[\\(\\)\\.\\-\\s\\d]{6,16}")
        private val URI_BECH32 = "(^bitcoin:(tb|bc)1([qpzry9x8gf2tvdw0s3jn54khce6mua7l]+)(\\?amount\\=([0-9.]+))?$)|(^bitcoin:(TB|BC)1([QPZRY9X8GF2TVDW0S3JN54KHCE6MUA7L]+)(\\?amount\\=([0-9.]+))?$)"
        private val URI_BECH32_LOWER = "^bitcoin:((tb|TB|bc|BC)1[qpzry9x8gf2tvdw0s3jn54khce6mua7l]+)(\\?amount\\=([0-9.]+))?$"
        fun validateBitcoinAddress(address: String): String? {
            return if (isValidBitcoinAddress(address)) {
                address
            } else {
                val addr = getBitcoinAddress(address)
                addr
            }
        }

        fun isBitcoinUri(s: String): Boolean {
            var ret = false
            var uri: BitcoinURI? = null
            try {
                uri = BitcoinURI(s)
                ret = true
            } catch (bupe: BitcoinURIParseException) {
                ret = s.matches(Regex(URI_BECH32))
            }
            return ret
        }

        fun getBitcoinUri(s: String): String? {
            var ret: String? = null
            var uri: BitcoinURI? = null
            try {
                uri = BitcoinURI(s)
                ret = uri.toString()
            } catch (bupe: BitcoinURIParseException) {
                ret = if (s.matches(Regex(URI_BECH32))) {
                    return s
                } else {
                    null
                }
            }
            return ret
        }

        fun getBitcoinAddress(s: String): String? {
            var ret: String? = null
            var uri: BitcoinURI? = null
            try {
                uri = BitcoinURI(s)
                ret = uri.address.toString()
            } catch (bupe: BitcoinURIParseException) {
                if (s.toLowerCase().matches(Regex(URI_BECH32_LOWER))) {
                    val pattern = Pattern.compile(URI_BECH32_LOWER)
                    val matcher = pattern.matcher(s.toLowerCase())
                    if (matcher.find() && matcher.group(1) != null) {
                        return matcher.group(1)
                    }
                } else {
                    ret = null
                }
            }
            return ret
        }

        fun getBitcoinAmount(s: String): String? {
            var ret: String? = null
            var uri: BitcoinURI? = null
            try {
                uri = BitcoinURI(s)
                ret = if (uri.amount != null) {
                    uri.amount.toString()
                } else {
                    "0.0000"
                }
            } catch (bupe: BitcoinURIParseException) {
                if (s.toLowerCase().matches(Regex(URI_BECH32_LOWER))) {
                    val pattern = Pattern.compile(URI_BECH32_LOWER)
                    val matcher = pattern.matcher(s.toLowerCase())
                    if (matcher.find() && matcher.group(4) != null) {
                        val amt = matcher.group(4)
                        ret = try {
                            return java.lang.Long.toString(Math.round(java.lang.Double.valueOf(amt) * 1e8))
                        } catch (nfe: NumberFormatException) {
                            "0.0000"
                        }
                    }
                } else {
                    ret = null
                }
            }
            return ret
        }

        fun isValidBitcoinAddress(address: String): Boolean {
            var ret = false
            var addr: Address? = null
            if (address.toLowerCase().startsWith("bc") || address.toLowerCase().startsWith("tb")) {
                try {
                    val pair = Bech32Segwit.decode(address.substring(0, 2), address)
                    if (pair.left == null || pair.right == null) {
                    } else {
                        ret = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                try {
                    addr = Address(getNetworkParam(), address)
                    if (addr != null) {
                        ret = true
                    }
                } catch (wne: WrongNetworkException) {
                    ret = false
                } catch (afe: AddressFormatException) {
                    ret = false
                }
            }
            return ret
        }

       public fun isValidBech32(address: String): Boolean {
            var ret = false
            ret = try {
                val pair0 = Bech32.bech32Decode(address)
                if (pair0.left == null || pair0.right == null) {
                    false
                } else {
                    val pair1 = Bech32Segwit.decode(address.substring(0, 2), address)
                    !(pair1.left == null || pair1.right == null)
                }
            } catch (e: Exception) {
                false
            }
            return ret
        }

        fun extractPublicKey(code: String): String {
            var payload = code

            if (code.startsWith("BITCOIN:")) {
                payload = code.substring(8)

            }
            if (code.startsWith("bitcoin:")) {
                payload = code.substring(8)
            }
            if (code.startsWith("bitcointestnet:")) {
                payload = code.substring(15)
            }
            if (code.contains("?")) {
                payload = code.substring(0, code.indexOf("?"))
            }
            if (code.contains("?")) {
                payload = code.substring(0, code.indexOf("?"))
            }
            return payload;
        }

        fun getPubKeyType(code: String): AddressTypes {

            var type = AddressTypes.ADDRESS

            if (code.startsWith("xpub") || code.startsWith("tpub")) {
                type = AddressTypes.BIP44
            } else if (code.startsWith("ypub") || code.startsWith("upub")) {
                type = AddressTypes.BIP49
            } else if (code.startsWith("zpub") || code.startsWith("vpub")) {
                type = AddressTypes.BIP84
            }
            return type

        }

        fun isValidXpub(xpub: String): Boolean {
            return try {
                val xpubBytes = Base58.decodeChecked(xpub)
                val byteBuffer = ByteBuffer.wrap(xpubBytes)
                val version = byteBuffer.int
                if (version != MAGIC_XPUB && version != MAGIC_TPUB && version != MAGIC_YPUB && version != MAGIC_UPUB && version != MAGIC_ZPUB && version != MAGIC_VPUB) {
                    throw AddressFormatException("invalid version: $xpub")
                } else {
                    val chain = ByteArray(32)
                    val pub = ByteArray(33)
                    // depth:
                    byteBuffer.get()
                    // parent fingerprint:
                    byteBuffer.int
                    // child no.
                    byteBuffer.int
                    byteBuffer[chain]
                    byteBuffer[pub]
                    val pubBytes = ByteBuffer.wrap(pub)
                    val firstByte = pubBytes.get().toInt()
                    firstByte == 0x02 || firstByte == 0x03
                }
            } catch (e: Exception) {
                false
            }
        }

    }

}