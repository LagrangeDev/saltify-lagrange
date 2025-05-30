package org.ntqqrev.saltify.lagrange.packet.login

import io.ktor.utils.io.core.*
import kotlinx.io.*
import kotlinx.io.Buffer
import org.ntqqrev.saltify.lagrange.BotContext
import org.ntqqrev.saltify.lagrange.util.binary.Prefix
import org.ntqqrev.saltify.lagrange.util.binary.barrier
import org.ntqqrev.saltify.lagrange.util.binary.writeBytes
import org.ntqqrev.saltify.lagrange.util.binary.writeString
import org.ntqqrev.saltify.lagrange.util.crypto.TEA
import kotlin.random.Random

class Tlv(val bot: BotContext) {
    private val builder = Buffer()

    private var tlvCount: UShort = 0u

    fun tlv18() = defineTlv(0x18u) {
        writeUShort(0u) // ping ver
        writeUInt(5u)
        writeUInt(0u)
        writeUInt(8001u) // app client ver
        writeUInt(bot.keystore.uin.toUInt())
        writeUShort(0u)
        writeUShort(0u)
    }

    fun tlv100() = defineTlv(0x100u) {
        writeUShort(0u) // db buf ver
        writeUInt(5u) // sso ver, dont over 7
        writeInt(bot.appInfo.appId)
        writeInt(bot.appInfo.subAppId)
        writeInt(bot.appInfo.appClientVersion) // app client ver
        writeInt(bot.appInfo.mainSigMap)
    }

    fun tlv106A2() = defineTlv(0x106u) {
        writeFully(bot.keystore.encryptedA1)
    }

    fun tlv106(md5pass: ByteArray) = defineTlv(0x106u) {
        val body = Buffer().apply {
            writeUShort(4u) // tgtgt ver
            writeFully(Random.nextBytes(4)) // crypto.randomBytes(4)
            writeUInt(0u) // sso ver
            writeInt(bot.appInfo.appId)
            writeInt(8001) // app client ver
            writeULong(bot.keystore.uin.toULong())
            writeInt((System.currentTimeMillis() / 1000).toInt())
            writeUInt(0u) // dummy ip
            writeByte(1) // save password
            writeFully(md5pass)
            writeFully(bot.keystore.a2)
            writeUInt(0u)
            writeByte(1) // guid available
            writeFully(bot.keystore.guid)
            writeUInt(1u)
            writeUInt(1u) // login type password
            writeString(bot.keystore.uin.toString(), Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        }

        val buf = Buffer()

        buf.writeInt(bot.keystore.uin.toInt())
        buf.writeFully(ByteArray(4))
        buf.writeFully(md5pass)

        writeBytes(TEA.encrypt(body.readByteArray(), buf.readByteArray()))
    }

    fun tlv107() = defineTlv(0x107u) {
        writeUShort(1u) // pic type
        writeUByte(0x0du) // captcha type
        writeUShort(0u) // pic size
        writeUByte(1u) // ret type
    }

    fun tlv116() = defineTlv(0x116u) {
        writeUByte(0u)
        writeUInt(12058620u)
        writeInt(bot.appInfo.subSigMap)
        writeUByte(0u)
    }

    fun tlv124() = defineTlv(0x124u) {
        writeBytes(ByteArray(12))
    }

    fun tlv128() = defineTlv(0x128u) {
        writeUShort(0u)
        writeUByte(0u) // guid new
        writeUByte(0u) // guid available
        writeUByte(0u) // guid changed
        writeUInt(0u) // guid flag
        writeString(bot.appInfo.os, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        writeBytes(bot.keystore.guid, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        writeString("", Prefix.UINT_16 or Prefix.LENGTH_ONLY) // brand
    }

    fun tlv141() = defineTlv(0x141u) {
        writeString("Unknown", Prefix.UINT_32 or Prefix.LENGTH_ONLY)
        writeUInt(0u)
    }

    fun tlv142() = defineTlv(0x142u) {
        writeUShort(0u)
        writeString(bot.appInfo.packageName, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
    }

    fun tlv144() = defineTlv(0x144u) {
        val tlvPack = Tlv(bot).apply {
            tlv16e()
            tlv147()
            tlv128()
            tlv124()
        }

        writeBytes(TEA.encrypt(tlvPack.build(), bot.keystore.tgtgt))
    }

    fun tlv145() = defineTlv(0x145u) {
        writeBytes(bot.keystore.guid)
    }

    fun tlv147() = defineTlv(0x147u) {
        writeInt(bot.appInfo.appId)
        writeString(bot.appInfo.ptVersion, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        writeString(bot.appInfo.packageName, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
    }

    fun tlv166() = defineTlv(0x166u) {
        writeUByte(5u)
    }

    fun tlv16a() = defineTlv(0x16au) {
        writeFully(bot.keystore.noPicSig)
    }

    fun tlv16e() = defineTlv(0x16eu) {
        writeString(bot.keystore.deviceName)
    }

    fun tlv177() = defineTlv(0x177u) {
        writeUByte(1u)
        writeUInt(0u)
        writeString(bot.appInfo.wtLoginSdk, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
    }

    fun tlv191() = defineTlv(0x191u) {
        writeUByte(0u)
    }

    fun tlv318() = defineTlv(0x318u) {

    }

    fun tlv521() = defineTlv(0x521u) {
        writeUInt(0x13u) // product type
        writeString("basicim", Prefix.UINT_16 or Prefix.LENGTH_ONLY)
    }

    fun build(): ByteArray = Buffer().apply {
        writeUShort(tlvCount)
        writeFully(builder.readByteArray())
    }.readByteArray()

    private fun defineTlv(tag: UShort, tlv: Sink.() -> Unit) {
        tlvCount++

        builder.writeUShort(tag)
        builder.barrier(Prefix.UINT_16 or Prefix.LENGTH_ONLY) {
            tlv()
        }
    }
}

